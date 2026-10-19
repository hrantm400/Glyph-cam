package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CameraPresetEntity
import com.example.data.CameraRepository
import com.example.data.CapturedShotEntity
import com.example.data.GlyphCamDatabase
import com.example.model.AnamorphicDeSqueeze
import com.example.model.AppMode
import com.example.model.AudioDirectionMode
import com.example.model.FocusPeakingColor
import com.example.model.FocusPullPoint
import com.example.model.FrameAspectGuide
import com.example.model.GlyphLightingMode
import com.example.model.GridOverlayMode
import com.example.model.LiveScopeTelemetry
import com.example.model.LutProfile
import com.example.model.MeteringMode
import com.example.model.NothingLens
import com.example.model.PhotoDriveMode
import com.example.model.PhotoFormat
import com.example.model.StabilizationMode
import com.example.model.VideoCodecProfile
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.model.ZebraThreshold
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

enum class ManualDialTab(val labelRu: String, val shortCode: String) {
    ISO("ЧУВСТВИТ. ISO", "ISO"),
    SHUTTER("ВЫДЕРЖКА / УГОЛ", "SHTR"),
    FOCUS("ФОКУС + ПУЛЛЕР", "MF"),
    WB("БАЛАНС БЕЛОГО", "WB"),
    EV("ЭКСПОКОРРЕКЦИЯ", "EV"),
    AUDIO("МИКРОФОН 24-BIT", "AUD")
}

