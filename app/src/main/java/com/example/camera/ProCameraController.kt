package com.example.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.model.AppMode
import com.example.model.AwbPreset
import com.example.model.CameraCapabilities
import com.example.model.CameraStatus
import com.example.model.FlashMode
import com.example.model.LensOption
import com.example.model.LiveExposureInfo
import com.example.model.LiveScopeTelemetry
import com.example.model.PhotoDriveMode
import com.example.model.PhotoFormat
import com.example.model.SavedCapture
import com.example.model.VideoDynamicRange
import com.example.model.VideoFps
import com.example.model.VideoResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The camera pipeline: binds CameraX use cases for the current mode, applies manual controls
 * through the Camera2 interop, captures photos and videos into MediaStore and feeds the UI with
 * real scopes, audio levels and sensor data. Nothing here is simulated: when something is not
 * available the UI is told so.
 */
@OptIn(ExperimentalCamera2Interop::class)
class ProCameraController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val listener: Listener
) : SensorEventListener {

    interface Listener {
        fun onStatus(status: CameraStatus)
        fun onCapabilities(capabilities: CameraCapabilities)
        fun onZoomChanged(ratio: Float, minRatio: Float, maxRatio: Float)
        fun onTelemetry(telemetry: LiveScopeTelemetry)
        fun onLiveExposure(info: LiveExposureInfo)
        fun onHorizon(rollDeg: Float, pitchDeg: Float)
        fun onAudioLevels(leftDb: Float, rightDb: Float, peakDb: Float, available: Boolean)
        fun onRecordingChanged(isRecording: Boolean, elapsedMs: Long)
        fun onCaptureSaved(capture: SavedCapture)
        fun onShutterFired()
        fun onTimerTick(secondsLeft: Int)
        fun onBusy(busy: Boolean)
        fun onMessage(text: String)
    }

    data class BindRequest(
        val mode: AppMode,
        val useFront: Boolean,
        val photoFormat: PhotoFormat,
        val videoResolution: VideoResolution,
        val fps: VideoFps,
        val dynamicRange: VideoDynamicRange,
        val stabilization: Boolean
    )

    data class ManualParams(
        val iso: Int, // 0 = auto
        val shutterNs: Long, // 0 = auto
        val awb: AwbPreset,
        val evCompensation: Float,
        val focusDiopters: Float?, // null = autofocus
        val torch: Boolean,
        val flash: FlashMode,
        val zebraIre: Int?,
        val isVideo: Boolean
    )

    data class PhotoRequest(
        val drive: PhotoDriveMode,
        val timerSeconds: Int,
        val lutMatrix: FloatArray?, // 4x5 colour matrix baked into JPEGs, null = none
        val lutCode: String
    )

    private val tag = "ProCameraController"
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val resolver = context.contentResolver

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var bindGeneration = 0
    private var analysisActive = false
    private var frontCameraActive = false

    @Volatile
    var capabilities: CameraCapabilities = CameraCapabilities()
        private set
    private var activePhotoFormat: PhotoFormat = PhotoFormat.JPEG
    private var currentParams: ManualParams? = null
    private var desiredZoom: Float? = null
    private var zoomLive: LiveData<ZoomState>? = null
    private val zoomObserver = Observer<ZoomState> { z ->
        listener.onZoomChanged(z.zoomRatio, z.minZoomRatio, z.maxZoomRatio)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var filteredRoll = 0f
    private var filteredPitch = 0f

    private var audioJob: Job? = null
    private var audioMeterWanted = false
    private var captureJob: Job? = null
    private var recordingStartMs = 0L
    private var recordingPeakDb = -60f

    @Volatile private var lastAutoIso = 0
    @Volatile private var lastAutoExposureNs = 0L
    @Volatile private var zebraThresholdIre = 95
    private var lastLiveExposureMs = 0L
    private var lastTelemetryMs = 0L

    val isRecording: Boolean get() = activeRecording != null

    // ------------------------------------------------------------------------------------------
    // Binding
    // ------------------------------------------------------------------------------------------

    fun bind(owner: LifecycleOwner, previewView: PreviewView, request: BindRequest) {
        val generation = ++bindGeneration
        listener.onStatus(CameraStatus.Starting)
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (generation != bindGeneration) return@addListener
            val provider = try {
                future.get()
            } catch (e: Exception) {
                reportError("Камера недоступна: ${e.localizedMessage ?: e.javaClass.simpleName}")
                return@addListener
            }
            cameraProvider = provider
            try {
                bindUseCases(provider, owner, previewView, request)
            } catch (e: Exception) {
                Log.e(tag, "bind failed", e)
                reportError("Не удалось запустить камеру: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }, mainExecutor)
    }

    private fun bindUseCases(
        provider: ProcessCameraProvider,
        owner: LifecycleOwner,
        previewView: PreviewView,
        request: BindRequest
    ) {
        val hasBack = runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)
        val hasFront = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
        if (!hasBack && !hasFront) {
            reportError("На устройстве не найдено ни одной камеры")
            return
        }
        val useFront = (request.useFront && hasFront) || !hasBack
        frontCameraActive = useFront
        val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val info = provider.getCameraInfo(selector)
        val caps = probeCapabilities(info, useFront, hasFront)
        capabilities = caps
        listener.onCapabilities(caps)

        val isVideo = request.mode == AppMode.CINE_VIDEO
        val fpsSupported = caps.videoFps.contains(request.fps)

        val previewBuilder = Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        if (isVideo) AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                        else AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                    )
                    .build()
            )
        if (isVideo && fpsSupported) previewBuilder.setTargetFrameRate(Range(request.fps.fps, request.fps.fps))
        if (isVideo && request.stabilization && caps.previewStabilization) previewBuilder.setPreviewStabilizationEnabled(true)
        Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(captureCallback)
        val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
                    )
                    .build()
            )
            .build()
            .also { it.setAnalyzer(analysisExecutor, ::analyzeFrame) }

        imageCapture = null
        videoCapture = null
        val captureUseCase: UseCase = if (isVideo) {
            buildVideoCapture(request, caps, fpsSupported).also { videoCapture = it }
        } else {
            buildImageCapture(request, caps).also { imageCapture = it }
        }

        val viewPort = previewView.viewPort
        val tiers = listOf(
            listOf(preview, captureUseCase, analysis),
            listOf(preview, captureUseCase),
            listOf(preview)
        )
        var camera: Camera? = null
        var boundTier = -1
        var lastError: Exception? = null
        for ((index, tier) in tiers.withIndex()) {
            try {
                provider.unbindAll()
                camera = if (viewPort != null) {
                    val group = UseCaseGroup.Builder().setViewPort(viewPort)
                    tier.forEach { group.addUseCase(it) }
                    provider.bindToLifecycle(owner, selector, group.build())
                } else {
                    provider.bindToLifecycle(owner, selector, *tier.toTypedArray())
                }
                boundTier = index
                break
            } catch (e: Exception) {
                lastError = e
                Log.w(tag, "bind tier $index failed: ${e.message}")
            }
        }
        if (camera == null) {
            reportError("Не удалось запустить камеру: ${lastError?.localizedMessage ?: "неподдерживаемая комбинация потоков"}")
            return
        }
        activeCamera = camera
        analysisActive = boundTier == 0
        if (boundTier >= 2) {
            imageCapture = null
            videoCapture = null
        }
        if (!analysisActive) {
            listener.onTelemetry(LiveScopeTelemetry(hasFrame = false))
        }

        zoomLive?.removeObserver(zoomObserver)
        zoomLive = camera.cameraInfo.zoomState.also { it.observe(owner, zoomObserver) }

        desiredZoom?.let { camera.cameraControl.setZoomRatio(it.coerceIn(caps.minZoom, caps.maxZoom)) }
        currentParams?.let { applyParams(it) }

        val detail = buildString {
            append(caps.cameraLabel)
            if (isVideo) {
                append(" · ")
                append(request.videoResolution.label)
                append(if (fpsSupported) " ${request.fps.fps}p" else "")
            } else {
                append(" · ")
                append(activePhotoFormat.label)
            }
            when (boundTier) {
                1 -> append(" · без скопов")
                2 -> append(" · только превью")
            }
        }
        listener.onStatus(CameraStatus.Ready(detail))
    }

    private fun buildImageCapture(request: BindRequest, caps: CameraCapabilities): ImageCapture {
        val builder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(95)
        val wanted = request.photoFormat
        activePhotoFormat = if (caps.photoFormats.contains(wanted)) wanted else PhotoFormat.JPEG
        when (activePhotoFormat) {
            PhotoFormat.RAW_DNG -> builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW)
            PhotoFormat.RAW_PLUS_JPEG -> builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
            PhotoFormat.ULTRA_HDR -> builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
            PhotoFormat.JPEG -> Unit
        }
        return builder.build()
    }

    private fun buildVideoCapture(
        request: BindRequest,
        caps: CameraCapabilities,
        fpsSupported: Boolean
    ): VideoCapture<Recorder> {
        val quality = when (request.videoResolution) {
            VideoResolution.UHD_4K -> Quality.UHD
            VideoResolution.FHD_1080P -> Quality.FHD
            VideoResolution.HD_720P -> Quality.HD
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(quality)))
            .build()
        val builder = VideoCapture.Builder(recorder)
        if (fpsSupported) builder.setTargetFrameRate(Range(request.fps.fps, request.fps.fps))
        if (request.dynamicRange == VideoDynamicRange.HLG_10 && caps.hdrVideo) builder.setDynamicRange(DynamicRange.HLG_10_BIT)
        if (request.stabilization && caps.videoStabilization) builder.setVideoStabilizationEnabled(true)
        return builder.build()
    }

    private fun reportError(message: String) {
        activeCamera = null
        imageCapture = null
        videoCapture = null
        listener.onStatus(CameraStatus.Error(message))
    }

    // ------------------------------------------------------------------------------------------
    // Capabilities
    // ------------------------------------------------------------------------------------------

    private fun probeCapabilities(info: CameraInfo, isFront: Boolean, hasFront: Boolean): CameraCapabilities {
        val c2 = runCatching { Camera2CameraInfo.from(info) }.getOrNull()
        fun <T> ch(key: CameraCharacteristics.Key<T>): T? = runCatching { c2?.getCameraCharacteristic(key) }.getOrNull()

        val capsArray = ch(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val manualSensor = capsArray.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val isoRange = ch(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureRange = ch(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val minFocus = ch(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val apertures = ch(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
        val focals = ch(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val physicalSize = ch(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val pixelArray = ch(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

        val zoom = info.zoomState.value
        val exposure = runCatching { info.exposureState }.getOrNull()
        val formats = runCatching { ImageCapture.getImageCaptureCapabilities(info).supportedOutputFormats }
            .getOrDefault(setOf(ImageCapture.OUTPUT_FORMAT_JPEG))
        val photoFormats = buildSet {
            add(PhotoFormat.JPEG)
            if (formats.contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)) add(PhotoFormat.ULTRA_HDR)
            if (formats.contains(ImageCapture.OUTPUT_FORMAT_RAW)) add(PhotoFormat.RAW_DNG)
            if (formats.contains(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)) add(PhotoFormat.RAW_PLUS_JPEG)
        }
        val videoCaps = runCatching { Recorder.getVideoCapabilities(info) }.getOrNull()
        val qualities = runCatching { videoCaps?.getSupportedQualities(DynamicRange.SDR) }.getOrNull() ?: emptyList()
        val resolutions = buildList {
            if (qualities.contains(Quality.UHD)) add(VideoResolution.UHD_4K)
            if (qualities.contains(Quality.FHD)) add(VideoResolution.FHD_1080P)
            if (qualities.contains(Quality.HD)) add(VideoResolution.HD_720P)
            if (isEmpty()) add(VideoResolution.FHD_1080P)
        }
        val hdr = runCatching { videoCaps?.supportedDynamicRanges?.contains(DynamicRange.HLG_10_BIT) }.getOrNull() == true
        val fpsRanges = runCatching { info.supportedFrameRateRanges }.getOrDefault(emptySet())
        val fpsOptions = VideoFps.entries.filter { opt ->
            fpsRanges.any { r -> r.upper >= opt.fps && r.lower <= opt.fps }
        }.ifEmpty { listOf(VideoFps.FPS_30) }
        val previewStab = runCatching { Preview.getPreviewCapabilities(info).isStabilizationSupported }.getOrDefault(false)
        val videoStab = runCatching { videoCaps?.isStabilizationSupported }.getOrNull() == true

        val focalMm = focals?.firstOrNull()
        val focalEquiv = equivalentFocal(focalMm, physicalSize?.width, physicalSize?.height)
        val megapixels = if (pixelArray != null) pixelArray.width * pixelArray.height / 1_000_000f else 0f
        val apertureLabel = apertures?.firstOrNull()?.let { String.format(Locale.US, "f/%.1f", it) } ?: ""
        val label = buildString {
            append(if (isFront) "Фронтальная камера" else "Основная камера")
            if (megapixels > 0f) append(String.format(Locale.US, " · %.0f МП", megapixels))
        }

        return CameraCapabilities(
            cameraLabel = label,
            isFront = isFront,
            hasFrontCamera = hasFront,
            minZoom = zoom?.minZoomRatio ?: 1f,
            maxZoom = zoom?.maxZoomRatio ?: 1f,
            hasFlash = info.hasFlashUnit(),
            manualSensor = manualSensor && isoRange != null && exposureRange != null,
            isoMin = isoRange?.lower ?: 100,
            isoMax = isoRange?.upper ?: 800,
            exposureMinNs = exposureRange?.lower ?: 100_000L,
            exposureMaxNs = exposureRange?.upper ?: 100_000_000L,
            manualFocus = minFocus > 0f,
            minFocusDiopters = minFocus,
            evIndexMin = exposure?.exposureCompensationRange?.lower ?: 0,
            evIndexMax = exposure?.exposureCompensationRange?.upper ?: 0,
            evStep = exposure?.exposureCompensationStep?.toFloat() ?: 0f,
            photoFormats = photoFormats,
            videoResolutions = resolutions,
            videoFps = fpsOptions,
            hdrVideo = hdr,
            previewStabilization = previewStab,
            videoStabilization = videoStab,
            sensorMegapixels = megapixels,
            apertureLabel = apertureLabel,
            focalEquivMm = focalEquiv ?: 0,
            lensOptions = buildLensOptions(info, zoom, focalEquiv, isFront)
        )
    }

    private fun equivalentFocal(focalMm: Float?, sensorW: Float?, sensorH: Float?): Int? {
        if (focalMm == null || sensorW == null || sensorH == null || sensorW <= 0f) return null
        val diagonal = sqrt(sensorW * sensorW + sensorH * sensorH)
        if (diagonal <= 0f) return null
        return (focalMm * 43.27f / diagonal).roundToInt().coerceIn(8, 400)
    }

    /** Lens pills: one per physical camera the logical camera can switch to, plus the usual 1× and 2× crops. */
    private fun buildLensOptions(info: CameraInfo, zoom: ZoomState?, mainEquiv: Int?, isFront: Boolean): List<LensOption> {
        val minZoom = zoom?.minZoomRatio ?: 1f
        val maxZoom = zoom?.maxZoomRatio ?: 1f
        val ratios = sortedSetOf(1.0f)
        if (!isFront) {
            val physical = runCatching { info.physicalCameraInfos }.getOrDefault(emptySet())
            if (mainEquiv != null && physical.size > 1) {
                physical.forEach { phys ->
                    val c2 = runCatching { Camera2CameraInfo.from(phys) }.getOrNull() ?: return@forEach
                    val f = runCatching { c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) }.getOrNull()?.firstOrNull()
                    val size = runCatching { c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) }.getOrNull()
                    val eq = equivalentFocal(f, size?.width, size?.height) ?: return@forEach
                    val ratio = eq.toFloat() / mainEquiv
                    if (ratio in (minZoom - 0.05f)..(maxZoom + 0.05f)) {
                        val rounded = (ratio * 10f).roundToInt() / 10f
                        if (ratios.none { abs(it - rounded) < 0.15f }) ratios.add(rounded)
                    }
                }
            }
            if (minZoom < 0.95f && ratios.none { it < 0.95f }) ratios.add((minZoom * 10f).roundToInt() / 10f)
            if (maxZoom >= 2f && ratios.none { it in 1.6f..2.4f }) ratios.add(2.0f)
        }
        return ratios.map { ratio ->
            val clamped = ratio.coerceIn(minZoom, maxZoom)
            val label = if (clamped < 0.95f || abs(clamped - clamped.roundToInt()) > 0.05f) {
                String.format(Locale.US, "%.1f×", clamped)
            } else {
                "${clamped.roundToInt()}×"
            }
            LensOption(
                id = String.format(Locale.US, "z%.1f", clamped),
                label = label,
                zoomRatio = clamped,
                focalEquivMm = mainEquiv?.let { (it * clamped).roundToInt() },
                isFront = isFront
            )
        }
    }

    // ------------------------------------------------------------------------------------------
    // Manual controls
    // ------------------------------------------------------------------------------------------

    fun setZoomRatio(ratio: Float) {
        val caps = capabilities
        val clamped = ratio.coerceIn(caps.minZoom, caps.maxZoom.coerceAtLeast(caps.minZoom))
        desiredZoom = clamped
        activeCamera?.cameraControl?.setZoomRatio(clamped)
    }

    fun currentZoom(): Float = zoomLive?.value?.zoomRatio ?: desiredZoom ?: 1f

    fun applyParams(params: ManualParams) {
        currentParams = params
        zebraThresholdIre = params.zebraIre ?: 95
        val cam = activeCamera ?: return
        val caps = capabilities
        try {
            cam.cameraControl.enableTorch(params.torch || params.flash == FlashMode.TORCH)
            imageCapture?.flashMode = when (params.flash) {
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                else -> ImageCapture.FLASH_MODE_OFF
            }
            if (caps.evStep > 0f) {
                val index = (params.evCompensation / caps.evStep).roundToInt().coerceIn(caps.evIndexMin, caps.evIndexMax)
                cam.cameraControl.setExposureCompensationIndex(index)
            }

            val builder = CaptureRequestOptions.Builder()
            val manualExposure = caps.manualSensor && (params.iso > 0 || params.shutterNs > 0)
            if (manualExposure) {
                val iso = (if (params.iso > 0) params.iso else lastAutoIso.takeIf { it > 0 } ?: 400)
                    .coerceIn(caps.isoMin, caps.isoMax)
                val exposureNs = (if (params.shutterNs > 0) params.shutterNs else lastAutoExposureNs.takeIf { it > 0 } ?: 16_666_667L)
                    .coerceIn(caps.exposureMinNs, caps.exposureMaxNs)
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNs)
            }
            if (params.focusDiopters != null && caps.manualFocus) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                builder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    params.focusDiopters.coerceIn(0f, caps.minFocusDiopters)
                )
            }
            if (params.awb != AwbPreset.AUTO) {
                val mode = when (params.awb) {
                    AwbPreset.INCANDESCENT -> CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT
                    AwbPreset.FLUORESCENT -> CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT
                    AwbPreset.DAYLIGHT -> CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT
                    AwbPreset.CLOUDY -> CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                    AwbPreset.SHADE -> CameraMetadata.CONTROL_AWB_MODE_SHADE
                    AwbPreset.AUTO -> CameraMetadata.CONTROL_AWB_MODE_AUTO
                }
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, mode)
            }
            Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(builder.build())
        } catch (e: Exception) {
            Log.w(tag, "applyParams: ${e.message}")
        }
    }

    fun tapToFocus(previewView: PreviewView, x: Float, y: Float, lock: Boolean) {
        val cam = activeCamera ?: return
        if (currentParams?.focusDiopters != null && capabilities.manualFocus) return
        try {
            val point = previewView.meteringPointFactory.createPoint(x, y)
            val manualExposure = capabilities.manualSensor &&
                ((currentParams?.iso ?: 0) > 0 || (currentParams?.shutterNs ?: 0L) > 0L)
            val flags = if (manualExposure) FocusMeteringAction.FLAG_AF else FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            val builder = FocusMeteringAction.Builder(point, flags)
            if (lock) builder.disableAutoCancel() else builder.setAutoCancelDuration(4, TimeUnit.SECONDS)
            cam.cameraControl.startFocusAndMetering(builder.build())
            vibrateTick(20)
        } catch (e: Exception) {
            Log.w(tag, "tapToFocus: ${e.message}")
        }
    }

    fun cancelFocusLock() {
        runCatching { activeCamera?.cameraControl?.cancelFocusAndMetering() }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: 0
            val exposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 0L
            val aeMode = result.get(CaptureResult.CONTROL_AE_MODE)
            if (aeMode != CameraMetadata.CONTROL_AE_MODE_OFF) {
                if (iso > 0) lastAutoIso = iso
                if (exposure > 0L) lastAutoExposureNs = exposure
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastLiveExposureMs < 250) return
            lastLiveExposureMs = now
            val focus = result.get(CaptureResult.LENS_FOCUS_DISTANCE) ?: 0f
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
            val info = LiveExposureInfo(
                iso = iso,
                exposureNs = exposure,
                focusDistanceDiopters = focus,
                afLocked = afState == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED,
                aeConverged = aeState == CameraMetadata.CONTROL_AE_STATE_CONVERGED ||
                    aeState == CameraMetadata.CONTROL_AE_STATE_LOCKED
            )
            mainExecutor.execute { listener.onLiveExposure(info) }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Photo capture
    // ------------------------------------------------------------------------------------------

    val isCaptureInProgress: Boolean get() = captureJob?.isActive == true

    /** Returns false when a timer countdown was cancelled instead of starting a capture. */
    fun capturePhoto(request: PhotoRequest): Boolean {
        if (captureJob?.isActive == true) {
            captureJob?.cancel()
            listener.onTimerTick(0)
            listener.onBusy(false)
            return false
        }
        captureJob = scope.launch(Dispatchers.Main) {
            try {
                if (request.timerSeconds > 0) {
                    for (s in request.timerSeconds downTo 1) {
                        listener.onTimerTick(s)
                        vibrateTick(if (s <= 3) 30 else 12)
                        delay(1000)
                    }
                    listener.onTimerTick(0)
                }
                val capture = imageCapture
                if (capture == null || activeCamera == null) {
                    listener.onMessage("Камера ещё не готова к съёмке")
                    return@launch
                }
                listener.onBusy(true)
                when (request.drive) {
                    PhotoDriveMode.SINGLE -> takeOne(capture, request)
                    PhotoDriveMode.BURST_5 -> repeat(5) { takeOne(capture, request) }
                    PhotoDriveMode.AEB_3 -> exposureBracket(capture, request)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                Log.w(tag, "capture failed", e)
                listener.onMessage("Ошибка съёмки: ${e.localizedMessage ?: e.javaClass.simpleName}")
            } finally {
                listener.onBusy(false)
            }
        }
        return true
    }

    private suspend fun exposureBracket(capture: ImageCapture, request: PhotoRequest) {
        val caps = capabilities
        val params = currentParams
        val manualExposure = caps.manualSensor && params != null && (params.iso > 0 || params.shutterNs > 0)
        if (manualExposure && params != null) {
            val baseNs = (if (params.shutterNs > 0) params.shutterNs else lastAutoExposureNs.takeIf { it > 0 } ?: 16_666_667L)
            for (factor in listOf(0.25f, 1f, 4f)) {
                applyParams(params.copy(shutterNs = (baseNs * factor).toLong().coerceIn(caps.exposureMinNs, caps.exposureMaxNs)))
                delay(400)
                takeOne(capture, request)
            }
            applyParams(params)
            return
        }
        if (caps.evStep <= 0f) {
            repeat(3) { takeOne(capture, request) }
            return
        }
        val cam = activeCamera ?: return
        val base = runCatching { cam.cameraInfo.exposureState.exposureCompensationIndex }.getOrDefault(0)
        val delta = (2f / caps.evStep).roundToInt().coerceAtLeast(1)
        for (offset in listOf(-delta, 0, delta)) {
            cam.cameraControl.setExposureCompensationIndex((base + offset).coerceIn(caps.evIndexMin, caps.evIndexMax))
            delay(450)
            takeOne(capture, request)
        }
        cam.cameraControl.setExposureCompensationIndex(base)
    }

    private suspend fun takeOne(capture: ImageCapture, request: PhotoRequest) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val format = activePhotoFormat
        val jpegName = "GlyphCam_$stamp.jpg"
        val dngName = "GlyphCam_$stamp.dng"

        fun options(name: String, mime: String) = ImageCapture.OutputFileOptions.Builder(
            resolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaValues(name, mime)
        ).build()

        val savedUri: Uri? = suspendCancellableCoroutine { cont ->
            val callback = object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (cont.isActive) cont.resume(outputFileResults.savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            }
            when (format) {
                PhotoFormat.RAW_PLUS_JPEG -> capture.takePicture(
                    options(dngName, "image/x-adobe-dng"),
                    options(jpegName, "image/jpeg"),
                    mainExecutor,
                    callback
                )
                PhotoFormat.RAW_DNG -> capture.takePicture(options(dngName, "image/x-adobe-dng"), mainExecutor, callback)
                else -> capture.takePicture(options(jpegName, "image/jpeg"), mainExecutor, callback)
            }
        }
        listener.onShutterFired()
        vibrateTick(35)

        val names = when (format) {
            PhotoFormat.RAW_PLUS_JPEG -> listOf(jpegName to "image/jpeg", dngName to "image/x-adobe-dng")
            PhotoFormat.RAW_DNG -> listOf(dngName to "image/x-adobe-dng")
            else -> listOf(jpegName to "image/jpeg")
        }
        val captures = withContext(Dispatchers.IO) {
            names.mapNotNull { (name, mime) ->
                val uri = findMediaUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, name)
                    ?: savedUri?.takeIf { names.size == 1 }
                    ?: return@mapNotNull null
                val isJpeg = mime == "image/jpeg"
                val baked = isJpeg && request.lutMatrix != null && bakeLutIntoJpeg(uri, request.lutMatrix, request.lutCode)
                describeImage(uri, name, mime, baked)
            }
        }
        captures.forEach { listener.onCaptureSaved(it) }
        if (captures.isEmpty()) listener.onMessage("Снимок сделан, но файл не найден в галерее")
    }

    private fun mediaValues(name: String, mime: String): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/GlyphCam")
        }
    }

    private fun findMediaUri(collection: Uri, displayName: String): Uri? {
        return try {
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(displayName),
                "${MediaStore.MediaColumns._ID} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Applies the colour matrix to the saved JPEG in place, keeping its EXIF block. */
    private fun bakeLutIntoJpeg(uri: Uri, matrix: FloatArray, lutCode: String): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val pixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
            if (pixels <= 0L || pixels > 26_000_000L) {
                Log.w(tag, "LUT bake skipped: $pixels px")
                return false
            }
            val exif = readExifTags(uri)
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                    inMutable = true
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                })
            } ?: return false
            applyMatrixInPlace(bitmap, matrix)
            resolver.openOutputStream(uri, "wt")?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: return false
            bitmap.recycle()
            writeExifTags(uri, exif, lutCode)
            true
        } catch (e: Throwable) {
            Log.w(tag, "LUT bake failed: ${e.message}")
            false
        }
    }

    private fun applyMatrixInPlace(bitmap: Bitmap, m: FloatArray) {
        val w = bitmap.width
        val h = bitmap.height
        val row = IntArray(w)
        for (y in 0 until h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val p = row[x]
                val a = p ushr 24
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val nr = (m[0] * r + m[1] * g + m[2] * b + m[4]).toInt().coerceIn(0, 255)
                val ng = (m[5] * r + m[6] * g + m[7] * b + m[9]).toInt().coerceIn(0, 255)
                val nb = (m[10] * r + m[11] * g + m[12] * b + m[14]).toInt().coerceIn(0, 255)
                row[x] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
            bitmap.setPixels(row, 0, w, 0, y, w, 1)
        }
    }

    private val exifTagsToKeep = listOf(
        ExifInterface.TAG_ORIENTATION, ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED, ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
        ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_FLASH, ExifInterface.TAG_EXPOSURE_BIAS_VALUE, ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifInterface.TAG_EXPOSURE_PROGRAM, ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF
    )

    private fun readExifTags(uri: Uri): Map<String, String> {
        return try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                exifTagsToKeep.mapNotNull { tag -> exif.getAttribute(tag)?.let { tag to it } }.toMap()
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writeExifTags(uri: Uri, tags: Map<String, String>, lutCode: String) {
        try {
            resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                tags.forEach { (tag, value) -> exif.setAttribute(tag, value) }
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, "GlyphCam Pro")
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "LUT $lutCode")
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            Log.w(tag, "EXIF restore failed: ${e.message}")
        }
    }

    private fun describeImage(uri: Uri, name: String, mime: String, baked: Boolean): SavedCapture {
        var width = 0
        var height = 0
        var size = 0L
        try {
            resolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT, MediaStore.MediaColumns.SIZE),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    width = c.getInt(0)
                    height = c.getInt(1)
                    size = c.getLong(2)
                }
            }
        } catch (_: Exception) {
        }
        var iso: Int? = null
        var exposure: Double? = null
        var fNumber: Double? = null
        var focal: Double? = null
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0).takeIf { it > 0 }
                exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0).takeIf { it > 0.0 }
                fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0.0 }
                focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0.0 }
                if (width == 0 || height == 0) {
                    width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, width)
                    height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, height)
                }
            }
        } catch (_: Exception) {
        }
        val thumb = loadThumbnail(uri, mime.endsWith("dng"))
        return SavedCapture(
            uri = uri.toString(),
            isVideo = false,
            displayName = name,
            mimeType = mime,
            width = width,
            height = height,
            sizeBytes = size,
            durationMs = 0L,
            iso = iso,
            exposureTimeSec = exposure,
            fNumber = fNumber,
            focalLengthMm = focal,
            lutBaked = baked,
            histogram16 = histogram16(thumb)
        )
    }

    private fun loadThumbnail(uri: Uri, isRaw: Boolean): Bitmap? {
        try {
            if (!isRaw) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
                resolver.openInputStream(uri)?.use {
                    return BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return resolver.loadThumbnail(uri, Size(512, 512), null)
            }
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                val bytes = exif.thumbnail ?: return null
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            Log.w(tag, "thumbnail failed: ${e.message}")
        }
        return null
    }

    private fun histogram16(bitmap: Bitmap?): IntArray {
        val bins = IntArray(16)
        if (bitmap == null) return bins
        val w = bitmap.width
        val h = bitmap.height
        if (w == 0 || h == 0) return bins
        val stepX = (w / 128).coerceAtLeast(1)
        val stepY = (h / 128).coerceAtLeast(1)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val p = bitmap.getPixel(x, y)
                val luma = (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
                bins[(luma * 15) / 255]++
                x += stepX
            }
            y += stepY
        }
        val max = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
        for (i in bins.indices) bins[i] = (bins[i] * 100) / max
        if (!bitmap.isRecycled) bitmap.recycle()
        return bins
    }

    // ------------------------------------------------------------------------------------------
    // Video recording
    // ------------------------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    fun toggleRecording(audioEnabled: Boolean) {
        activeRecording?.let {
            it.stop()
            return
        }
        val capture = videoCapture
        if (capture == null || activeCamera == null) {
            listener.onMessage("Видео недоступно: камера не готова")
            return
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = "GlyphCam_$stamp.mp4"
        val options = MediaStoreOutputOptions.Builder(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(mediaValues(name, "video/mp4"))
            .build()
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        var pending = capture.output.prepareRecording(context, options)
        if (audioEnabled && hasMic) pending = pending.withAudioEnabled()
        stopAudioMeter()
        recordingPeakDb = -60f
        recordingStartMs = SystemClock.elapsedRealtime()
        try {
            activeRecording = pending.start(mainExecutor) { event -> handleRecordEvent(event, name) }
        } catch (e: Exception) {
            activeRecording = null
            restartAudioMeterIfWanted()
            listener.onMessage("Не удалось начать запись: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private fun handleRecordEvent(event: VideoRecordEvent, name: String) {
        when (event) {
            is VideoRecordEvent.Start -> {
                vibrateTick(40)
                listener.onRecordingChanged(true, 0L)
            }
            is VideoRecordEvent.Status -> {
                val stats = event.recordingStats
                val elapsedMs = stats.recordedDurationNanos / 1_000_000L
                val audio = stats.audioStats
                if (audio.hasAudio()) {
                    val amplitude = audio.audioAmplitude
                    val db = if (amplitude > 0.0) (20.0 * log10(amplitude)).toFloat().coerceIn(-60f, 0f) else -60f
                    recordingPeakDb = maxOf(recordingPeakDb - 0.6f, db)
                    listener.onAudioLevels(db, db, recordingPeakDb, true)
                } else {
                    listener.onAudioLevels(-60f, -60f, -60f, false)
                }
                listener.onRecordingChanged(true, elapsedMs)
            }
            is VideoRecordEvent.Finalize -> {
                activeRecording = null
                val elapsedMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                listener.onRecordingChanged(false, elapsedMs)
                vibrateTick(45)
                restartAudioMeterIfWanted()
                val recoverable = !event.hasError() ||
                    event.error == VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED ||
                    event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED ||
                    event.error == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE ||
                    event.error == VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE
                val uri = event.outputResults.outputUri
                if (recoverable && uri != Uri.EMPTY) {
                    scope.launch(Dispatchers.IO) {
                        val capture = describeVideo(uri, name)
                        withContext(Dispatchers.Main) { listener.onCaptureSaved(capture) }
                    }
                    if (event.hasError()) listener.onMessage("Запись остановлена (код ${event.error}), файл сохранён")
                } else {
                    val reason = when (event.error) {
                        VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> "слишком короткая запись"
                        VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE -> "недостаточно места"
                        VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED -> "ошибка кодирования"
                        VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR -> "ошибка рекордера"
                        else -> "код ${event.error}"
                    }
                    listener.onMessage("Видео не сохранено: $reason")
                }
            }
            else -> Unit
        }
    }

    private fun describeVideo(uri: Uri, name: String): SavedCapture {
        var width = 0
        var height = 0
        var duration = 0L
        var thumb: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val t = width
                width = height
                height = t
            }
            thumb = retriever.getFrameAtTime(0)
        } catch (e: Exception) {
            Log.w(tag, "video metadata failed: ${e.message}")
        } finally {
            runCatching { retriever.release() }
        }
        var size = 0L
        try {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) size = c.getLong(0)
            }
        } catch (_: Exception) {
        }
        val scaled = thumb?.let { if (it.width > 512) Bitmap.createScaledBitmap(it, 512, 512 * it.height / it.width, true) else it }
        return SavedCapture(
            uri = uri.toString(),
            isVideo = true,
            displayName = name,
            mimeType = "video/mp4",
            width = width,
            height = height,
            sizeBytes = size,
            durationMs = duration,
            iso = null,
            exposureTimeSec = null,
            fNumber = null,
            focalLengthMm = null,
            lutBaked = false,
            histogram16 = histogram16(scaled)
        )
    }

    // ------------------------------------------------------------------------------------------
    // Sensors & audio meter
    // ------------------------------------------------------------------------------------------

    fun startSensors() {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val rollDeg = Math.toDegrees(atan2(-ax.toDouble(), ay.toDouble())).toFloat().coerceIn(-90f, 90f)
        val pitchDeg = Math.toDegrees(atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble()))).toFloat().coerceIn(-90f, 90f)
        filteredRoll += (rollDeg - filteredRoll) * 0.18f
        filteredPitch += (pitchDeg - filteredPitch) * 0.18f
        listener.onHorizon(filteredRoll, filteredPitch)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun setAudioMeterEnabled(enabled: Boolean) {
        audioMeterWanted = enabled
        if (enabled && activeRecording == null) startAudioMeter() else if (!enabled) stopAudioMeter()
    }

    private fun restartAudioMeterIfWanted() {
        if (audioMeterWanted) startAudioMeter()
    }

    @SuppressLint("MissingPermission")
    private fun startAudioMeter() {
        if (audioJob?.isActive == true) return
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            listener.onAudioLevels(-60f, -60f, -60f, false)
            return
        }
        audioJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 48_000
            var channels = 2
            var channelMask = AudioFormat.CHANNEL_IN_STEREO
            var minBuf = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuf <= 0) {
                channels = 1
                channelMask = AudioFormat.CHANNEL_IN_MONO
                minBuf = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
            }
            if (minBuf <= 0) {
                withContext(Dispatchers.Main) { listener.onAudioLevels(-60f, -60f, -60f, false) }
                return@launch
            }
            var record: AudioRecord? = null
            try {
                record = AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
                if (record.state != AudioRecord.STATE_INITIALIZED && channels == 2) {
                    record.release()
                    channels = 1
                    channelMask = AudioFormat.CHANNEL_IN_MONO
                    minBuf = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
                    record = AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
                }
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    withContext(Dispatchers.Main) { listener.onAudioLevels(-60f, -60f, -60f, false) }
                    return@launch
                }
                record.startRecording()
                val frames = 2048
                val buffer = ShortArray(frames * channels)
                var peak = -60f
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read <= 0) {
                        delay(40)
                        continue
                    }
                    var sumL = 0.0
                    var sumR = 0.0
                    var n = 0
                    var i = 0
                    while (i + channels - 1 < read) {
                        val l = buffer[i].toDouble()
                        val r = if (channels == 2) buffer[i + 1].toDouble() else l
                        sumL += l * l
                        sumR += r * r
                        n++
                        i += channels
                    }
                    if (n == 0) continue
                    val leftDb = rmsToDb(sqrt(sumL / n))
                    val rightDb = rmsToDb(sqrt(sumR / n))
                    peak = maxOf(peak - 0.5f, leftDb, rightDb)
                    val l = leftDb
                    val r = rightDb
                    val p = peak
                    withContext(Dispatchers.Main) { listener.onAudioLevels(l, r, p, true) }
                }
            } catch (e: Exception) {
                Log.w(tag, "audio meter: ${e.message}")
                withContext(Dispatchers.Main) { listener.onAudioLevels(-60f, -60f, -60f, false) }
            } finally {
                runCatching { record?.stop() }
                runCatching { record?.release() }
            }
        }
    }

    private fun rmsToDb(rms: Double): Float =
        if (rms < 1.0) -60f else (20.0 * log10(rms / 32768.0)).toFloat().coerceIn(-60f, 0f)

    private fun stopAudioMeter() {
        audioJob?.cancel()
        audioJob = null
    }

    // ------------------------------------------------------------------------------------------
    // Frame analysis: histogram, waveform, vectorscope, zebra, peaking, false colour
    // ------------------------------------------------------------------------------------------

    private fun analyzeFrame(image: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (now - lastTelemetryMs < 90) return
            lastTelemetryMs = now
            if (image.format != ImageFormat.YUV_420_888 || image.planes.size < 3) return

            val crop = image.cropRect
            val cropW = crop.width().coerceAtLeast(2)
            val cropH = crop.height().coerceAtLeast(2)
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]
            val yBuf = yPlane.buffer
            val uBuf = uPlane.buffer
            val vBuf = vPlane.buffer
            val yRow = yPlane.rowStride
            val yPix = yPlane.pixelStride
            val uvRow = uPlane.rowStride
            val uvPix = uPlane.pixelStride
            val yLimit = yBuf.limit()
            val uLimit = uBuf.limit()
            val vLimit = vBuf.limit()
            val rotation = image.imageInfo.rotationDegrees
            val mirror = frontCameraActive

            val cols = 96
            val rows = 72
            val lumaHist = IntArray(64)
            val rHist = IntArray(64)
            val gHist = IntArray(64)
            val bHist = IntArray(64)
            val waveSum = FloatArray(48)
            val waveCnt = IntArray(48)
            val zebraCnt = Array(12) { IntArray(16) }
            val cellCnt = Array(12) { IntArray(16) }
            val ireSum = Array(12) { IntArray(16) }
            val peaking = Array(16) { FloatArray(24) }
            val vecPoints = ArrayList<Pair<Float, Float>>(64)
            var totalLuma = 0L
            var totalR = 0L
            var totalB = 0L
            var samples = 0
            var clip = 0
            var crush = 0
            val threshold = zebraThresholdIre

            for (gy in 0 until rows) {
                val y = crop.top + (gy * (cropH - 1)) / (rows - 1)
                var prevLuma = -1
                for (gx in 0 until cols) {
                    val x = crop.left + (gx * (cropW - 1)) / (cols - 1)
                    val yIndex = y * yRow + x * yPix
                    val uvIndex = (y / 2) * uvRow + (x / 2) * uvPix
                    if (yIndex >= yLimit || uvIndex >= uLimit || uvIndex >= vLimit) continue
                    val luma = yBuf.get(yIndex).toInt() and 0xFF
                    val u = (uBuf.get(uvIndex).toInt() and 0xFF) - 128
                    val v = (vBuf.get(uvIndex).toInt() and 0xFF) - 128
                    val r = (luma + 1.402f * v).toInt().coerceIn(0, 255)
                    val g = (luma - 0.344f * u - 0.714f * v).toInt().coerceIn(0, 255)
                    val b = (luma + 1.772f * u).toInt().coerceIn(0, 255)

                    val nx = gx / (cols - 1f)
                    val ny = gy / (rows - 1f)
                    var dx: Float
                    var dy: Float
                    when (rotation) {
                        90 -> { dx = 1f - ny; dy = nx }
                        180 -> { dx = 1f - nx; dy = 1f - ny }
                        270 -> { dx = ny; dy = 1f - nx }
                        else -> { dx = nx; dy = ny }
                    }
                    if (mirror) dx = 1f - dx

                    lumaHist[(luma * 63) / 255]++
                    rHist[(r * 63) / 255]++
                    gHist[(g * 63) / 255]++
                    bHist[(b * 63) / 255]++
                    totalLuma += luma
                    totalR += r
                    totalB += b
                    samples++
                    val ire = (luma * 100) / 255
                    if (ire >= threshold) clip++
                    if (ire <= 4) crush++

                    val wCol = (dx * 47.999f).toInt().coerceIn(0, 47)
                    waveSum[wCol] += luma / 255f
                    waveCnt[wCol]++

                    val zr = (dy * 11.999f).toInt().coerceIn(0, 11)
                    val zc = (dx * 15.999f).toInt().coerceIn(0, 15)
                    cellCnt[zr][zc]++
                    ireSum[zr][zc] += ire
                    if (ire >= threshold) zebraCnt[zr][zc]++

                    if (prevLuma >= 0) {
                        val grad = abs(luma - prevLuma) / 255f
                        val pr = (dy * 15.999f).toInt().coerceIn(0, 15)
                        val pc = (dx * 23.999f).toInt().coerceIn(0, 23)
                        val strength = (grad * 3.2f).coerceIn(0f, 1f)
                        if (strength > peaking[pr][pc]) peaking[pr][pc] = strength
                    }
                    prevLuma = luma

                    if ((gx + gy * cols) % 97 == 0 && vecPoints.size < 64) {
                        vecPoints.add(Pair((u / 128f).coerceIn(-1f, 1f), (v / 128f).coerceIn(-1f, 1f)))
                    }
                }
            }
            if (samples == 0) return

            val maxBin = lumaHist.maxOrNull()?.coerceAtLeast(1) ?: 1
            val maxRgb = maxOf(rHist.maxOrNull() ?: 1, gHist.maxOrNull() ?: 1, bHist.maxOrNull() ?: 1).coerceAtLeast(1)
            val lumaOut = IntArray(64) { (lumaHist[it] * 100) / maxBin }
            val rOut = IntArray(64) { (rHist[it] * 100) / maxRgb }
            val gOut = IntArray(64) { (gHist[it] * 100) / maxRgb }
            val bOut = IntArray(64) { (bHist[it] * 100) / maxRgb }
            val waveform = FloatArray(48) { if (waveCnt[it] > 0) waveSum[it] / waveCnt[it] else 0f }
            val zebra = Array(12) { r -> BooleanArray(16) { c -> cellCnt[r][c] > 0 && zebraCnt[r][c] * 4 >= cellCnt[r][c] } }
            val falseColor = Array(12) { r -> IntArray(16) { c -> if (cellCnt[r][c] > 0) ireSum[r][c] / cellCnt[r][c] else 0 } }

            // Scene contrast between the 2nd and 98th luma percentiles, in stops
            var acc = 0
            var p2 = 0
            var p98 = 63
            for (i in 0 until 64) {
                acc += lumaHist[i]
                if (acc >= samples * 0.02f) { p2 = i; break }
            }
            acc = 0
            for (i in 63 downTo 0) {
                acc += lumaHist[i]
                if (acc >= samples * 0.02f) { p98 = i; break }
            }
            val contrastStops = (ln((p98 * 4f + 4f) / (p2 * 4f + 4f)) / ln(2f)).coerceIn(0f, 8f)
            val avgR = totalR.toFloat() / samples
            val avgB = totalB.toFloat() / samples
            val tempK = if (avgR > 1f && avgB > 1f) (5500f * (avgB / avgR).coerceIn(0.4f, 1.8f)).roundToInt() else 0

            val telemetry = LiveScopeTelemetry(
                hasFrame = true,
                lumaHistogram = lumaOut,
                redHistogram = rOut,
                greenHistogram = gOut,
                blueHistogram = bOut,
                waveformColumns = waveform,
                vectorscopePoints = vecPoints,
                zebraGrid = zebra,
                peakingGrid = peaking,
                falseColorGrid = falseColor,
                averageIre = ((totalLuma / samples) * 100 / 255).toInt(),
                highlightClipPercent = clip * 100f / samples,
                shadowCrushPercent = crush * 100f / samples,
                sceneContrastStops = contrastStops,
                colorTempEstimateK = tempK
            )
            mainExecutor.execute { listener.onTelemetry(telemetry) }
        } catch (e: Exception) {
            Log.w(tag, "analyze: ${e.message}")
        } finally {
            image.close()
        }
    }

    // ------------------------------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------------------------------

    fun vibrateTick(durationMs: Long = 18L) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        captureJob?.cancel()
        stopAudioMeter()
        stopSensors()
        runCatching { activeRecording?.stop() }
        runCatching { zoomLive?.removeObserver(zoomObserver) }
        runCatching { cameraProvider?.unbindAll() }
        analysisExecutor.shutdown()
    }
}
