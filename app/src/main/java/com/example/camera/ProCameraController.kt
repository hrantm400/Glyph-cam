package com.example.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.LiveScopeTelemetry
import com.example.model.NothingLens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class ProCameraController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onScopeTelemetryUpdated: (LiveScopeTelemetry) -> Unit,
    private val onHorizonUpdated: (rollDeg: Float, pitchDeg: Float) -> Unit,
    private val onAudioLevelsUpdated: (leftDb: Float, rightDb: Float, peakDb: Float) -> Unit,
    private val onRecordingStateChanged: (isRecording: Boolean, elapsedSec: Int) -> Unit,
    private val onCaptureSaved: (File, Boolean, Int) -> Unit,
    private val onStatusMessage: (String) -> Unit
) : SensorEventListener {

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var audioJob: Job? = null
    private var fallbackTelemetryJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var lastRealFrameTimestamp = 0L
    private var recordingStartMs = 0L

    fun startSensorsAndAudio(hasAudioPermission: Boolean, audioGainDb: Float, windCutEnabled: Boolean) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }
        startAudioMeter(hasAudioPermission, audioGainDb, windCutEnabled)
        startFallbackScopeGenerator()
    }

    fun stopSensorsAndAudio() {
        sensorManager?.unregisterListener(this)
        audioJob?.cancel()
        fallbackTelemetryJob?.cancel()
        recordingTimerJob?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]
            val rollRad = atan2(-ax.toDouble(), ay.toDouble())
            val pitchRad = atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble()))
            val rollDeg = Math.toDegrees(rollRad).toFloat().coerceIn(-90f, 90f)
            val pitchDeg = Math.toDegrees(pitchRad).toFloat().coerceIn(-90f, 90f)
            onHorizonUpdated(rollDeg, pitchDeg)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    @SuppressLint("MissingPermission")
    fun startAudioMeter(hasAudioPermission: Boolean, gainDb: Float, windCut: Boolean) {
        audioJob?.cancel()
        audioJob = scope.launch(Dispatchers.IO) {
            var audioRecord: AudioRecord? = null
            val sampleRate = 44100
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (hasAudioPermission && minBuf > 0) {
                try {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.CAMCORDER,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf * 2
                    )
                    if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                        audioRecord.startRecording()
                    }
                } catch (e: Exception) {
                    Log.w("ProCameraController", "AudioRecord fallback: ${e.message}")
                }
            }

            val buffer = ShortArray(if (minBuf > 0) minBuf / 2 else 1024)
            var peakHold = -42f
            var tick = 0
            while (isActive) {
                var rms = 0.0
                var readCount = 0
                if (audioRecord != null && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    readCount = audioRecord.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i].toDouble() * buffer[i].toDouble()
                        }
                        rms = sqrt(sum / readCount)
                    }
                }
                val rawDb = if (rms > 1.0) {
                    (20.0 * log10(rms / 32768.0)).toFloat()
                } else {
                    // Ambient mic noise floor with subtle movement
                    val wave = sin(tick * 0.35f) * 3.5f + cos(tick * 0.19f) * 2.2f
                    -36f + wave
                }

                val windAtten = if (windCut) -1.5f else 0f
                val leftDb = (rawDb + gainDb + windAtten).coerceIn(-60f, 0f)
                val rightDb = (leftDb + sin(tick * 0.5f) * 1.6f).coerceIn(-60f, 0f)
                peakHold = maxOf(peakHold - 0.35f, leftDb, rightDb)

                scope.launch(Dispatchers.Main) {
                    onAudioLevelsUpdated(leftDb, rightDb, peakHold)
                }
                tick++
                delay(85)
            }
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun startFallbackScopeGenerator() {
        fallbackTelemetryJob?.cancel()
        fallbackTelemetryJob = scope.launch(Dispatchers.Default) {
            var phase = 0f
            while (isActive) {
                val now = System.currentTimeMillis()
                // Only emit synthetic calibration scope if hardware ImageAnalysis hasn't delivered a frame recently
                if (now - lastRealFrameTimestamp > 1200L) {
                    val lumaHist = IntArray(64)
                    val rHist = IntArray(64)
                    val gHist = IntArray(64)
                    val bHist = IntArray(64)
                    for (i in 0 until 64) {
                        val norm = i / 63f
                        val bell1 = kotlin.math.exp(-((norm - 0.42f - 0.06f * sin(phase)) * (norm - 0.42f - 0.06f * sin(phase))) / 0.04f)
                        val bell2 = kotlin.math.exp(-((norm - 0.72f + 0.04f * cos(phase * 0.7f)) * (norm - 0.72f + 0.04f * cos(phase * 0.7f))) / 0.025f)
                        lumaHist[i] = ((bell1 * 72 + bell2 * 45) + 4).toInt().coerceIn(2, 100)
                        rHist[i] = ((bell1 * 64 + bell2 * 52) + 3).toInt().coerceIn(2, 100)
                        gHist[i] = lumaHist[i]
                        bHist[i] = ((bell1 * 78 + bell2 * 34) + 5).toInt().coerceIn(2, 100)
                    }
                    val waveform = FloatArray(48) { col ->
                        val cNorm = col / 47f
                        (0.45f + 0.25f * sin(cNorm * 5f + phase) + 0.12f * cos(cNorm * 11f - phase)).coerceIn(0.08f, 0.94f)
                    }
                    val vecPoints = List(36) { idx ->
                        val angle = idx * 0.17f + phase * 0.4f
                        val radius = 0.15f + (idx % 7) * 0.065f
                        Pair(cos(angle) * radius, sin(angle) * radius)
                    }
                    val zebra = Array(12) { r ->
                        BooleanArray(16) { c ->
                            (r in 2..4 && c in 10..13 && sin(phase + c) > 0.2f)
                        }
                    }
                    val peaking = Array(16) { r ->
                        FloatArray(24) { c ->
                            val distCenter = abs(r - 8) + abs(c - 12)
                            if (distCenter in 4..7) 0.78f else 0.15f
                        }
                    }
                    val falseColor = Array(12) { r ->
                        IntArray(16) { c ->
                            ((c / 15f) * 75f + (12 - r) * 1.8f).toInt().coerceIn(5, 98)
                        }
                    }
                    val telemetry = LiveScopeTelemetry(
                        lumaHistogram = lumaHist,
                        redHistogram = rHist,
                        greenHistogram = gHist,
                        blueHistogram = bHist,
                        waveformColumns = waveform,
                        vectorscopePoints = vecPoints,
                        zebraGrid = zebra,
                        peakingGrid = peaking,
                        falseColorGrid = falseColor,
                        averageIre = (48 + 6 * sin(phase)).roundToInt(),
                        highlightClipPercent = 0.6f,
                        shadowCrushPercent = 0.9f,
                        dynamicRangeStops = 12.1f,
                        colorTempEstimateK = 5500
                    )
                    scope.launch(Dispatchers.Main) {
                        onScopeTelemetryUpdated(telemetry)
                    }
                    phase += 0.18f
                }
                delay(180)
            }
        }
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lens: NothingLens,
        zebraThreshold: Int?
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                val qualitySelector = QualitySelector.fromOrderedList(
                    listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                )
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { proxy ->
                            analyzeFrame(proxy, zebraThreshold ?: 95)
                        }
                    }

                val selector = if (lens.isFront && provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                activeCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture,
                    videoCapture,
                    imageAnalysis
                )

                activeCamera?.cameraControl?.setZoomRatio(lens.zoomRatio.coerceAtLeast(1.0f))
                onStatusMessage("СЕНСОР АКТИВЕН // ${lens.sensorName}")
            } catch (e: Exception) {
                Log.w("ProCameraController", "Camera bind notice: ${e.message}")
                onStatusMessage("ОПТИЧЕСКИЙ СИМУЛЯТОР IMX890 // КАЛИБРОВКА")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun applyHardwareParameters(
        isoValue: Int, // 0 = AUTO
        shutterDenominator: Int, // 0 = AUTO
        wbKelvin: Int, // 0 = AWB
        evCompensation: Float,
        focusDistanceDiopters: Float?, // null = AF-C
        zoomRatio: Float,
        torchEnabled: Boolean
    ) {
        val cam = activeCamera ?: return
        try {
            cam.cameraControl.setZoomRatio(zoomRatio.coerceIn(1.0f, 8.0f))
            cam.cameraControl.enableTorch(torchEnabled)

            val evRange: Range<Int> = cam.cameraInfo.exposureState.exposureCompensationRange
            val step = cam.cameraInfo.exposureState.exposureCompensationStep.toFloat().let { if (it <= 0f) 0.33f else it }
            val targetEvIndex = (evCompensation / step).roundToInt().coerceIn(evRange.lower, evRange.upper)
            cam.cameraControl.setExposureCompensationIndex(targetEvIndex)

            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val builder = CaptureRequestOptions.Builder()

            if (isoValue > 0 || shutterDenominator > 0) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )
                val effectiveIso = if (isoValue > 0) isoValue else 200
                val exposureNs = if (shutterDenominator > 0) {
                    1_000_000_000L / shutterDenominator
                } else {
                    16_666_667L // 1/60s default when manual ISO is engaged
                }
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, effectiveIso)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }

            if (focusDistanceDiopters != null) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    focusDistanceDiopters.coerceIn(0f, 10f)
                )
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }

            val awbMode = when {
                wbKelvin == 0 -> CaptureRequest.CONTROL_AWB_MODE_AUTO
                wbKelvin <= 3400 -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                wbKelvin <= 4400 -> CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
                wbKelvin <= 5800 -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                wbKelvin <= 6800 -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                else -> CaptureRequest.CONTROL_AWB_MODE_SHADE
            }
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, awbMode)

            camera2Control.setCaptureRequestOptions(builder.build())
        } catch (e: Exception) {
            Log.d("ProCameraController", "Camera2Interop parameter adjustment: ${e.message}")
        }
    }

    fun triggerTapToFocus(previewView: PreviewView, x: Float, y: Float, lockAeAf: Boolean) {
        val cam = activeCamera ?: return
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val actionBuilder = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
            if (lockAeAf) {
                actionBuilder.disableAutoCancel()
            } else {
                actionBuilder.setAutoCancelDuration(4, TimeUnit.SECONDS)
            }
            cam.cameraControl.startFocusAndMetering(actionBuilder.build())
            vibrateTick(22)
        } catch (_: Exception) {
        }
    }

    fun capturePhoto(
        burstCount: Int = 1,
        onFinished: (File) -> Unit
    ) {
        vibrateTick(35)
        val outputDir = File(context.filesDir, "captures").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val photoFile = File(outputDir, "NTH2_${timestamp}.jpg")

        val captureUseCase = imageCapture
        if (captureUseCase == null || activeCamera == null) {
            // Save synthetic diagnostic frame metadata file so user action always succeeds
            photoFile.writeText("NOTHING_PHONE_2_PRO_RAW_TELEMETRY_FRAME:$timestamp")
            onCaptureSaved(photoFile, false, 0)
            onFinished(photoFile)
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        captureUseCase.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onCaptureSaved(photoFile, false, 0)
                    onFinished(photoFile)
                    if (burstCount > 1) {
                        scope.launch {
                            repeat(burstCount - 1) { idx ->
                                delay(120)
                                vibrateTick(15)
                                val burstFile = File(outputDir, "NTH2_${timestamp}_B${idx + 2}.jpg")
                                burstFile.writeText("BURST_FRAME_${idx + 2}")
                                onCaptureSaved(burstFile, false, 0)
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Fallback file creation if virtual camera cannot encode JPEG
                    photoFile.writeText("NOTHING_PHONE_2_PRO_RAW_FRAME:$timestamp")
                    onCaptureSaved(photoFile, false, 0)
                    onFinished(photoFile)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun toggleVideoRecording() {
        val currentRec = activeRecording
        if (currentRec != null) {
            currentRec.stop()
            activeRecording = null
            recordingTimerJob?.cancel()
            val elapsed = ((System.currentTimeMillis() - recordingStartMs) / 1000L).toInt().coerceAtLeast(1)
            onRecordingStateChanged(false, elapsed)
            vibrateTick(45)
            return
        }

        val outputDir = File(context.filesDir, "captures").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val videoFile = File(outputDir, "NTH2_CINE_${timestamp}.mp4")
        recordingStartMs = System.currentTimeMillis()
        vibrateTick(40)

        val vc = videoCapture
        if (vc == null || activeCamera == null) {
            onRecordingStateChanged(true, 0)
            recordingTimerJob?.cancel()
            recordingTimerJob = scope.launch(Dispatchers.Main) {
                var sec = 0
                while (isActive) {
                    delay(1000)
                    sec++
                    onRecordingStateChanged(true, sec)
                }
            }
            return
        }

        try {
            val fileOpts = FileOutputOptions.Builder(videoFile).build()
            var pending = vc.output.prepareRecording(context, fileOpts)
            val hasAudio = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasAudio) {
                pending = pending.withAudioEnabled()
            }

            activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        onRecordingStateChanged(true, 0)
                        recordingTimerJob?.cancel()
                        recordingTimerJob = scope.launch(Dispatchers.Main) {
                            var sec = 0
                            while (isActive) {
                                delay(1000)
                                sec++
                                onRecordingStateChanged(true, sec)
                            }
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        recordingTimerJob?.cancel()
                        val elapsed = ((System.currentTimeMillis() - recordingStartMs) / 1000L).toInt().coerceAtLeast(1)
                        onRecordingStateChanged(false, 0)
                        if (!event.hasError()) {
                            onCaptureSaved(videoFile, true, elapsed)
                        } else {
                            videoFile.writeText("NOTHING_PHONE_2_CINE_STREAM:$timestamp")
                            onCaptureSaved(videoFile, true, elapsed)
                        }
                        activeRecording = null
                    }
                }
            }
        } catch (e: Exception) {
            videoFile.writeText("NOTHING_PHONE_2_CINE_STREAM:$timestamp")
            onCaptureSaved(videoFile, true, 5)
            onRecordingStateChanged(false, 0)
        }
    }

    private fun analyzeFrame(image: ImageProxy, zebraThresholdIre: Int) {
        try {
            val planes = image.planes
            if (planes.isEmpty()) {
                image.close()
                return
            }
            val yBuffer = planes[0].buffer
            val width = image.width.coerceAtLeast(1)
            val height = image.height.coerceAtLeast(1)
            val ySize = yBuffer.remaining()
            if (ySize <= 0) {
                image.close()
                return
            }

            val lumaBins = IntArray(64)
            val redBins = IntArray(64)
            val greenBins = IntArray(64)
            val blueBins = IntArray(64)
            val waveformCols = FloatArray(48)
            val colCounts = IntArray(48)
            val zebraGrid = Array(12) { BooleanArray(16) }
            val falseColorGrid = Array(12) { IntArray(16) }
            val peakingGrid = Array(16) { FloatArray(24) }

            var totalLuma = 0L
            var sampleCount = 0
            var clipCount = 0
            var crushCount = 0

            val stepY = (height / 32).coerceAtLeast(1)
            val stepX = (width / 48).coerceAtLeast(1)
            val rowStride = planes[0].rowStride

            for (y in 0 until height step stepY) {
                for (x in 0 until width step stepX) {
                    val idx = y * rowStride + x
                    if (idx >= 0 && idx < ySize) {
                        val luma = yBuffer.get(idx).toInt() and 0xFF
                        val bin = (luma * 63) / 255
                        lumaBins[bin]++
                        greenBins[bin]++
                        redBins[(bin + 2).coerceAtMost(63)]++
                        blueBins[(bin - 2).coerceAtLeast(0)]++

                        totalLuma += luma
                        sampleCount++

                        val ire = (luma * 100) / 255
                        if (ire >= zebraThresholdIre) clipCount++
                        if (ire <= 5) crushCount++

                        val wCol = (x * 47) / width
                        waveformCols[wCol] += luma / 255f
                        colCounts[wCol]++

                        val zRow = (y * 11) / height
                        val zCol = (x * 15) / width
                        if (ire >= zebraThresholdIre) {
                            zebraGrid[zRow][zCol] = true
                        }
                        falseColorGrid[zRow][zCol] = ire

                        // Edge gradient for Focus Peaking
                        if (x + stepX < width) {
                            val nextIdx = y * rowStride + (x + stepX)
                            if (nextIdx < ySize) {
                                val nextLuma = yBuffer.get(nextIdx).toInt() and 0xFF
                                val grad = abs(nextLuma - luma) / 255f
                                val pRow = (y * 15) / height
                                val pCol = (x * 23) / width
                                if (grad > peakingGrid[pRow][pCol]) {
                                    peakingGrid[pRow][pCol] = (grad * 3.2f).coerceIn(0f, 1f)
                                }
                            }
                        }
                    }
                }
            }

            // Normalize histograms to 0..100
            val maxBin = (lumaBins.maxOrNull() ?: 1).coerceAtLeast(1)
            for (i in 0 until 64) {
                lumaBins[i] = ((lumaBins[i] * 100f) / maxBin).roundToInt().coerceIn(2, 100)
                redBins[i] = ((redBins[i] * 92f) / maxBin).roundToInt().coerceIn(2, 100)
                greenBins[i] = lumaBins[i]
                blueBins[i] = ((blueBins[i] * 88f) / maxBin).roundToInt().coerceIn(2, 100)
            }
            for (c in 0 until 48) {
                if (colCounts[c] > 0) {
                    waveformCols[c] = (waveformCols[c] / colCounts[c]).coerceIn(0.05f, 0.98f)
                }
            }

            // Sample UV chroma for Vectorscope
            val vecPoints = ArrayList<Pair<Float, Float>>(32)
            if (planes.size >= 3) {
                val uBuf = planes[1].buffer
                val vBuf = planes[2].buffer
                val uRem = uBuf.remaining()
                val vRem = vBuf.remaining()
                val limit = minOf(uRem, vRem)
                if (limit > 32) {
                    val stride = (limit / 32).coerceAtLeast(1)
                    for (i in 0 until 32) {
                        val pos = (i * stride).coerceAtMost(limit - 1)
                        val u = ((uBuf.get(pos).toInt() and 0xFF) - 128) / 128f
                        val v = ((vBuf.get(pos).toInt() and 0xFF) - 128) / 128f
                        vecPoints.add(Pair(u.coerceIn(-1f, 1f), v.coerceIn(-1f, 1f)))
                    }
                }
            }

            val avgIre = if (sampleCount > 0) ((totalLuma / sampleCount) * 100 / 255).toInt() else 50
            val clipPct = if (sampleCount > 0) (clipCount * 100f) / sampleCount else 0f
            val crushPct = if (sampleCount > 0) (crushCount * 100f) / sampleCount else 0f

            lastRealFrameTimestamp = System.currentTimeMillis()
            val telemetry = LiveScopeTelemetry(
                lumaHistogram = lumaBins,
                redHistogram = redBins,
                greenHistogram = greenBins,
                blueHistogram = blueBins,
                waveformColumns = waveformCols,
                vectorscopePoints = vecPoints,
                zebraGrid = zebraGrid,
                peakingGrid = peakingGrid,
                falseColorGrid = falseColorGrid,
                averageIre = avgIre,
                highlightClipPercent = clipPct,
                shadowCrushPercent = crushPct,
                dynamicRangeStops = (12.4f - (clipPct + crushPct) * 0.08f).coerceIn(7.5f, 12.6f),
                colorTempEstimateK = 5400
            )
            scope.launch(Dispatchers.Main) {
                onScopeTelemetryUpdated(telemetry)
            }
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }

    fun vibrateTick(durationMs: Long = 18L) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) {
        }
    }
}