data class ProCameraUiState(
    val activeMode: AppMode = AppMode.CINE_VIDEO,
    val selectedLens: NothingLens = NothingLens.MAIN_WIDE,
    val fineZoomRatio: Float = 1.0f,
    val selectedDialTab: ManualDialTab? = ManualDialTab.SHUTTER,

    // Manual Exposure & Focus
    val isoValue: Int = 0, // 0 = AUTO
    val shutterDenominator: Int = 48, // 0 = AUTO, 48 = 1/48s
    val shutterAngleDeg: Float = 180.0f,
    val useShutterAngleInVideo: Boolean = true,
    val wbKelvin: Int = 5600, // 0 = AWB
    val wbTint: Int = 0, // -50..+50
    val evCompensation: Float = 0.0f, // -3.0..+3.0
    val manualFocusDiopters: Float? = null, // null = AF-C, 0f = inf, 10f = macro
    val focusPullPoints: List<FocusPullPoint> = listOf(
        FocusPullPoint("A", 8.0f, "0.12m MACRO"),
        FocusPullPoint("B", 2.2f, "0.45m SUBJ"),
        FocusPullPoint("C", 0.1f, "∞ HORIZON")
    ),
    val activeFocusPullSlot: String? = null,
    val focusPullDurationMs: Long = 1400L,
    val aeAfLocked: Boolean = false,
    val tapFocusNormalized: Pair<Float, Float>? = null,
    val meteringMode: MeteringMode = MeteringMode.MATRIX,
    val stabilizationMode: StabilizationMode = StabilizationMode.HYBRID_OIS_EIS,

    // Photo Pro Configuration
    val photoFormat: PhotoFormat = PhotoFormat.RAW_PLUS_JPEG,
    val photoDriveMode: PhotoDriveMode = PhotoDriveMode.SINGLE,
    val ndFilterStops: Int = 0, // 0, 3 (ND8), 6 (ND64), 10 (ND1000)

    // Cinema Video Configuration
    val videoResolution: VideoResolution = VideoResolution.DCI_4K,
    val videoFps: VideoFps = VideoFps.FPS_24,
    val videoCodec: VideoCodecProfile = VideoCodecProfile.HEVC_ALL_I,
    val anamorphicDeSqueeze: AnamorphicDeSqueeze = AnamorphicDeSqueeze.NONE,
    val aspectGuide: FrameAspectGuide = FrameAspectGuide.CINE_239,
    val aspectMaskOpacity: Float = 0.65f,

    // Audio Studio
    val audioGainDb: Float = 0.0f,
    val windCutEnabled: Boolean = true,
    val audioDirection: AudioDirectionMode = AudioDirectionMode.STEREO_WIDE,
    val leftAudioDb: Float = -28f,
    val rightAudioDb: Float = -27f,
    val peakAudioDb: Float = -18f,

    // Scopes & Overlays
    val showHistogramRgb: Boolean = true,
    val showWaveformScope: Boolean = true,
    val showVectorscope: Boolean = false,
    val showFalseColor: Boolean = false,
    val zebraThreshold: ZebraThreshold = ZebraThreshold.IRE_95,
    val focusPeakingColor: FocusPeakingColor = FocusPeakingColor.NOTHING_RED,
    val gridOverlayMode: GridOverlayMode = GridOverlayMode.RULE_OF_THIRDS,
    val showHorizonLevel: Boolean = true,
    val showCenterCrosshair: Boolean = true,

    // Nothing Phone (2) Glyph Interface Studio
    val glyphMode: GlyphLightingMode = GlyphLightingMode.PORTRAIT_CATCHLIGHT,
    val activeGlyphSegments: Set<Int> = setOf(0, 1, 2, 5, 6), // 0..10 (11 physical strips on Phone 2)
    val glyphBrightness: Int = 75,
    val glyphTorchHardwareSync: Boolean = false,
    val glyphAudioReactive: Boolean = true,

    // 3D LUT & Color Grading
    val activeLut: LutProfile = LutProfile.KODAK_2383_TEAL,
    val userContrast: Float = 1.0f,
    val userSaturation: Float = 1.0f,
    val shadowLift: Float = 0.0f,
    val highlightRollOff: Float = 0.15f,
    val filmGrainAmount: Float = 0.18f,
    val splitScreenLutCompare: Boolean = false,

    // Live Telemetry & Recording State
    val scopeTelemetry: LiveScopeTelemetry = LiveScopeTelemetry(),
    val rollDegrees: Float = 0.0f,
    val pitchDegrees: Float = 0.0f,
    val isRecording: Boolean = false,
    val recordingElapsedSec: Int = 0,
    val shutterFlashTrigger: Long = 0L,
    val statusBanner: String = "SONY IMX890 // ГОТОВ К СЪЁМКЕ",
    val inspectShot: CapturedShotEntity? = null
) {
    val formattedTimecode: String
        get() {
            val hrs = recordingElapsedSec / 3600
            val mins = (recordingElapsedSec % 3600) / 60
            val secs = recordingElapsedSec % 60
            val frames = ((System.currentTimeMillis() / 42) % videoFps.fps).toInt()
            return String.format(Locale.US, "%02d:%02d:%02d:%02d", hrs, mins, secs, frames)
        }

    val shutterDisplayLabel: String
        get() {
            if (activeMode == AppMode.CINE_VIDEO && useShutterAngleInVideo) {
                return String.format(Locale.US, "%.1f°", shutterAngleDeg)
            }
            return if (shutterDenominator <= 0) "AUTO"
            else if (shutterDenominator == 1) "1.0\""
            else "1/$shutterDenominator"
        }

    val isoDisplayLabel: String
        get() = if (isoValue <= 0) "AUTO" else "ISO $isoValue"

    val wbDisplayLabel: String
        get() = if (wbKelvin <= 0) "AWB" else "${wbKelvin}K"

    val focusDisplayLabel: String
        get() {
            val d = manualFocusDiopters ?: return "AF-C"
            if (d <= 0.15f) return "∞ INF"
            val meters = (1.0f / d).coerceIn(0.04f, 10.0f)
            return String.format(Locale.US, "%.2fm", meters)
        }
}

class ProCameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CameraRepository
    private val _uiState = MutableStateFlow(ProCameraUiState())
    val uiState: StateFlow<ProCameraUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<CameraPresetEntity>>
    val capturedShots: StateFlow<List<CapturedShotEntity>>

    private var focusPullJob: Job? = null

    init {
        val db = GlyphCamDatabase.getInstance(application)
        repository = CameraRepository(db.cameraDao())
        presets = repository.presets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        capturedShots = repository.capturedShots.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        viewModelScope.launch {
            repository.seedFactoryPresetsIfNeeded()
        }
    }

    fun setAppMode(mode: AppMode) {
        _uiState.update { it.copy(activeMode = mode) }
    }

    fun selectLens(lens: NothingLens) {
        _uiState.update {
            it.copy(
                selectedLens = lens,
                fineZoomRatio = lens.zoomRatio,
                statusBanner = "${lens.focalLabel} // ${lens.sensorName}"
            )
        }
    }

    fun setFineZoom(zoom: Float) {
        _uiState.update { it.copy(fineZoomRatio = zoom.coerceIn(0.6f, 8.0f)) }
    }

    fun toggleDialTab(tab: ManualDialTab) {
        _uiState.update {
            it.copy(selectedDialTab = if (it.selectedDialTab == tab) null else tab)
        }
    }

    fun setIso(iso: Int) {
        _uiState.update { it.copy(isoValue = iso) }
    }

    fun setShutterDenominator(denom: Int) {
        _uiState.update { state ->
            val angle = if (denom > 0) {
                ((state.videoFps.fps * 360f) / denom).coerceIn(11.2f, 360f)
            } else 180f
            state.copy(shutterDenominator = denom, shutterAngleDeg = angle)
        }
    }

    fun setShutterAngle(angleDeg: Float) {
        _uiState.update { state ->
            val denom = ((state.videoFps.fps * 360f) / angleDeg).roundToInt().coerceAtLeast(1)
            state.copy(
                shutterAngleDeg = angleDeg,
                shutterDenominator = denom,
                useShutterAngleInVideo = true
            )
        }
    }

    fun toggleShutterAngleMode() {
        _uiState.update { it.copy(useShutterAngleInVideo = !it.useShutterAngleInVideo) }
    }

    fun setWhiteBalance(kelvin: Int, tint: Int = _uiState.value.wbTint) {
        _uiState.update { it.copy(wbKelvin = kelvin, wbTint = tint.coerceIn(-50, 50)) }
    }

    fun setEvCompensation(ev: Float) {
        _uiState.update { it.copy(evCompensation = ((ev * 10f).roundToInt() / 10f).coerceIn(-3.0f, 3.0f)) }
    }

    fun setManualFocus(diopters: Float?) {
        focusPullJob?.cancel()
        _uiState.update {
            it.copy(
                manualFocusDiopters = diopters,
                activeFocusPullSlot = null
            )
        }
    }

    fun saveFocusPullPoint(slot: String) {
        val currentDiopters = _uiState.value.manualFocusDiopters ?: 2.0f
        val metersLabel = if (currentDiopters <= 0.15f) "∞ INF" else String.format(Locale.US, "%.2fm", 1f / currentDiopters)
        _uiState.update { state ->
            val updated = state.focusPullPoints.map { pt ->
                if (pt.slot == slot) FocusPullPoint(slot, currentDiopters, metersLabel) else pt
            }
            state.copy(
                focusPullPoints = updated,
                statusBanner = "ФОКУС-ТОЧКА [$slot] СОХРАНЕНА НА $metersLabel"
            )
        }
    }

    fun triggerRackFocusPull(slot: String) {
        val targetPt = _uiState.value.focusPullPoints.find { it.slot == slot } ?: return
        focusPullJob?.cancel()
        focusPullJob = viewModelScope.launch {
            val startD = _uiState.value.manualFocusDiopters ?: 1.5f
            val endD = targetPt.distanceDiopters
            val duration = _uiState.value.focusPullDurationMs.coerceAtLeast(300L)
            val steps = 30
            val stepDelay = duration / steps
            _uiState.update {
                it.copy(
                    activeFocusPullSlot = slot,
                    statusBanner = "RACK FOCUS // ПЕРЕВОД ФОКУСА -> [$slot] (${targetPt.labelMeters})"
                )
            }
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                // Smooth ease-in-out cubic interpolation for cinema lens pull
                val smoothT = t * t * (3f - 2f * t)
                val interp = startD + (endD - startD) * smoothT
                _uiState.update { it.copy(manualFocusDiopters = interp) }
                delay(stepDelay)
            }
        }
    }

    fun setFocusPullSpeed(durationMs: Long) {
        _uiState.update { it.copy(focusPullDurationMs = durationMs) }
    }

    fun setTapFocusPoint(normX: Float, normY: Float, toggleLock: Boolean = false) {
        _uiState.update {
            val locked = if (toggleLock) !it.aeAfLocked else it.aeAfLocked
            it.copy(
                tapFocusNormalized = Pair(normX, normY),
                aeAfLocked = locked,
                statusBanner = if (locked) "AE/AF LOCK // ЭКСПОЗИЦИЯ И ФОКУС ЗАБЛОКИРОВАНЫ" else "ТОЧЕЧНЫЙ ЗАМЕР И ФОКУС AF"
            )
        }
    }

    fun toggleAeAfLock() {
        _uiState.update {
            val next = !it.aeAfLocked
            it.copy(
                aeAfLocked = next,
                statusBanner = if (next) "AE/AF LOCK АКТИВЕН" else "AE/AF LOCK СНЯТ"
            )
        }
    }

    fun setMeteringMode(mode: MeteringMode) {
        _uiState.update { it.copy(meteringMode = mode, statusBanner = "ЗАМЕР ЭКСПОЗИЦИИ // ${mode.labelRu}") }
    }

    fun setStabilizationMode(mode: StabilizationMode) {
        _uiState.update { it.copy(stabilizationMode = mode, statusBanner = "СТАБИЛИЗАЦИЯ // ${mode.labelRu}") }
    }

    fun setPhotoFormat(format: PhotoFormat) {
        _uiState.update { it.copy(photoFormat = format, statusBanner = "ФОРМАТ ФОТО // ${format.label} (${format.bitDepth})") }
    }

    fun setPhotoDriveMode(drive: PhotoDriveMode) {
        _uiState.update { it.copy(photoDriveMode = drive, statusBanner = "РЕЖИМ ПРИВОДА // ${drive.labelRu}") }
    }

    fun cycleNdFilter() {
        val stopsOrder = listOf(0, 3, 6, 10)
        _uiState.update {
            val nextIdx = (stopsOrder.indexOf(it.ndFilterStops) + 1) % stopsOrder.size
            val nextStops = stopsOrder[nextIdx]
            val label = when (nextStops) {
                3 -> "ND8 (-3 EV)"
                6 -> "ND64 (-6 EV)"
                10 -> "ND1000 (-10 EV)"
                else -> "БЕЗ ND"
            }
            it.copy(ndFilterStops = nextStops, statusBanner = "ОПТИЧЕСКИЙ СИМУЛЯТОР ND // $label")
        }
    }

    fun setVideoResolution(res: VideoResolution) {
        _uiState.update { it.copy(videoResolution = res, statusBanner = "РАЗРЕШЕНИЕ КИНО // ${res.label} (${res.dimensions})") }
    }

    fun setVideoFps(fps: VideoFps) {
        _uiState.update { state ->
            val newDenom = if (state.useShutterAngleInVideo) {
                ((fps.fps * 360f) / state.shutterAngleDeg).roundToInt().coerceAtLeast(1)
            } else state.shutterDenominator
            state.copy(
                videoFps = fps,
                shutterDenominator = newDenom,
                statusBanner = "ЧАСТОТА КАДРОВ // ${fps.label}"
            )
        }
    }

    fun setVideoCodec(codec: VideoCodecProfile) {
        _uiState.update { it.copy(videoCodec = codec, statusBanner = "КОДЕК // ${codec.label} @ ${codec.bitrateMbps} Мбит/с") }
    }

    fun setAnamorphicDeSqueeze(mode: AnamorphicDeSqueeze) {
        _uiState.update { it.copy(anamorphicDeSqueeze = mode, statusBanner = "ДЕ-СКВИЗ // ${mode.label} (${mode.descRu})") }
    }

    fun setAspectGuide(guide: FrameAspectGuide) {
        _uiState.update { it.copy(aspectGuide = guide, statusBanner = "КАШЕТИРОВАНИЕ // ${guide.label}") }
    }

    fun setAudioGain(gainDb: Float) {
        _uiState.update { it.copy(audioGainDb = gainDb.coerceIn(-12f, 12f)) }
    }

    fun toggleWindCut() {
        _uiState.update { it.copy(windCutEnabled = !it.windCutEnabled) }
    }

    fun setAudioDirection(dir: AudioDirectionMode) {
        _uiState.update { it.copy(audioDirection = dir, statusBanner = "МИКРОФОННЫЙ МАССИВ // ${dir.labelRu}") }
    }

    // Scopes & Overlays
    fun toggleHistogram() = _uiState.update { it.copy(showHistogramRgb = !it.showHistogramRgb) }
    fun toggleWaveform() = _uiState.update { it.copy(showWaveformScope = !it.showWaveformScope) }
    fun toggleVectorscope() = _uiState.update { it.copy(showVectorscope = !it.showVectorscope) }
    fun toggleFalseColor() = _uiState.update { it.copy(showFalseColor = !it.showFalseColor) }
    fun toggleHorizonLevel() = _uiState.update { it.copy(showHorizonLevel = !it.showHorizonLevel) }

    fun cycleZebraThreshold() {
        val values = ZebraThreshold.entries
        _uiState.update {
            val next = values[(values.indexOf(it.zebraThreshold) + 1) % values.size]
            it.copy(zebraThreshold = next, statusBanner = "ЗЕБРА ПЕРЕСВЕТОВ // ${next.label}")
        }
    }

    fun cycleFocusPeaking() {
        val values = FocusPeakingColor.entries
        _uiState.update {
            val next = values[(values.indexOf(it.focusPeakingColor) + 1) % values.size]
            it.copy(focusPeakingColor = next, statusBanner = "ФОКУС-ПИКИНГ // ${next.labelRu}")
        }
    }

    fun cycleGridOverlay() {
        val values = GridOverlayMode.entries
        _uiState.update {
            val next = values[(values.indexOf(it.gridOverlayMode) + 1) % values.size]
            it.copy(gridOverlayMode = next, statusBanner = "КОМПОЗИЦИОННАЯ СЕТКА // ${next.labelRu}")
        }
    }

    // Nothing Phone (2) Glyph Studio
    fun selectGlyphLightingMode(mode: GlyphLightingMode) {
        val defaultSegments = when (mode) {
            GlyphLightingMode.OFF -> emptySet()
            GlyphLightingMode.STUDIO_SOFT_FILL -> (0..10).toSet()
            GlyphLightingMode.CAMERA_RING_MACRO -> setOf(0, 1)
            GlyphLightingMode.PORTRAIT_CATCHLIGHT -> setOf(0, 1, 2, 4, 5, 6, 7, 8)
            GlyphLightingMode.TALLY_AND_AUDIO_VU -> setOf(2, 9, 10)
        }
        _uiState.update {
            it.copy(
                glyphMode = mode,
                activeGlyphSegments = defaultSegments,
                glyphTorchHardwareSync = mode == GlyphLightingMode.STUDIO_SOFT_FILL || mode == GlyphLightingMode.CAMERA_RING_MACRO,
                statusBanner = "GLYPH MATRIX // ${mode.titleRu}"
            )
        }
    }

    fun toggleGlyphSegment(segmentIndex: Int) {
        _uiState.update { state ->
            val next = state.activeGlyphSegments.toMutableSet()
            if (next.contains(segmentIndex)) next.remove(segmentIndex) else next.add(segmentIndex)
            state.copy(activeGlyphSegments = next)
        }
    }

    fun setGlyphBrightness(percent: Int) {
        _uiState.update { it.copy(glyphBrightness = percent.coerceIn(0, 100)) }
    }

    fun toggleGlyphTorchHardware() {
        _uiState.update { it.copy(glyphTorchHardwareSync = !it.glyphTorchHardwareSync) }
    }

    // LUT & Color Grading
    fun selectLutProfile(lut: LutProfile) {
        _uiState.update {
            it.copy(
                activeLut = lut,
                statusBanner = "3D LUT АКТИВЕН // ${lut.nameRu}"
            )
        }
    }

    fun updateColorGrading(
        contrast: Float = _uiState.value.userContrast,
        saturation: Float = _uiState.value.userSaturation,
        shadowLift: Float = _uiState.value.shadowLift,
        highlightRollOff: Float = _uiState.value.highlightRollOff,
        grain: Float = _uiState.value.filmGrainAmount
    ) {
        _uiState.update {
            it.copy(
                userContrast = contrast.coerceIn(0.5f, 1.5f),
                userSaturation = saturation.coerceIn(0.0f, 1.8f),
                shadowLift = shadowLift.coerceIn(-0.4f, 0.4f),
                highlightRollOff = highlightRollOff.coerceIn(0.0f, 1.0f),
                filmGrainAmount = grain.coerceIn(0.0f, 1.0f)
            )
        }
    }

    fun toggleSplitScreenLut() {
        _uiState.update { it.copy(splitScreenLutCompare = !it.splitScreenLutCompare) }
    }

    fun resetColorGrade() {
        _uiState.update {
            it.copy(
                userContrast = 1.0f,
                userSaturation = 1.0f,
                shadowLift = 0.0f,
                highlightRollOff = 0.15f,
                filmGrainAmount = 0.10f
            )
        }
    }

    // Telemetry callbacks from ProCameraController
    fun updateScopeTelemetry(telemetry: LiveScopeTelemetry) {
        _uiState.update { it.copy(scopeTelemetry = telemetry) }
    }

    fun updateHorizon(rollDeg: Float, pitchDeg: Float) {
        _uiState.update { it.copy(rollDegrees = rollDeg, pitchDegrees = pitchDeg) }
    }

    fun updateAudioLevels(leftDb: Float, rightDb: Float, peakDb: Float) {
        _uiState.update {
            it.copy(
                leftAudioDb = leftDb,
                rightAudioDb = rightDb,
                peakAudioDb = peakDb
            )
        }
    }

    fun updateRecordingState(isRec: Boolean, elapsedSec: Int) {
        _uiState.update {
            it.copy(
                isRecording = isRec,
                recordingElapsedSec = elapsedSec
            )
        }
    }

    fun triggerShutterFlash() {
        _uiState.update { it.copy(shutterFlashTrigger = System.currentTimeMillis()) }
    }

    fun updateStatusBanner(msg: String) {
        _uiState.update { it.copy(statusBanner = msg) }
    }

    fun recordCapturedShot(file: File, isVideo: Boolean, durationSec: Int) {
        val state = _uiState.value
        val hist16 = (0 until 16).joinToString(",") { idx ->
            val bin = state.scopeTelemetry.lumaHistogram[(idx * 4).coerceAtMost(63)]
            bin.toString()
        }
        val shot = CapturedShotEntity(
            filePath = file.absolutePath,
            isVideo = isVideo,
            durationSec = durationSec,
            titleCode = file.name,
            lensInfo = "${state.selectedLens.focalLabel} ${state.selectedLens.sensorName}",
            resolutionLabel = if (isVideo) "${state.videoResolution.label} (${state.videoResolution.dimensions})" else state.photoFormat.label,
            formatBadge = if (isVideo) state.videoCodec.label else "${state.photoFormat.badge} ${state.photoFormat.bitDepth}",
            isoUsed = if (state.isoValue == 0) 160 else state.isoValue,
            shutterLabel = state.shutterDisplayLabel,
            wbKelvin = if (state.wbKelvin == 0) 5500 else state.wbKelvin,
            apertureLabel = state.selectedLens.aperture,
            evValue = state.evCompensation,
            lutApplied = state.activeLut.code,
            glyphModeUsed = state.glyphMode.code,
            averageIre = state.scopeTelemetry.averageIre,
            dynamicRangeStops = state.scopeTelemetry.dynamicRangeStops,
            bitrateMbps = if (isVideo) state.videoCodec.bitrateMbps else 0,
            audioPeakDb = state.peakAudioDb,
            histogramSummary = hist16
        )
        viewModelScope.launch {
            repository.logCapturedShot(shot)
            _uiState.update {
                it.copy(
                    statusBanner = if (isVideo) "ВИДЕО СОХРАНЕНО // ${file.name}" else "КАДР СОХРАНЁН // ${file.name} (${state.photoFormat.badge})"
                )
            }
        }
    }

    fun saveCurrentAsPreset(customName: String) {
        val s = _uiState.value
        val nextSlotNumber = (presets.value.size + 1).coerceAtMost(99)
        val preset = CameraPresetEntity(
            slotCode = "C$nextSlotNumber",
            name = customName.ifBlank { "${s.selectedLens.focalLabel} ${s.activeLut.code} RIG" }.uppercase(),
            isVideoPreset = s.activeMode == AppMode.CINE_VIDEO,
            lensId = s.selectedLens.id,
            isoValue = s.isoValue,
            shutterDenominator = s.shutterDenominator,
            shutterAngleDeg = s.shutterAngleDeg,
            wbKelvin = s.wbKelvin,
            evCompensation = s.evCompensation,
            lutId = s.activeLut.id,
            anamorphicLabel = s.anamorphicDeSqueeze.label,
            aspectGuideLabel = s.aspectGuide.label,
            glyphModeName = s.glyphMode.name,
            glyphBrightness = s.glyphBrightness
        )
        viewModelScope.launch {
            repository.savePreset(preset)
            _uiState.update { it.copy(statusBanner = "ПРЕСЕТ [${preset.slotCode}] СОХРАНЁН В ПАМЯТЬ") }
        }
    }

    fun applyPreset(preset: CameraPresetEntity) {
        val lens = NothingLens.entries.find { it.id == preset.lensId } ?: NothingLens.MAIN_WIDE
        val lut = LutProfile.entries.find { it.id == preset.lutId } ?: LutProfile.NOTHING_STD
        val anamorphic = AnamorphicDeSqueeze.entries.find { it.label == preset.anamorphicLabel } ?: AnamorphicDeSqueeze.NONE
        val guide = FrameAspectGuide.entries.find { it.label == preset.aspectGuideLabel } ?: FrameAspectGuide.OFF
        val glyph = GlyphLightingMode.entries.find { it.name == preset.glyphModeName } ?: GlyphLightingMode.OFF

        _uiState.update {
            it.copy(
                activeMode = if (preset.isVideoPreset) AppMode.CINE_VIDEO else AppMode.PHOTO_PRO,
                selectedLens = lens,
                fineZoomRatio = lens.zoomRatio,
                isoValue = preset.isoValue,
                shutterDenominator = preset.shutterDenominator,
                shutterAngleDeg = preset.shutterAngleDeg,
                wbKelvin = preset.wbKelvin,
                evCompensation = preset.evCompensation,
                activeLut = lut,
                anamorphicDeSqueeze = anamorphic,
                aspectGuide = guide,
                glyphMode = glyph,
                glyphBrightness = preset.glyphBrightness,
                statusBanner = "ЗАГРУЖЕН ПРЕСЕТ [${preset.slotCode}] // ${preset.name}"
            )
        }
    }

    fun deletePreset(id: Int) {
        viewModelScope.launch {
            repository.deletePreset(id)
        }
    }

    fun inspectCapturedShot(shot: CapturedShotEntity?) {
        _uiState.update { it.copy(inspectShot = shot) }
    }

    fun deleteCapturedShot(id: Int) {
        viewModelScope.launch {
            repository.deleteCapturedShot(id)
            if (_uiState.value.inspectShot?.id == id) {
                _uiState.update { it.copy(inspectShot = null) }
            }
        }
    }
}
