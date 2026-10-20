package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.view.Surface
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.camera.ProCameraController
import com.example.data.CameraPresetEntity
import com.example.data.CameraRepository
import com.example.data.CapturedShotEntity
import com.example.data.GlyphCamDatabase
import com.example.data.UserSettings
import com.example.glyph.GlyphBridge
import com.example.model.AnamorphicDeSqueeze
import com.example.model.AntiBanding
import com.example.model.AudioSourceOption
import com.example.model.AppMode
import com.example.model.AwbPreset
import com.example.model.CameraCapabilities
import com.example.model.CameraStatus
import com.example.model.CapturePriority
import com.example.model.FlashMode
import com.example.model.FocusPeakingColor
import com.example.model.FocusPullPoint
import com.example.model.FrameAspectGuide
import com.example.model.GlyphLightingMode
import com.example.model.GridOverlayMode
import com.example.model.IntervalCount
import com.example.model.IntervalSpacing
import com.example.model.JpegQuality
import com.example.model.LensKind
import com.example.model.LensOption
import com.example.model.LiveExposureInfo
import com.example.model.LiveScopeTelemetry
import com.example.model.LutProfile
import com.example.model.PhotoAspect
import com.example.model.PhotoDriveMode
import com.example.model.PhotoFormat
import com.example.model.ProcessingMode
import com.example.model.RecordingLimit
import com.example.model.SceneExtension
import com.example.model.SavedCapture
import com.example.model.SelfTimer
import com.example.model.VideoBitrate
import com.example.model.VideoDynamicRange
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.model.ZebraThreshold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

enum class ManualDialTab(val labelRu: String) {
    ISO("ISO"),
    SHUTTER("Выдержка"),
    FOCUS("Фокус"),
    WB("Баланс белого"),
    EV("Экспокоррекция"),
    AUDIO("Звук")
}

/** A HUD chip whose options open as a labelled sheet instead of blind cycling. */
enum class QuickSetting(val titleRu: String) {
    PHOTO_FORMAT("Формат файла"),
    DRIVE("Режим съёмки"),
    INTERVAL("Интервальная съёмка"),
    TIMER("Автоспуск"),
    FLASH("Вспышка"),
    ASPECT("Формат кадра"),
    SCENE("Сцена и обработка"),
    QUALITY("Качество и приоритет"),
    RESOLUTION("Разрешение видео"),
    FPS("Частота кадров"),
    DYNAMIC_RANGE("Динамический диапазон"),
    BITRATE("Битрейт видео"),
    DESQUEEZE("Анаморфотный де-сквиз"),
    GUIDE("Рамка кадра"),
    PEAKING("Пикинг фокуса"),
    ZEBRA("Зебра"),
    GRID("Сетка")
}

val AppMode.isCameraMode: Boolean
    get() = this == AppMode.PHOTO_PRO || this == AppMode.CINE_VIDEO

/** Everything the bind depends on; a change here re-binds the CameraX use cases. */
data class CameraBindKey(
    val mode: AppMode,
    val useFront: Boolean,
    val photoFormat: PhotoFormat,
    val videoResolution: VideoResolution,
    val videoFps: VideoFps,
    val dynamicRange: VideoDynamicRange,
    val stabilization: Boolean,
    val photoAspect: PhotoAspect,
    val jpegQuality: JpegQuality,
    val capturePriority: CapturePriority,
    val videoBitrate: VideoBitrate,
    val sceneExtension: SceneExtension,
    val cameraId: String?,
    val slowMotion: Boolean,
    val audioSource: AudioSourceOption,
    val mirrorSelfie: Boolean
)

data class ProCameraUiState(
    val activeMode: AppMode = AppMode.PHOTO_PRO,
    val lastCameraMode: AppMode = AppMode.PHOTO_PRO,
    val cameraStatus: CameraStatus = CameraStatus.NoPermission,
    val capabilities: CameraCapabilities = CameraCapabilities(),
    val useFrontCamera: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val selectedDialTab: ManualDialTab? = null,

    // Exposure & focus
    val isoValue: Int = 0, // 0 = AUTO
    val shutterNs: Long = 0L, // 0 = AUTO
    val shutterAngleDeg: Float = 180f,
    val useShutterAngleInVideo: Boolean = true,
    val awbPreset: AwbPreset = AwbPreset.AUTO,
    val evCompensation: Float = 0.0f,
    val manualFocusDiopters: Float? = null, // null = AF
    val focusPullPoints: List<FocusPullPoint> = listOf(
        FocusPullPoint("A", 5.0f, "0.20 м"),
        FocusPullPoint("B", 1.0f, "1.00 м"),
        FocusPullPoint("C", 0.0f, "∞")
    ),
    val activeFocusPullSlot: String? = null,
    val focusPullDurationMs: Long = 1400L,
    val aeAfLocked: Boolean = false,
    val tapFocusNormalized: Pair<Float, Float>? = null,
    val liveExposure: LiveExposureInfo = LiveExposureInfo(),

    // Photo
    val photoFormat: PhotoFormat = PhotoFormat.JPEG,
    val photoDriveMode: PhotoDriveMode = PhotoDriveMode.SINGLE,
    val selfTimer: SelfTimer = SelfTimer.OFF,
    val flashMode: FlashMode = FlashMode.OFF,
    val timerCountdown: Int = 0,
    val isCapturing: Boolean = false,

    // Video
    val videoResolution: VideoResolution = VideoResolution.UHD_4K,
    val videoFps: VideoFps = VideoFps.FPS_30,
    val videoDynamicRange: VideoDynamicRange = VideoDynamicRange.SDR,
    val stabilization: Boolean = false,
    val anamorphicDeSqueeze: AnamorphicDeSqueeze = AnamorphicDeSqueeze.NONE,
    val aspectGuide: FrameAspectGuide = FrameAspectGuide.OFF,
    val aspectMaskOpacity: Float = 0.65f,

    // Audio
    val audioEnabled: Boolean = true,
    val audioAvailable: Boolean = false,
    val leftAudioDb: Float = -60f,
    val rightAudioDb: Float = -60f,
    val peakAudioDb: Float = -60f,

    // Scopes & overlays
    val showHistogramRgb: Boolean = false,
    val showWaveformScope: Boolean = false,
    val showVectorscope: Boolean = false,
    val showFalseColor: Boolean = false,
    val zebraThreshold: ZebraThreshold = ZebraThreshold.OFF,
    val focusPeakingColor: FocusPeakingColor = FocusPeakingColor.OFF,
    val gridOverlayMode: GridOverlayMode = GridOverlayMode.RULE_OF_THIRDS,
    val showHorizonLevel: Boolean = true,

    // Glyph
    val glyphMode: GlyphLightingMode = GlyphLightingMode.OFF,
    val activeGlyphSegments: Set<Int> = emptySet(),
    val glyphBrightness: Int = 70,
    val glyphTorchHardwareSync: Boolean = false,
    val glyphSupported: Boolean = false,
    val glyphConnected: Boolean = false,
    val glyphDeviceName: String = "",
    val glyphMessage: String = "",

    // LUT & grading
    val activeLut: LutProfile = LutProfile.NOTHING_STD,
    val userContrast: Float = 1.0f,
    val userSaturation: Float = 1.0f,
    val shadowLift: Float = 0.0f,
    val highlightRollOff: Float = 0.15f,
    val bakeLutIntoPhotos: Boolean = true,

    // Live telemetry & recording
    val scopeTelemetry: LiveScopeTelemetry = LiveScopeTelemetry(),
    val rollDegrees: Float = 0.0f,
    val pitchDegrees: Float = 0.0f,
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val shutterFlashTrigger: Long = 0L,
    val statusBanner: String = "",
    val inspectShot: CapturedShotEntity? = null,

    // Pro pack
    val photoAspect: PhotoAspect = PhotoAspect.FULL_4_3,
    val jpegQuality: JpegQuality = JpegQuality.Q95,
    val capturePriority: CapturePriority = CapturePriority.QUALITY,
    val sceneExtension: SceneExtension = SceneExtension.NONE,
    val intervalSpacing: IntervalSpacing = IntervalSpacing.S2,
    val intervalCount: IntervalCount = IntervalCount.C30,
    val intervalShotsTaken: Int = 0,
    val intervalRunning: Boolean = false,
    val videoBitrate: VideoBitrate = VideoBitrate.AUTO,
    val isRecordingPaused: Boolean = false,
    val aeLocked: Boolean = false,
    val awbLocked: Boolean = false,
    val antiBanding: AntiBanding = AntiBanding.AUTO,
    val noiseReduction: ProcessingMode = ProcessingMode.AUTO,
    val edgeMode: ProcessingMode = ProcessingMode.AUTO,
    val focusMagnifier: Boolean = false,
    val shutterSound: Boolean = true,
    val volumeKeyShutter: Boolean = true,
    val mirrorSelfie: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val quickSetting: QuickSetting? = null,
    val zoomRulerUntilMs: Long = 0L,
    val deviceRotationDeg: Int = 0,
    val activeCameraId: String? = null, // null = default camera of the facing
    val slowMotion: Boolean = true,
    val audioSource: AudioSourceOption = AudioSourceOption.CAMCORDER,
    val recordingLimit: RecordingLimit = RecordingLimit.NONE
) {
    val isVideoMode: Boolean get() = activeMode == AppMode.CINE_VIDEO
    val cameraMode: AppMode get() = if (activeMode.isCameraMode) activeMode else lastCameraMode
    val isManualExposure: Boolean get() = isoValue > 0 || shutterNs > 0L

    val bindKey: CameraBindKey
        get() = CameraBindKey(
            cameraMode, useFrontCamera, photoFormat, videoResolution, videoFps, videoDynamicRange, stabilization,
            photoAspect, jpegQuality, capturePriority, videoBitrate, if (cameraMode == AppMode.CINE_VIDEO) SceneExtension.NONE else sceneExtension,
            activeCameraId, slowMotion, audioSource, mirrorSelfie
        )

    /** Recording at 120 fps or more through a constrained high-speed session. */
    val isSlowMotionMode: Boolean get() = cameraMode == AppMode.CINE_VIDEO && videoFps.isHighSpeed

    /** Width / height of the viewfinder box. */
    val viewfinderAspect: Float
        get() = if (cameraMode == AppMode.CINE_VIDEO) 9f / 16f else 1f / photoAspect.ratio

    /** A vendor scene mode is running: the camera owns exposure, focus and processing. */
    val isExtensionActive: Boolean
        get() = cameraMode != AppMode.CINE_VIDEO && sceneExtension != SceneExtension.NONE &&
            capabilities.availableExtensions.contains(sceneExtension)

    val intervalProgressLabel: String
        get() = if (intervalCount.count > 0) "$intervalShotsTaken / ${intervalCount.count}" else "$intervalShotsTaken"

    val formattedTimecode: String
        get() {
            val totalSec = recordingElapsedMs / 1000L
            val hrs = totalSec / 3600
            val mins = (totalSec % 3600) / 60
            val secs = totalSec % 60
            val frames = ((recordingElapsedMs % 1000L) * videoFps.fps / 1000L).toInt()
            return String.format(Locale.US, "%02d:%02d:%02d:%02d", hrs, mins, secs, frames)
        }

    val shutterDisplayLabel: String
        get() {
            if (shutterNs <= 0L) return "AUTO"
            if (isVideoMode && useShutterAngleInVideo) return String.format(Locale.US, "%.0f°", shutterAngleDeg)
            return formatExposure(shutterNs)
        }

    /** What the sensor is doing right now, even in auto mode. */
    val liveShutterLabel: String
        get() = if (liveExposure.exposureNs > 0L) formatExposure(liveExposure.exposureNs) else "—"

    val liveIsoLabel: String
        get() = if (liveExposure.iso > 0) liveExposure.iso.toString() else "—"

    val isoDisplayLabel: String
        get() = if (isoValue <= 0) "AUTO" else isoValue.toString()

    val wbDisplayLabel: String
        get() = awbPreset.code

    val focusDisplayLabel: String
        get() {
            val d = manualFocusDiopters ?: return "AF"
            return formatDiopters(d)
        }

    val zoomLabel: String
        get() = if (zoomRatio < 0.95f || zoomRatio - zoomRatio.toInt() > 0.05f) {
            String.format(Locale.US, "%.1f×", zoomRatio)
        } else {
            "${zoomRatio.roundToInt()}×"
        }

    val selectedLensId: String?
        get() {
            val activeId = activeCameraId ?: capabilities.activeCameraId
            return capabilities.lensOptions
                .filter { it.cameraId == null || it.cameraId == activeId }
                .minByOrNull { kotlin.math.abs(it.zoomRatio - zoomRatio) }
                ?.takeIf { kotlin.math.abs(it.zoomRatio - zoomRatio) < 0.08f }?.id
        }

    val lutColorMatrix: FloatArray?
        get() = if (activeLut.isIdentity && userContrast == 1f && userSaturation == 1f && shadowLift == 0f) null
        else activeLut.buildColorMatrix(userContrast, userSaturation, shadowLift, highlightRollOff).values
}

fun formatExposure(ns: Long): String {
    if (ns <= 0L) return "—"
    if (ns >= 1_000_000_000L) return String.format(Locale.US, "%.1f\"", ns / 1_000_000_000.0)
    val denom = (1_000_000_000.0 / ns).roundToInt().coerceAtLeast(1)
    return "1/$denom"
}

fun formatDiopters(d: Float): String {
    if (d <= 0.05f) return "∞"
    val meters = 1f / d
    return if (meters >= 10f) "∞" else String.format(Locale.US, "%.2f м", meters)
}

class ProCameraViewModel(application: Application) : AndroidViewModel(application), ProCameraController.Listener {

    private val repository: CameraRepository
    private val _uiState = MutableStateFlow(ProCameraUiState())
    val uiState: StateFlow<ProCameraUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<CameraPresetEntity>>
    val capturedShots: StateFlow<List<CapturedShotEntity>>

    val controller = ProCameraController(application, viewModelScope, this)
    private val glyph = GlyphBridge(application) { st ->
        _uiState.update {
            it.copy(
                glyphSupported = st.supported,
                glyphConnected = st.connected,
                glyphDeviceName = st.deviceName,
                glyphMessage = st.message
            )
        }
    }

    private var focusPullJob: Job? = null
    private var lastGlyphProgressMs = 0L

    init {
        val settings = UserSettings.load(application)
        _uiState.update {
            it.copy(
                jpegQuality = settings.jpegQuality,
                capturePriority = settings.capturePriority,
                photoAspect = settings.photoAspect,
                videoBitrate = settings.videoBitrate,
                antiBanding = settings.antiBanding,
                noiseReduction = settings.noiseReduction,
                edgeMode = settings.edgeMode,
                shutterSound = settings.shutterSound,
                volumeKeyShutter = settings.volumeKeyShutter,
                mirrorSelfie = settings.mirrorSelfie,
                hapticsEnabled = settings.hapticsEnabled,
                bakeLutIntoPhotos = settings.bakeLutIntoPhotos,
                keepScreenOn = settings.keepScreenOn,
                audioSource = settings.audioSource,
                recordingLimit = settings.recordingLimit,
                slowMotion = settings.slowMotion
            )
        }
        controller.shutterSoundEnabled = settings.shutterSound
        controller.hapticsEnabled = settings.hapticsEnabled
        val db = GlyphCamDatabase.getInstance(application)
        repository = CameraRepository(db.cameraDao())
        presets = repository.presets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        capturedShots = repository.capturedShots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        viewModelScope.launch {
            repository.seedFactoryPresetsIfNeeded()
            purgeMissingShots()
        }
        glyph.connect()
    }

    // ------------------------------------------------------------------------------------------
    // Camera lifecycle
    // ------------------------------------------------------------------------------------------

    fun bindCamera(owner: LifecycleOwner, previewView: PreviewView) {
        val s = _uiState.value
        controller.bind(
            owner,
            previewView,
            ProCameraController.BindRequest(
                mode = s.cameraMode,
                useFront = s.useFrontCamera,
                photoFormat = s.photoFormat,
                videoResolution = s.videoResolution,
                fps = s.videoFps,
                dynamicRange = s.videoDynamicRange,
                stabilization = s.stabilization,
                jpegQuality = s.jpegQuality.value,
                capturePriority = s.capturePriority,
                photoAspect = s.photoAspect,
                videoBitrateMbps = s.videoBitrate.mbps,
                extension = s.bindKey.sceneExtension,
                cameraId = s.activeCameraId,
                slowMotion = s.slowMotion,
                audioSource = s.audioSource.mediaRecorderSource,
                mirrorFrontVideo = s.mirrorSelfie
            )
        )
    }

    fun onPermissionsChanged(camera: Boolean) {
        if (!camera) _uiState.update { it.copy(cameraStatus = CameraStatus.NoPermission) }
    }

    fun onForeground() {
        controller.startSensors()
        refreshAudioMeter()
    }

    fun onBackground() {
        controller.stopSensors()
        controller.setAudioMeterEnabled(false)
    }

    private fun refreshAudioMeter() {
        val s = _uiState.value
        controller.setAudioMeterEnabled(s.cameraMode == AppMode.CINE_VIDEO && s.audioEnabled && s.activeMode.isCameraMode)
    }

    private fun pushParams() {
        val s = _uiState.value
        controller.applyParams(
            ProCameraController.ManualParams(
                iso = s.isoValue,
                shutterNs = s.shutterNs,
                awb = s.awbPreset,
                evCompensation = s.evCompensation,
                focusDiopters = s.manualFocusDiopters,
                torch = s.glyphTorchHardwareSync,
                flash = s.flashMode,
                zebraIre = s.zebraThreshold.irePercent,
                isVideo = s.cameraMode == AppMode.CINE_VIDEO,
                aeLock = s.aeLocked,
                awbLock = s.awbLocked,
                antiBanding = s.antiBanding,
                noiseReduction = s.noiseReduction,
                edgeMode = s.edgeMode
            )
        )
    }

    private fun persistSettings() {
        val s = _uiState.value
        UserSettings(
            jpegQuality = s.jpegQuality,
            capturePriority = s.capturePriority,
            photoAspect = s.photoAspect,
            videoBitrate = s.videoBitrate,
            antiBanding = s.antiBanding,
            noiseReduction = s.noiseReduction,
            edgeMode = s.edgeMode,
            shutterSound = s.shutterSound,
            volumeKeyShutter = s.volumeKeyShutter,
            mirrorSelfie = s.mirrorSelfie,
            hapticsEnabled = s.hapticsEnabled,
            bakeLutIntoPhotos = s.bakeLutIntoPhotos,
            keepScreenOn = s.keepScreenOn,
            audioSource = s.audioSource,
            recordingLimit = s.recordingLimit,
            slowMotion = s.slowMotion
        ).save(getApplication())
        controller.shutterSoundEnabled = s.shutterSound
        controller.hapticsEnabled = s.hapticsEnabled
    }

    private fun pushGlyph() {
        val s = _uiState.value
        if (s.glyphMode == GlyphLightingMode.OFF) {
            glyph.off()
            return
        }
        val segments = if (s.glyphMode == GlyphLightingMode.TALLY_AND_AUDIO_VU && !s.isRecording) {
            s.activeGlyphSegments - 10
        } else s.activeGlyphSegments
        glyph.apply(segments, s.glyphBrightness)
    }

    // ------------------------------------------------------------------------------------------
    // Controller callbacks
    // ------------------------------------------------------------------------------------------

    override fun onStatus(status: CameraStatus) {
        _uiState.update {
            it.copy(
                cameraStatus = status,
                statusBanner = when (status) {
                    is CameraStatus.Ready -> status.detail
                    is CameraStatus.Error -> status.message
                    else -> it.statusBanner
                }
            )
        }
        if (status is CameraStatus.Ready) {
            pushParams()
            refreshAudioMeter()
        }
    }

    override fun onCapabilities(capabilities: CameraCapabilities) {
        _uiState.update { s ->
            val resolution = if (capabilities.videoResolutions.contains(s.videoResolution)) s.videoResolution
            else capabilities.videoResolutions.first()
            val fps = when {
                capabilities.supports(resolution, s.videoFps) -> s.videoFps
                else -> capabilities.fpsFor(resolution).filter { !it.isHighSpeed && it.fps <= s.videoFps.fps }.lastOrNull()
                    ?: capabilities.fpsFor(resolution).firstOrNull()
                    ?: VideoFps.FPS_30
            }
            s.copy(
                capabilities = capabilities,
                photoFormat = if (capabilities.photoFormats.contains(s.photoFormat)) s.photoFormat else PhotoFormat.JPEG,
                videoResolution = resolution,
                videoFps = fps,
                activeCameraId = if (s.activeCameraId == null) null else capabilities.activeCameraId.takeIf { it.isNotEmpty() },
                videoDynamicRange = if (capabilities.hdrVideo) s.videoDynamicRange else VideoDynamicRange.SDR,
                flashMode = if (capabilities.hasFlash) s.flashMode else FlashMode.OFF,
                evCompensation = if (capabilities.evRange > 0f) s.evCompensation.coerceIn(-capabilities.evRange, capabilities.evRange) else 0f,
                sceneExtension = if (capabilities.availableExtensions.contains(s.sceneExtension)) s.sceneExtension else SceneExtension.NONE,
                antiBanding = if (capabilities.antiBandingModes.contains(s.antiBanding)) s.antiBanding else AntiBanding.AUTO,
                noiseReduction = if (capabilities.noiseReductionModes.contains(s.noiseReduction)) s.noiseReduction else ProcessingMode.AUTO,
                edgeMode = if (capabilities.edgeModes.contains(s.edgeMode)) s.edgeMode else ProcessingMode.AUTO
            )
        }
    }

    override fun onZoomChanged(ratio: Float, minRatio: Float, maxRatio: Float) {
        _uiState.update {
            val moved = kotlin.math.abs(it.zoomRatio - ratio) > 0.01f
            it.copy(zoomRatio = ratio, zoomRulerUntilMs = if (moved) SystemClock.elapsedRealtime() + 1600L else it.zoomRulerUntilMs)
        }
    }

    override fun onIntervalProgress(shotsTaken: Int, totalShots: Int, running: Boolean) {
        _uiState.update { it.copy(intervalShotsTaken = shotsTaken, intervalRunning = running) }
    }

    override fun onRecordingPaused(paused: Boolean) {
        _uiState.update { it.copy(isRecordingPaused = paused, statusBanner = if (paused) "Запись на паузе" else "Запись продолжается") }
    }

    override fun onDeviceRotation(surfaceRotation: Int) {
        val degrees = when (surfaceRotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        _uiState.update { it.copy(deviceRotationDeg = degrees) }
    }

    override fun onTelemetry(telemetry: LiveScopeTelemetry) {
        _uiState.update { it.copy(scopeTelemetry = telemetry) }
    }

    override fun onLiveExposure(info: LiveExposureInfo) {
        _uiState.update { it.copy(liveExposure = info) }
    }

    override fun onHorizon(rollDeg: Float, pitchDeg: Float) {
        _uiState.update { it.copy(rollDegrees = rollDeg, pitchDegrees = pitchDeg) }
    }

    override fun onAudioLevels(leftDb: Float, rightDb: Float, peakDb: Float, available: Boolean) {
        _uiState.update { it.copy(leftAudioDb = leftDb, rightAudioDb = rightDb, peakAudioDb = peakDb, audioAvailable = available) }
        val s = _uiState.value
        if (s.glyphConnected && s.glyphMode == GlyphLightingMode.TALLY_AND_AUDIO_VU && s.isRecording) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastGlyphProgressMs > 160) {
                lastGlyphProgressMs = now
                glyph.showProgress(((peakDb + 60f) / 60f * 100f).roundToInt())
            }
        }
    }

    override fun onRecordingChanged(isRecording: Boolean, elapsedMs: Long) {
        val wasRecording = _uiState.value.isRecording
        _uiState.update { it.copy(isRecording = isRecording, recordingElapsedMs = elapsedMs) }
        if (wasRecording != isRecording) {
            if (isRecording) {
                _uiState.update { it.copy(statusBanner = "Идёт запись") }
            }
            pushGlyph()
        }
    }

    override fun onCaptureSaved(capture: SavedCapture) {
        val s = _uiState.value
        val caps = s.capabilities
        val focalEquiv = caps.focalEquivMm.takeIf { it > 0 }?.let { (it * s.zoomRatio).roundToInt() }
        val focalLabel = buildString {
            capture.focalLengthMm?.let { append(String.format(Locale.US, "%.1f мм", it)) }
            if (focalEquiv != null) {
                if (isNotEmpty()) append(" · ")
                append("≈$focalEquiv мм экв.")
            }
        }
        val shutterLabel = when {
            capture.exposureTimeSec != null -> formatExposure((capture.exposureTimeSec * 1_000_000_000.0).toLong())
            capture.isVideo && s.shutterNs > 0L -> s.shutterDisplayLabel
            s.liveExposure.exposureNs > 0L -> s.liveShutterLabel
            else -> "—"
        }
        val entity = CapturedShotEntity(
            uri = capture.uri,
            isVideo = capture.isVideo,
            displayName = capture.displayName,
            mimeType = capture.mimeType,
            width = capture.width,
            height = capture.height,
            sizeBytes = capture.sizeBytes,
            durationMs = capture.durationMs,
            isoUsed = capture.iso ?: s.liveExposure.iso.takeIf { it > 0 } ?: s.isoValue,
            shutterLabel = shutterLabel,
            apertureLabel = capture.fNumber?.let { String.format(Locale.US, "f/%.1f", it) } ?: caps.apertureLabel,
            focalLabel = focalLabel,
            zoomRatio = s.zoomRatio,
            wbLabel = s.awbPreset.code,
            evValue = s.evCompensation,
            lutApplied = s.activeLut.code,
            lutBaked = capture.lutBaked,
            glyphModeUsed = s.glyphMode.code,
            cameraLabel = caps.cameraLabel,
            histogramSummary = capture.histogram16.joinToString(",")
        )
        viewModelScope.launch {
            repository.logCapturedShot(entity)
            _uiState.update {
                it.copy(
                    statusBanner = if (capture.isVideo) {
                        "Видео сохранено · ${capture.displayName}"
                    } else {
                        "Снимок сохранён · ${capture.displayName}" + if (capture.lutBaked) " · LUT ${s.activeLut.code}" else ""
                    }
                )
            }
        }
    }

    override fun onShutterFired() {
        _uiState.update { it.copy(shutterFlashTrigger = System.currentTimeMillis()) }
    }

    override fun onTimerTick(secondsLeft: Int) {
        _uiState.update { it.copy(timerCountdown = secondsLeft) }
    }

    override fun onBusy(busy: Boolean) {
        _uiState.update { it.copy(isCapturing = busy) }
    }

    override fun onMessage(text: String) {
        _uiState.update { it.copy(statusBanner = text) }
    }

    // ------------------------------------------------------------------------------------------
    // Navigation & capture
    // ------------------------------------------------------------------------------------------

    fun setAppMode(mode: AppMode) {
        _uiState.update {
            it.copy(
                activeMode = mode,
                lastCameraMode = if (mode.isCameraMode) mode else it.lastCameraMode,
                selectedDialTab = if (mode.isCameraMode) it.selectedDialTab else null,
                quickSetting = null,
                focusMagnifier = false
            )
        }
        refreshAudioMeter()
    }

    /** Volume keys act as a shutter release when enabled in settings. Returns true when consumed. */
    fun onVolumeKey(): Boolean {
        val s = _uiState.value
        if (!s.volumeKeyShutter || !s.activeMode.isCameraMode) return false
        triggerShutter()
        return true
    }

    fun triggerShutter() {
        val s = _uiState.value
        if (s.cameraStatus !is CameraStatus.Ready) {
            _uiState.update { it.copy(statusBanner = "Камера ещё не запущена") }
            return
        }
        if (s.isVideoMode) {
            controller.toggleRecording(s.audioEnabled, s.recordingLimit.minutes * 60_000L)
        } else {
            val wasInterval = controller.isIntervalRunning
            val started = controller.capturePhoto(
                ProCameraController.PhotoRequest(
                    drive = s.photoDriveMode,
                    timerSeconds = s.selfTimer.seconds,
                    lutMatrix = if (s.bakeLutIntoPhotos) s.lutColorMatrix else null,
                    lutCode = s.activeLut.code,
                    intervalSeconds = s.intervalSpacing.seconds,
                    intervalCount = s.intervalCount.count,
                    mirrorFront = s.mirrorSelfie
                )
            )
            if (!started && !wasInterval) _uiState.update { it.copy(statusBanner = "Таймер отменён", timerCountdown = 0) }
        }
    }

    fun togglePauseRecording() {
        if (!_uiState.value.isRecording) return
        controller.pauseResumeRecording()
    }

    /** Still frame from the live preview while a video is being recorded. */
    fun snapshotWhileRecording(previewView: PreviewView) {
        val s = _uiState.value
        controller.snapshotFromPreview(previewView, if (s.bakeLutIntoPhotos) s.lutColorMatrix else null)
    }

    fun toggleFocusMagnifier() {
        _uiState.update {
            it.copy(
                focusMagnifier = !it.focusMagnifier,
                statusBanner = if (!it.focusMagnifier) "Лупа фокуса ×2 (только превью)" else "Лупа выключена"
            )
        }
    }

    fun toggleAeLock() {
        _uiState.update { s ->
            if (s.isManualExposure) return@update s.copy(statusBanner = "Экспозиция уже ручная, блокировка AE не нужна")
            s.copy(aeLocked = !s.aeLocked, statusBanner = if (!s.aeLocked) "Экспозиция заблокирована" else "Блокировка экспозиции снята")
        }
        pushParams()
    }

    fun toggleAwbLock() {
        _uiState.update { s ->
            if (s.awbPreset != AwbPreset.AUTO) return@update s.copy(statusBanner = "Баланс белого уже задан вручную")
            s.copy(awbLocked = !s.awbLocked, statusBanner = if (!s.awbLocked) "Баланс белого заблокирован" else "Блокировка баланса белого снята")
        }
        pushParams()
    }

    // ------------------------------------------------------------------------------------------
    // Quick settings (labelled option sheets) and direct setters
    // ------------------------------------------------------------------------------------------

    fun openQuickSetting(setting: QuickSetting?) {
        _uiState.update {
            it.copy(quickSetting = if (it.quickSetting == setting) null else setting, selectedDialTab = if (setting != null) null else it.selectedDialTab)
        }
    }

    fun setPhotoFormat(format: PhotoFormat) {
        _uiState.update { s ->
            if (!s.capabilities.photoFormats.contains(format)) return@update s.copy(statusBanner = "${format.label}: камера не поддерживает")
            s.copy(photoFormat = format, statusBanner = "Формат: ${format.label} · ${format.descRu}")
        }
    }

    fun setPhotoDrive(mode: PhotoDriveMode) {
        _uiState.update { it.copy(photoDriveMode = mode, statusBanner = mode.labelRu) }
    }

    fun setSelfTimer(timer: SelfTimer) {
        _uiState.update { it.copy(selfTimer = timer, statusBanner = if (timer == SelfTimer.OFF) "Таймер выключен" else "Таймер ${timer.seconds} с") }
    }

    fun setFlash(mode: FlashMode) {
        _uiState.update { s ->
            if (!s.capabilities.hasFlash) return@update s.copy(statusBanner = "У этой камеры нет вспышки")
            s.copy(flashMode = mode, statusBanner = "Вспышка: ${mode.labelRu}")
        }
        pushParams()
    }

    fun setPhotoAspect(aspect: PhotoAspect) {
        _uiState.update { it.copy(photoAspect = aspect, statusBanner = "Кадр ${aspect.label} · ${aspect.descRu}") }
        persistSettings()
    }

    fun setJpegQuality(quality: JpegQuality) {
        _uiState.update { it.copy(jpegQuality = quality, statusBanner = "Качество JPEG ${quality.label}") }
        persistSettings()
    }

    fun setCapturePriority(priority: CapturePriority) {
        _uiState.update { s ->
            if (priority == CapturePriority.ZERO_LAG && !s.capabilities.zslSupported) {
                return@update s.copy(statusBanner = "ZSL не поддерживается этой камерой")
            }
            s.copy(capturePriority = priority, statusBanner = "Приоритет: ${priority.labelRu} · ${priority.descRu}")
        }
        persistSettings()
    }

    fun selectSceneExtension(extension: SceneExtension) {
        _uiState.update { s ->
            if (extension != SceneExtension.NONE && !s.capabilities.availableExtensions.contains(extension)) {
                return@update s.copy(statusBanner = "Режим «${extension.labelRu}» недоступен на этой камере")
            }
            s.copy(
                sceneExtension = extension,
                isoValue = if (extension == SceneExtension.NONE) s.isoValue else 0,
                shutterNs = if (extension == SceneExtension.NONE) s.shutterNs else 0L,
                manualFocusDiopters = if (extension == SceneExtension.NONE) s.manualFocusDiopters else null,
                selectedDialTab = if (extension == SceneExtension.NONE) s.selectedDialTab else null,
                aeLocked = false,
                awbLocked = false,
                statusBanner = if (extension == SceneExtension.NONE) "Обычный режим: ручное управление доступно"
                else "${extension.labelRu}: ${extension.descRu}"
            )
        }
    }

    fun setVideoResolution(resolution: VideoResolution) {
        _uiState.update { s ->
            val caps = s.capabilities
            val known = caps.cameraLabel.isNotEmpty()
            if (known && !caps.videoResolutions.contains(resolution)) return@update s.copy(statusBanner = "${resolution.label}: не поддерживается")
            val fps = when {
                !known || caps.supports(resolution, s.videoFps) -> s.videoFps
                else -> caps.fpsFor(resolution).filter { !it.isHighSpeed && it.fps <= s.videoFps.fps }.lastOrNull()
                    ?: caps.fpsFor(resolution).firstOrNull()
                    ?: s.videoFps
            }
            s.copy(
                videoResolution = resolution,
                videoFps = fps,
                statusBanner = "Разрешение: ${resolution.label} (${resolution.dimensions})" +
                    if (fps != s.videoFps) " · частота ${fps.fps} к/с" else ""
            )
        }
        pushParams()
    }

    /** Picks the frame rate; if the current resolution cannot do it, moves to the best resolution that can. */
    fun setVideoFps(fps: VideoFps) {
        _uiState.update { s ->
            val caps = s.capabilities
            val known = caps.cameraLabel.isNotEmpty()
            val resolution = when {
                !known -> s.videoResolution
                caps.supports(s.videoResolution, fps) -> s.videoResolution
                else -> caps.resolutionsFor(fps).firstOrNull()
                    ?: return@update s.copy(statusBanner = "${fps.fps} к/с эта камера не поддерживает")
            }
            val ns = if (s.shutterNs > 0L && s.useShutterAngleInVideo) {
                (1_000_000_000.0 * s.shutterAngleDeg / (360.0 * fps.fps)).toLong()
            } else s.shutterNs
            s.copy(
                videoFps = fps,
                videoResolution = resolution,
                shutterNs = ns,
                statusBanner = buildString {
                    append(if (fps.isHighSpeed) "Slow-motion ${fps.fps} к/с" else "Частота кадров: ${fps.fps} к/с")
                    if (resolution != s.videoResolution) append(" · разрешение ${resolution.label}")
                    if (fps.isHighSpeed) append(if (s.slowMotion) " · замедление ×${fps.slowMotionFactor}" else " · реальное время")
                }
            )
        }
        pushParams()
    }

    fun setSlowMotion(enabled: Boolean) {
        _uiState.update {
            it.copy(
                slowMotion = enabled,
                statusBanner = if (enabled) "Slow-motion: файл проигрывается замедленно" else "Slow-motion: файл в реальном времени, ${it.videoFps.fps} к/с"
            )
        }
        persistSettings()
    }

    fun setAudioSource(source: AudioSourceOption) {
        _uiState.update { it.copy(audioSource = source, statusBanner = "Микрофон: ${source.labelRu}") }
        persistSettings()
    }

    fun setRecordingLimit(limit: RecordingLimit) {
        _uiState.update { it.copy(recordingLimit = limit, statusBanner = if (limit == RecordingLimit.NONE) "Лимит записи снят" else "Лимит записи: ${limit.label}") }
        persistSettings()
    }

    fun setDynamicRange(range: VideoDynamicRange) {
        _uiState.update { s ->
            if (range == VideoDynamicRange.HLG_10 && !s.capabilities.hdrVideo) return@update s.copy(statusBanner = "HDR-видео не поддерживается этой камерой")
            s.copy(videoDynamicRange = range, statusBanner = "${range.label} · ${range.descRu}")
        }
    }

    fun setStabilization(enabled: Boolean) {
        _uiState.update { s ->
            if (enabled && !s.capabilities.videoStabilization && !s.capabilities.previewStabilization) {
                return@update s.copy(statusBanner = "Стабилизация недоступна на этой камере")
            }
            s.copy(stabilization = enabled, statusBanner = if (enabled) "Стабилизация включена" else "Стабилизация выключена")
        }
    }

    fun setVideoBitrate(bitrate: VideoBitrate) {
        _uiState.update { it.copy(videoBitrate = bitrate, statusBanner = "Битрейт: ${bitrate.label}") }
        persistSettings()
    }

    fun setAnamorphic(value: AnamorphicDeSqueeze) {
        _uiState.update { it.copy(anamorphicDeSqueeze = value, statusBanner = "Де-сквиз превью: ${value.descRu}") }
    }

    fun setAspectGuide(value: FrameAspectGuide) {
        _uiState.update { it.copy(aspectGuide = value, statusBanner = "Рамка: ${value.descRu}") }
    }

    fun setFocusPeaking(value: FocusPeakingColor) {
        _uiState.update { it.copy(focusPeakingColor = value, statusBanner = if (value == FocusPeakingColor.OFF) "Пикинг выключен" else "Пикинг: ${value.labelRu.lowercase()}") }
    }

    fun setZebraThreshold(value: ZebraThreshold) {
        _uiState.update { it.copy(zebraThreshold = value, statusBanner = if (value == ZebraThreshold.OFF) "Зебра выключена" else "Зебра от ${value.label}") }
        pushParams()
    }

    fun setGridOverlay(value: GridOverlayMode) {
        _uiState.update { it.copy(gridOverlayMode = value, statusBanner = "Сетка: ${value.labelRu}") }
    }

    fun setIntervalSpacing(value: IntervalSpacing) {
        _uiState.update { it.copy(intervalSpacing = value, photoDriveMode = PhotoDriveMode.INTERVAL, statusBanner = "Интервал ${value.label}") }
    }

    fun setIntervalCount(value: IntervalCount) {
        _uiState.update {
            it.copy(
                intervalCount = value,
                photoDriveMode = PhotoDriveMode.INTERVAL,
                statusBanner = if (value.count > 0) "Серия из ${value.count} кадров" else "Серия до остановки"
            )
        }
    }

    // Persistent preferences (settings screen)
    fun setAntiBanding(value: AntiBanding) {
        _uiState.update { it.copy(antiBanding = value, statusBanner = "Антимерцание: ${value.labelRu}") }
        pushParams()
        persistSettings()
    }

    fun setNoiseReduction(value: ProcessingMode) {
        _uiState.update { it.copy(noiseReduction = value, statusBanner = "Шумоподавление: ${value.labelRu}") }
        pushParams()
        persistSettings()
    }

    fun setEdgeMode(value: ProcessingMode) {
        _uiState.update { it.copy(edgeMode = value, statusBanner = "Резкость: ${value.labelRu}") }
        pushParams()
        persistSettings()
    }

    fun toggleShutterSound() {
        _uiState.update { it.copy(shutterSound = !it.shutterSound) }
        persistSettings()
    }

    fun toggleVolumeKeyShutter() {
        _uiState.update { it.copy(volumeKeyShutter = !it.volumeKeyShutter) }
        persistSettings()
    }

    fun toggleMirrorSelfie() {
        _uiState.update { it.copy(mirrorSelfie = !it.mirrorSelfie) }
        persistSettings()
    }

    fun toggleHaptics() {
        _uiState.update { it.copy(hapticsEnabled = !it.hapticsEnabled) }
        persistSettings()
    }

    fun toggleKeepScreenOn() {
        _uiState.update { it.copy(keepScreenOn = !it.keepScreenOn) }
        persistSettings()
    }

    fun toggleFrontCamera() {
        controller.setZoomRatio(1f)
        _uiState.update {
            it.copy(
                useFrontCamera = !it.useFrontCamera,
                activeCameraId = null,
                zoomRatio = 1f,
                manualFocusDiopters = null,
                tapFocusNormalized = null,
                aeAfLocked = false,
                aeLocked = false,
                awbLocked = false,
                focusMagnifier = false
            )
        }
    }

    fun selectLens(option: LensOption) {
        val s = _uiState.value
        if (option.isFront != s.useFrontCamera) {
            toggleFrontCamera()
            return
        }
        val currentId = s.activeCameraId ?: s.capabilities.activeCameraId
        val equiv = option.focalEquivMm?.let { mm -> " · ≈$mm мм" } ?: ""
        if (option.cameraId != null && option.cameraId != currentId) {
            // A different physical camera: rebind on it at 1× and drop per-lens focus state.
            controller.setZoomRatio(1f)
            _uiState.update {
                it.copy(
                    activeCameraId = option.cameraId,
                    zoomRatio = 1f,
                    manualFocusDiopters = null,
                    tapFocusNormalized = null,
                    aeAfLocked = false,
                    focusMagnifier = false,
                    statusBanner = "${option.kind.labelRu} ${option.label}$equiv"
                )
            }
            return
        }
        controller.setZoomRatio(option.zoomRatio)
        _uiState.update {
            it.copy(
                zoomRatio = option.zoomRatio,
                statusBanner = (if (option.kind != LensKind.MAIN && option.kind != LensKind.FRONT) "${option.kind.labelRu} " else "") + option.label + equiv
            )
        }
    }

    fun pinchZoom(scaleFactor: Float) {
        val current = controller.currentZoom().takeIf { it > 0f } ?: _uiState.value.zoomRatio
        controller.setZoomRatio(current * scaleFactor)
    }

    fun setZoom(ratio: Float) {
        controller.setZoomRatio(ratio)
    }

    fun onViewfinderTap(normX: Float, normY: Float, previewView: PreviewView, pxX: Float, pxY: Float, longPress: Boolean) {
        val s = _uiState.value
        if (longPress && s.aeAfLocked) {
            controller.cancelFocusLock()
            _uiState.update { it.copy(aeAfLocked = false, tapFocusNormalized = null, statusBanner = "Блокировка AE/AF снята") }
            return
        }
        controller.tapToFocus(previewView, pxX, pxY, longPress)
        _uiState.update {
            it.copy(
                tapFocusNormalized = Pair(normX, normY),
                aeAfLocked = longPress,
                statusBanner = if (longPress) "AE/AF заблокированы" else "Фокус и экспозиция по точке"
            )
        }
        if (!longPress) {
            viewModelScope.launch {
                delay(4000)
                _uiState.update { st -> if (!st.aeAfLocked) st.copy(tapFocusNormalized = null) else st }
            }
        }
    }

    fun toggleAeAfLock(previewView: PreviewView?) {
        val s = _uiState.value
        if (s.aeAfLocked) {
            controller.cancelFocusLock()
            _uiState.update { it.copy(aeAfLocked = false, tapFocusNormalized = null, statusBanner = "Блокировка AE/AF снята") }
        } else if (previewView != null) {
            controller.tapToFocus(previewView, previewView.width / 2f, previewView.height / 2f, true)
            _uiState.update { it.copy(aeAfLocked = true, tapFocusNormalized = Pair(0.5f, 0.5f), statusBanner = "AE/AF заблокированы по центру") }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Manual exposure & focus
    // ------------------------------------------------------------------------------------------

    fun toggleDialTab(tab: ManualDialTab) {
        _uiState.update { it.copy(selectedDialTab = if (it.selectedDialTab == tab) null else tab, quickSetting = null) }
    }

    fun setIso(iso: Int) {
        _uiState.update { it.copy(isoValue = iso, aeLocked = if (iso > 0) false else it.aeLocked) }
        pushParams()
    }

    fun setShutterDenominator(denom: Int) = setShutterNs(if (denom > 0) 1_000_000_000L / denom else 0L)

    /** Exposure time in nanoseconds, 0 = automatic. Keeps the shutter angle in sync for video. */
    fun setShutterNs(ns: Long) {
        _uiState.update { state ->
            val angle = if (ns > 0L) {
                ((ns * 360.0 * state.videoFps.fps) / 1_000_000_000.0).toFloat().coerceIn(1f, 360f)
            } else 180f
            state.copy(shutterNs = ns, shutterAngleDeg = angle, useShutterAngleInVideo = false)
        }
        pushParams()
    }

    fun setShutterAngle(angleDeg: Float) {
        _uiState.update { state ->
            val ns = (1_000_000_000.0 * angleDeg / (360.0 * state.videoFps.fps)).toLong().coerceAtLeast(1L)
            state.copy(shutterAngleDeg = angleDeg, shutterNs = ns, useShutterAngleInVideo = true)
        }
        pushParams()
    }

    fun setShutterAuto() {
        _uiState.update { it.copy(shutterNs = 0L) }
        pushParams()
    }

    fun toggleShutterAngleMode() {
        _uiState.update { it.copy(useShutterAngleInVideo = !it.useShutterAngleInVideo) }
    }

    fun setWhiteBalance(preset: AwbPreset) {
        _uiState.update { it.copy(awbPreset = preset, awbLocked = false, statusBanner = "Баланс белого: ${preset.labelRu}") }
        pushParams()
    }

    fun setEvCompensation(ev: Float) {
        val caps = _uiState.value.capabilities
        val range = caps.evRange.takeIf { it > 0f } ?: 3f
        val snapped = if (caps.evStep > 0f) (ev / caps.evStep).roundToInt() * caps.evStep else (ev * 10f).roundToInt() / 10f
        _uiState.update { it.copy(evCompensation = snapped.coerceIn(-range, range)) }
        pushParams()
    }

    fun setManualFocus(diopters: Float?) {
        focusPullJob?.cancel()
        _uiState.update {
            it.copy(
                manualFocusDiopters = diopters?.coerceIn(0f, it.capabilities.minFocusDiopters.takeIf { m -> m > 0f } ?: 10f),
                activeFocusPullSlot = null,
                aeAfLocked = false,
                tapFocusNormalized = null
            )
        }
        pushParams()
    }

    fun saveFocusPullPoint(slot: String) {
        val current = _uiState.value.manualFocusDiopters ?: _uiState.value.liveExposure.focusDistanceDiopters
        val label = formatDiopters(current)
        _uiState.update { state ->
            state.copy(
                focusPullPoints = state.focusPullPoints.map { if (it.slot == slot) FocusPullPoint(slot, current, label) else it },
                statusBanner = "Точка $slot записана: $label"
            )
        }
    }

    fun triggerRackFocusPull(slot: String) {
        val target = _uiState.value.focusPullPoints.find { it.slot == slot } ?: return
        focusPullJob?.cancel()
        focusPullJob = viewModelScope.launch {
            val start = _uiState.value.manualFocusDiopters ?: _uiState.value.liveExposure.focusDistanceDiopters
            val end = target.distanceDiopters
            val duration = _uiState.value.focusPullDurationMs.coerceAtLeast(300L)
            val steps = 30
            _uiState.update { it.copy(activeFocusPullSlot = slot, statusBanner = "Перевод фокуса → $slot (${target.labelMeters})") }
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val smooth = t * t * (3f - 2f * t)
                _uiState.update { it.copy(manualFocusDiopters = start + (end - start) * smooth) }
                pushParams()
                delay(duration / steps)
            }
        }
    }

    fun setFocusPullSpeed(durationMs: Long) {
        _uiState.update { it.copy(focusPullDurationMs = durationMs) }
    }

    // ------------------------------------------------------------------------------------------
    // Photo & video settings
    // ------------------------------------------------------------------------------------------

    fun cyclePhotoFormat() {
        _uiState.update { s ->
            val available = PhotoFormat.entries.filter { s.capabilities.photoFormats.contains(it) }
            val next = available[(available.indexOf(s.photoFormat) + 1) % available.size]
            s.copy(photoFormat = next, statusBanner = "Формат: ${next.label} · ${next.descRu}")
        }
    }

    fun cyclePhotoDrive() {
        _uiState.update { s ->
            val list = PhotoDriveMode.entries
            val next = list[(list.indexOf(s.photoDriveMode) + 1) % list.size]
            s.copy(photoDriveMode = next, statusBanner = next.labelRu)
        }
    }

    fun cycleSelfTimer() {
        _uiState.update { s ->
            val list = SelfTimer.entries
            val next = list[(list.indexOf(s.selfTimer) + 1) % list.size]
            s.copy(selfTimer = next, statusBanner = if (next == SelfTimer.OFF) "Таймер выключен" else "Таймер ${next.seconds} с")
        }
    }

    fun cycleFlash() {
        _uiState.update { s ->
            if (!s.capabilities.hasFlash) return@update s.copy(statusBanner = "У этой камеры нет вспышки")
            val list = FlashMode.entries
            val next = list[(list.indexOf(s.flashMode) + 1) % list.size]
            s.copy(flashMode = next, statusBanner = "Вспышка: ${next.labelRu}")
        }
        pushParams()
    }

    fun cycleVideoResolution() {
        _uiState.update { s ->
            val list = s.capabilities.videoResolutions
            val next = list[(list.indexOf(s.videoResolution) + 1).mod(list.size)]
            s.copy(videoResolution = next, statusBanner = "Разрешение: ${next.label} (${next.dimensions})")
        }
    }

    fun cycleVideoFps() {
        _uiState.update { s ->
            val list = s.capabilities.fpsFor(s.videoResolution).ifEmpty { listOf(s.videoFps) }
            val next = list[(list.indexOf(s.videoFps) + 1).mod(list.size)]
            val ns = if (s.shutterNs > 0L && s.useShutterAngleInVideo) {
                (1_000_000_000.0 * s.shutterAngleDeg / (360.0 * next.fps)).toLong()
            } else s.shutterNs
            s.copy(videoFps = next, shutterNs = ns, statusBanner = "Частота кадров: ${next.fps} к/с")
        }
        pushParams()
    }

    fun cycleDynamicRange() {
        _uiState.update { s ->
            if (!s.capabilities.hdrVideo) return@update s.copy(statusBanner = "HDR-видео не поддерживается этой камерой")
            val next = if (s.videoDynamicRange == VideoDynamicRange.SDR) VideoDynamicRange.HLG_10 else VideoDynamicRange.SDR
            s.copy(videoDynamicRange = next, statusBanner = "${next.label} · ${next.descRu}")
        }
    }

    fun toggleStabilization() {
        _uiState.update { s ->
            if (!s.capabilities.videoStabilization && !s.capabilities.previewStabilization) {
                return@update s.copy(statusBanner = "Стабилизация недоступна на этой камере")
            }
            s.copy(stabilization = !s.stabilization, statusBanner = if (!s.stabilization) "Стабилизация включена" else "Стабилизация выключена")
        }
    }

    fun cycleAnamorphicDeSqueeze() {
        _uiState.update { s ->
            val list = AnamorphicDeSqueeze.entries
            val next = list[(list.indexOf(s.anamorphicDeSqueeze) + 1) % list.size]
            s.copy(anamorphicDeSqueeze = next, statusBanner = "Де-сквиз превью: ${next.descRu}")
        }
    }

    fun cycleAspectGuide() {
        _uiState.update { s ->
            val list = FrameAspectGuide.entries
            val next = list[(list.indexOf(s.aspectGuide) + 1) % list.size]
            s.copy(aspectGuide = next, statusBanner = "Рамка: ${next.descRu}")
        }
    }

    fun toggleAudioEnabled() {
        _uiState.update { it.copy(audioEnabled = !it.audioEnabled, statusBanner = if (!it.audioEnabled) "Микрофон включён" else "Видео без звука") }
        refreshAudioMeter()
    }

    // Scopes & overlays
    fun toggleHistogram() = _uiState.update { it.copy(showHistogramRgb = !it.showHistogramRgb) }
    fun toggleWaveform() = _uiState.update { it.copy(showWaveformScope = !it.showWaveformScope) }
    fun toggleVectorscope() = _uiState.update { it.copy(showVectorscope = !it.showVectorscope) }
    fun toggleFalseColor() = _uiState.update { it.copy(showFalseColor = !it.showFalseColor) }
    fun toggleHorizonLevel() = _uiState.update { it.copy(showHorizonLevel = !it.showHorizonLevel) }

    fun cycleZebraThreshold() {
        _uiState.update { s ->
            val values = ZebraThreshold.entries
            val next = values[(values.indexOf(s.zebraThreshold) + 1) % values.size]
            s.copy(zebraThreshold = next, statusBanner = if (next == ZebraThreshold.OFF) "Зебра выключена" else "Зебра от ${next.label}")
        }
        pushParams()
    }

    fun cycleFocusPeaking() {
        _uiState.update { s ->
            val values = FocusPeakingColor.entries
            val next = values[(values.indexOf(s.focusPeakingColor) + 1) % values.size]
            s.copy(focusPeakingColor = next, statusBanner = if (next == FocusPeakingColor.OFF) "Пикинг выключен" else "Пикинг: ${next.labelRu.lowercase()}")
        }
    }

    fun cycleGridOverlay() {
        _uiState.update { s ->
            val values = GridOverlayMode.entries
            val next = values[(values.indexOf(s.gridOverlayMode) + 1) % values.size]
            s.copy(gridOverlayMode = next, statusBanner = "Сетка: ${next.labelRu}")
        }
    }

    // ------------------------------------------------------------------------------------------
    // Glyph
    // ------------------------------------------------------------------------------------------

    fun selectGlyphLightingMode(mode: GlyphLightingMode) {
        _uiState.update {
            it.copy(
                glyphMode = mode,
                activeGlyphSegments = mode.segments,
                statusBanner = "Glyph: ${mode.titleRu}"
            )
        }
        pushGlyph()
    }

    fun toggleGlyphSegment(segmentIndex: Int) {
        _uiState.update { state ->
            val next = state.activeGlyphSegments.toMutableSet()
            if (!next.add(segmentIndex)) next.remove(segmentIndex)
            state.copy(
                activeGlyphSegments = next,
                glyphMode = if (state.glyphMode == GlyphLightingMode.OFF && next.isNotEmpty()) GlyphLightingMode.STUDIO_SOFT_FILL else state.glyphMode
            )
        }
        pushGlyph()
    }

    fun setGlyphBrightness(percent: Int) {
        _uiState.update { it.copy(glyphBrightness = percent.coerceIn(0, 100)) }
        pushGlyph()
    }

    fun toggleGlyphTorchHardware() {
        _uiState.update { it.copy(glyphTorchHardwareSync = !it.glyphTorchHardwareSync) }
        pushParams()
    }

    fun reconnectGlyph() = glyph.connect()

    // ------------------------------------------------------------------------------------------
    // LUT & grading
    // ------------------------------------------------------------------------------------------

    fun selectLutProfile(lut: LutProfile) {
        _uiState.update { it.copy(activeLut = lut, statusBanner = "LUT: ${lut.nameRu}") }
    }

    fun updateColorGrading(
        contrast: Float = _uiState.value.userContrast,
        saturation: Float = _uiState.value.userSaturation,
        shadowLift: Float = _uiState.value.shadowLift,
        highlightRollOff: Float = _uiState.value.highlightRollOff
    ) {
        _uiState.update {
            it.copy(
                userContrast = contrast.coerceIn(0.5f, 1.5f),
                userSaturation = saturation.coerceIn(0.0f, 1.8f),
                shadowLift = shadowLift.coerceIn(-0.4f, 0.4f),
                highlightRollOff = highlightRollOff.coerceIn(0.0f, 1.0f)
            )
        }
    }

    fun toggleBakeLut() {
        _uiState.update { it.copy(bakeLutIntoPhotos = !it.bakeLutIntoPhotos) }
        persistSettings()
    }

    fun resetColorGrade() {
        _uiState.update { it.copy(userContrast = 1.0f, userSaturation = 1.0f, shadowLift = 0.0f, highlightRollOff = 0.15f) }
    }

    // ------------------------------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------------------------------

    fun saveCurrentAsPreset(customName: String) {
        val s = _uiState.value
        val nextSlotNumber = (presets.value.size + 1).coerceAtMost(99)
        val preset = CameraPresetEntity(
            slotCode = "C$nextSlotNumber",
            name = customName.ifBlank { "${s.zoomLabel} ${s.activeLut.code} ${if (s.isVideoMode) "VIDEO" else "PHOTO"}" }.uppercase(),
            isVideoPreset = s.cameraMode == AppMode.CINE_VIDEO,
            zoomRatio = s.zoomRatio,
            useFrontCamera = s.useFrontCamera,
            isoValue = s.isoValue,
            shutterNs = s.shutterNs,
            shutterAngleDeg = s.shutterAngleDeg,
            awbPreset = s.awbPreset.name,
            evCompensation = s.evCompensation,
            lutId = s.activeLut.id,
            anamorphicLabel = s.anamorphicDeSqueeze.label,
            aspectGuideLabel = s.aspectGuide.label,
            glyphModeName = s.glyphMode.name,
            glyphBrightness = s.glyphBrightness,
            videoResolution = s.videoResolution.name,
            videoFps = s.videoFps.fps,
            hdrVideo = s.videoDynamicRange == VideoDynamicRange.HLG_10
        )
        viewModelScope.launch {
            repository.savePreset(preset)
            _uiState.update { it.copy(statusBanner = "Пресет ${preset.slotCode} сохранён") }
        }
    }

    fun applyPreset(preset: CameraPresetEntity) {
        val lut = LutProfile.entries.find { it.id == preset.lutId } ?: LutProfile.NOTHING_STD
        val anamorphic = AnamorphicDeSqueeze.entries.find { it.label == preset.anamorphicLabel } ?: AnamorphicDeSqueeze.NONE
        val guide = FrameAspectGuide.entries.find { it.label == preset.aspectGuideLabel } ?: FrameAspectGuide.OFF
        val glyphMode = GlyphLightingMode.entries.find { it.name == preset.glyphModeName } ?: GlyphLightingMode.OFF
        val awb = AwbPreset.entries.find { it.name == preset.awbPreset } ?: AwbPreset.AUTO
        val resolution = VideoResolution.entries.find { it.name == preset.videoResolution }
        val fps = VideoFps.entries.find { it.fps == preset.videoFps }

        _uiState.update { s ->
            val caps = s.capabilities
            val targetResolution = resolution?.takeIf { caps.videoResolutions.contains(it) } ?: s.videoResolution
            s.copy(
                activeMode = if (preset.isVideoPreset) AppMode.CINE_VIDEO else AppMode.PHOTO_PRO,
                lastCameraMode = if (preset.isVideoPreset) AppMode.CINE_VIDEO else AppMode.PHOTO_PRO,
                useFrontCamera = preset.useFrontCamera,
                isoValue = preset.isoValue,
                shutterNs = preset.shutterNs,
                shutterAngleDeg = preset.shutterAngleDeg,
                awbPreset = awb,
                evCompensation = preset.evCompensation,
                activeLut = lut,
                anamorphicDeSqueeze = anamorphic,
                aspectGuide = guide,
                glyphMode = glyphMode,
                activeGlyphSegments = glyphMode.segments,
                glyphBrightness = preset.glyphBrightness,
                videoResolution = targetResolution,
                videoFps = fps?.takeIf { caps.cameraLabel.isEmpty() || caps.supports(targetResolution, it) } ?: s.videoFps,
                videoDynamicRange = if (preset.hdrVideo && caps.hdrVideo) VideoDynamicRange.HLG_10 else VideoDynamicRange.SDR,
                statusBanner = "Пресет ${preset.slotCode}: ${preset.name}"
            )
        }
        controller.setZoomRatio(preset.zoomRatio)
        pushParams()
        pushGlyph()
        refreshAudioMeter()
    }

    fun deletePreset(id: Int) {
        viewModelScope.launch { repository.deletePreset(id) }
    }

    // ------------------------------------------------------------------------------------------
    // Archive
    // ------------------------------------------------------------------------------------------

    fun inspectCapturedShot(shot: CapturedShotEntity?) {
        _uiState.update { it.copy(inspectShot = shot) }
    }

    fun deleteCapturedShot(shot: CapturedShotEntity) {
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.delete(Uri.parse(shot.uri), null, null) >= 0
                } catch (e: SecurityException) {
                    false
                } catch (e: Exception) {
                    true
                }
            }
            if (removed) {
                repository.deleteCapturedShot(shot.id)
                if (_uiState.value.inspectShot?.id == shot.id) _uiState.update { it.copy(inspectShot = null) }
            } else {
                _uiState.update { it.copy(statusBanner = "Файл можно удалить только через системную галерею") }
            }
        }
    }

    private suspend fun purgeMissingShots() {
        val resolver = getApplication<Application>().contentResolver
        val shots = repository.allShotsOnce()
        withContext(Dispatchers.IO) {
            shots.forEach { shot ->
                val exists = try {
                    resolver.query(Uri.parse(shot.uri), arrayOf("_id"), null, null, null)?.use { it.count > 0 } ?: false
                } catch (e: Exception) {
                    false
                }
                if (!exists) repository.deleteCapturedShot(shot.id)
            }
        }
    }

    override fun onCleared() {
        glyph.release()
        controller.release()
        super.onCleared()
    }
}
