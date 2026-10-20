package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix

enum class AppMode(val titleRu: String, val badgeCode: String) {
    PHOTO_PRO("Фото", "ФОТО"),
    CINE_VIDEO("Видео", "ВИДЕО"),
    GLYPH_STUDIO("Glyph-студия", "GLYPH"),
    LUT_LAB("LUT и цвет", "LUT"),
    MEDIA_VAULT("Архив", "АРХИВ"),
    SETTINGS("Настройки", "НАСТР")
}

/**
 * One entry of the lens row. The list is built from the real camera: the zoom range of the
 * logical camera and the focal lengths of its physical cameras, so every pill switches to a
 * zoom ratio the device can actually deliver.
 */
enum class LensKind(val labelRu: String, val code: String) {
    ULTRA_WIDE("Сверхширокий", "UW"),
    MAIN("Основной", ""),
    TELE("Телеобъектив", "TELE"),
    MACRO("Макро", "MACRO"),
    FRONT("Фронтальный", "")
}

/** A lens pill: a zoom step on the active camera, or a switch to another physical camera of the phone. */
data class LensOption(
    val id: String,
    val label: String,
    val zoomRatio: Float,
    val focalEquivMm: Int? = null,
    val isFront: Boolean = false,
    val cameraId: String? = null, // null = zoom step on whichever camera is active
    val kind: LensKind = LensKind.MAIN
)

enum class PhotoFormat(val label: String, val badge: String, val descRu: String) {
    JPEG("JPEG", "JPG", "Стандартный JPEG"),
    ULTRA_HDR("Ultra HDR", "UHDR", "JPEG с картой яркости HDR"),
    RAW_DNG("RAW DNG", "DNG", "Несжатый RAW без обработки"),
    RAW_PLUS_JPEG("RAW + JPEG", "R+J", "DNG и JPEG одним нажатием")
}

enum class PhotoDriveMode(val labelRu: String, val code: String, val shots: Int) {
    SINGLE("Одиночный кадр", "1", 1),
    BURST_5("Серия из 5 кадров", "×5", 5),
    AEB_3("Брекетинг ±2 EV", "AEB", 3),
    FOCUS_STACK_5("Стекинг фокуса ×5", "STACK", 5),
    INTERVAL("Интервальная съёмка", "INT", 0)
}

/** Output aspect of photos. 16:9 is a native sensor stream; 1:1 is a WYSIWYG crop. */
enum class PhotoAspect(val label: String, val ratio: Float, val descRu: String) {
    FULL_4_3("4:3", 4f / 3f, "Полный кадр сенсора"),
    WIDE_16_9("16:9", 16f / 9f, "Широкий кадр"),
    SQUARE_1_1("1:1", 1f, "Квадрат")
}

enum class CapturePriority(val labelRu: String, val code: String, val descRu: String) {
    QUALITY("Качество", "HQ", "Максимальная детализация, чуть дольше"),
    SPEED("Скорость", "FAST", "Минимальная задержка затвора"),
    ZERO_LAG("Без задержки", "ZSL", "Кадр из буфера в момент нажатия")
}

enum class JpegQuality(val value: Int, val label: String) {
    Q85(85, "85"),
    Q95(95, "95"),
    Q100(100, "100")
}

enum class VideoBitrate(val mbps: Int, val label: String) {
    AUTO(0, "АВТО"),
    M35(35, "35 Мбит/с"),
    M70(70, "70 Мбит/с"),
    M120(120, "120 Мбит/с")
}

/** Values are the Camera2 CONTROL_AE_ANTIBANDING_MODE constants. */
enum class AntiBanding(val labelRu: String, val camera2Mode: Int) {
    AUTO("Авто", 3),
    HZ_50("50 Гц", 1),
    HZ_60("60 Гц", 2),
    OFF("Выкл", 0)
}

/** Values are the Camera2 NOISE_REDUCTION_MODE / EDGE_MODE constants; AUTO leaves the key unset. */
enum class ProcessingMode(val labelRu: String, val camera2Mode: Int) {
    AUTO("Авто", -1),
    OFF("Выкл", 0),
    FAST("Быстро", 1),
    HIGH_QUALITY("Макс. качество", 2)
}

enum class IntervalSpacing(val seconds: Int, val label: String) {
    S1(1, "1 с"),
    S2(2, "2 с"),
    S5(5, "5 с"),
    S10(10, "10 с"),
    S30(30, "30 с")
}

enum class IntervalCount(val count: Int, val label: String) {
    C10(10, "10"),
    C30(30, "30"),
    C100(100, "100"),
    UNTIL_STOPPED(0, "∞")
}

/** CameraX vendor extensions; [extensionMode] mirrors androidx.camera.extensions.ExtensionMode. */
enum class SceneExtension(val labelRu: String, val code: String, val extensionMode: Int, val descRu: String) {
    NONE("Обычный", "STD", 0, "Полный ручной контроль"),
    NIGHT("Ночь", "NIGHT", 3, "Многокадровая ночная обработка производителя"),
    HDR("HDR", "HDR", 2, "Расширенный динамический диапазон"),
    BOKEH("Портрет", "BOKEH", 1, "Размытие фона"),
    FACE_RETOUCH("Ретушь", "FACE", 4, "Сглаживание кожи"),
    AUTO("Авто-сцена", "AUTO", 5, "Камера сама выбирает обработку")
}

enum class SelfTimer(val seconds: Int, val label: String) {
    OFF(0, "ВЫКЛ"),
    SEC_3(3, "3 С"),
    SEC_10(10, "10 С")
}

enum class FlashMode(val labelRu: String, val code: String) {
    OFF("Выкл", "OFF"),
    AUTO("Авто", "AUTO"),
    ON("Вкл", "ON"),
    TORCH("Фонарик", "TORCH")
}

enum class VideoResolution(val label: String, val dimensions: String) {
    UHD_4K("4K", "3840×2160"),
    FHD_1080P("1080p", "1920×1080"),
    HD_720P("720p", "1280×720")
}

enum class VideoFps(val fps: Int, val label: String) {
    FPS_24(24, "24"),
    FPS_25(25, "25"),
    FPS_30(30, "30"),
    FPS_50(50, "50"),
    FPS_60(60, "60"),
    FPS_120(120, "120"),
    FPS_240(240, "240"),
    FPS_480(480, "480"),
    FPS_960(960, "960");

    /** 120 fps and above need a constrained high-speed session (slow motion). */
    val isHighSpeed: Boolean get() = fps >= 120

    /** Slow-down factor when a high-speed clip is written for 30 fps playback. */
    val slowMotionFactor: Int get() = (fps / 30).coerceAtLeast(1)
}

/** Values are android.media.MediaRecorder.AudioSource constants. */
enum class AudioSourceOption(val labelRu: String, val mediaRecorderSource: Int, val descRu: String) {
    CAMCORDER("Камкордер", 5, "Микрофон, направленный к сцене, с подавлением шума"),
    MIC("Стандартный", 1, "Основной микрофон, как в диктофоне"),
    UNPROCESSED("Без обработки", 9, "Сырой сигнал без шумоподавления и автоусиления, для монтажа")
}

enum class RecordingLimit(val minutes: Int, val label: String) {
    NONE(0, "Без лимита"),
    M1(1, "1 мин"),
    M5(5, "5 мин"),
    M10(10, "10 мин"),
    M30(30, "30 мин")
}

enum class VideoDynamicRange(val label: String, val descRu: String) {
    SDR("SDR", "H.264 · 8 бит"),
    HLG_10("HDR HLG", "HEVC · 10 бит")
}

/** White balance presets map to the Camera2 AWB modes every device supports. */
enum class AwbPreset(val labelRu: String, val code: String, val kelvinApprox: Int) {
    AUTO("Авто", "AWB", 0),
    INCANDESCENT("Лампа накаливания", "2700K", 2700),
    FLUORESCENT("Флуоресцент", "4000K", 4000),
    DAYLIGHT("Дневной свет", "5500K", 5500),
    CLOUDY("Облачно", "6500K", 6500),
    SHADE("Тень", "7500K", 7500)
}

enum class AnamorphicDeSqueeze(val label: String, val scaleX: Float, val descRu: String) {
    NONE("1.00x", 1.0f, "Без растяжения"),
    ANA_133("1.33x", 1.33f, "Анаморфотный адаптер 1.33x"),
    ANA_155("1.55x", 1.55f, "Анаморфот 1.55x"),
    ANA_180("1.80x", 1.80f, "Анаморфот 1.8x")
}

enum class FrameAspectGuide(val label: String, val ratio: Float?, val descRu: String) {
    OFF("БЕЗ РАМКИ", null, "Полное поле кадра"),
    CINE_239("2.39:1", 2.39f, "Широкий киноформат"),
    FLAT_185("1.85:1", 1.85f, "Кашетированный киноформат"),
    UNIVISIUM_200("2.00:1", 2.0f, "Формат Univisium 2:1"),
    HD_16_9("16:9", 16f / 9f, "Стандарт вещания"),
    VERTICAL_9_16("9:16", 9f / 16f, "Вертикальный формат"),
    SQUARE_1_1("1:1", 1.0f, "Квадрат")
}

enum class GridOverlayMode(val labelRu: String, val code: String) {
    NONE("Выкл", "OFF"),
    RULE_OF_THIRDS("Трети 3×3", "3X3"),
    GOLDEN_RATIO("Золотое сечение", "PHI"),
    DIAGONAL_CROSS("Диагонали и центр", "DIAG"),
    CINE_CROSSHAIR("Кино-перекрестие", "XHAIR")
}

enum class FocusPeakingColor(val labelRu: String, val color: Color) {
    OFF("ВЫКЛ", Color.Transparent),
    NOTHING_RED("КРАСНЫЙ", Color(0xFFFF2A32)),
    SCOPE_AMBER("ЯНТАРНЫЙ", Color(0xFFFFB800)),
    CYAN_SHARP("ГОЛУБОЙ", Color(0xFF00E5FF)),
    CHALK_WHITE("БЕЛЫЙ", Color(0xFFFFFFFF))
}

enum class ZebraThreshold(val label: String, val irePercent: Int?) {
    OFF("ВЫКЛ", null),
    IRE_95("95%", 95),
    IRE_85("85%", 85),
    IRE_70("70%", 70)
}

/**
 * Glyph scenarios. Segments are the 11 light strips of Nothing Phone (2); on a device with the
 * Glyph Interface they are driven through the official SDK, elsewhere the screen says so.
 */
enum class GlyphLightingMode(
    val titleRu: String,
    val subtitleRu: String,
    val code: String,
    val segments: Set<Int>
) {
    OFF(
        "Выключено",
        "Подсветка Glyph не используется",
        "OFF",
        emptySet()
    ),
    STUDIO_SOFT_FILL(
        "Мягкий заполняющий свет",
        "Все полосы Glyph светят ровно, как небольшой софтбокс",
        "FILL",
        (0..10).toSet()
    ),
    CAMERA_RING_MACRO(
        "Кольцевой макро-свет",
        "Только дуги вокруг камер: бестеневая подсветка вблизи",
        "RING",
        setOf(0, 1)
    ),
    PORTRAIT_CATCHLIGHT(
        "Портретный блик",
        "Центральное C-кольцо даёт характерный блик в глазах",
        "CATCH",
        setOf(3, 4, 5, 6, 7, 8)
    ),
    TALLY_AND_AUDIO_VU(
        "Индикатор записи и уровень звука",
        "Нижняя полоса показывает уровень микрофона, точка горит во время записи",
        "VU+REC",
        setOf(9, 10)
    )
}

enum class LutProfile(
    val id: String,
    val nameRu: String,
    val code: String,
    val subtitleRu: String,
    val accentHex: Long,
    val baseContrast: Float,
    val baseSaturation: Float,
    val warmShift: Float,
    val tealShift: Float
) {
    NOTHING_STD(
        id = "nothing_std",
        nameRu = "NOTHING NATURAL",
        code = "N-STD",
        subtitleRu = "Без цветокоррекции: кадр таким, каким его отдаёт камера",
        accentHex = 0xFFF5F5F7,
        baseContrast = 1.0f,
        baseSaturation = 1.0f,
        warmShift = 0f,
        tealShift = 0f
    ),
    N_LOG_FLAT(
        id = "n_log_flat",
        nameRu = "FLAT LOG",
        code = "FLAT",
        subtitleRu = "Пониженный контраст и насыщенность: запас для цветокоррекции потом",
        accentHex = 0xFFA0A0AB,
        baseContrast = 0.72f,
        baseSaturation = 0.62f,
        warmShift = 0.02f,
        tealShift = 0.02f
    ),
    KODAK_2383_TEAL(
        id = "kodak_2383",
        nameRu = "CINE TEAL & ORANGE",
        code = "K-2383",
        subtitleRu = "Киноплёночный вид: холодные тени и тёплые тона кожи",
        accentHex = 0xFF00E5FF,
        baseContrast = 1.15f,
        baseSaturation = 1.12f,
        warmShift = 0.12f,
        tealShift = 0.16f
    ),
    NOTHING_MONO_RED(
        id = "mono_red",
        nameRu = "MONO · RED FILTER",
        code = "B&W-R",
        subtitleRu = "Контрастное чёрно-белое с имитацией красного фильтра",
        accentHex = 0xFFD71921,
        baseContrast = 1.32f,
        baseSaturation = 0.0f,
        warmShift = 0f,
        tealShift = 0f
    ),
    CYBER_HALATION(
        id = "cyber_2099",
        nameRu = "NEON 800T",
        code = "800T",
        subtitleRu = "Ночной вольфрамовый вид для неона и города",
        accentHex = 0xFFFF4081,
        baseContrast = 1.18f,
        baseSaturation = 1.24f,
        warmShift = -0.08f,
        tealShift = 0.22f
    ),
    BLEACH_BYPASS(
        id = "bleach_bypass",
        nameRu = "BLEACH BYPASS",
        code = "BLCH",
        subtitleRu = "Жёсткий микроконтраст и приглушённый цвет",
        accentHex = 0xFFFFB800,
        baseContrast = 1.28f,
        baseSaturation = 0.48f,
        warmShift = -0.04f,
        tealShift = 0.06f
    );

    val isIdentity: Boolean get() = this == NOTHING_STD

    /** The 4×5 colour matrix applied to the live preview and baked into photos. */
    fun buildColorMatrix(
        userContrast: Float = 1.0f,
        userSaturation: Float = 1.0f,
        shadowLift: Float = 0.0f,
        highlightRollOff: Float = 0.0f
    ): ColorMatrix {
        val totalSat = (baseSaturation * userSaturation).coerceIn(0f, 2f)
        val totalContrast = (baseContrast * userContrast * (1f - highlightRollOff * 0.2f)).coerceIn(0.4f, 2.0f)
        val translate = (1f - totalContrast) * 128f + (shadowLift * 36f)

        val satMatrix = ColorMatrix().apply { setToSaturation(totalSat) }
        val rScale = totalContrast * (1f + warmShift)
        val gScale = totalContrast * (1f + tealShift * 0.4f)
        val bScale = totalContrast * (1f + tealShift - warmShift * 0.5f)

        val gradeMatrix = ColorMatrix(
            floatArrayOf(
                rScale, 0f, 0f, 0f, translate,
                0f, gScale, 0f, 0f, translate,
                0f, 0f, bScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.timesAssign(gradeMatrix)
        return satMatrix
    }
}

data class FocusPullPoint(
    val slot: String, // "A", "B", "C"
    val distanceDiopters: Float, // 0f = infinity, 10f = 0.1 m
    val labelMeters: String
)

/** Everything the scopes and overlays draw. Computed from real preview frames only. */
data class LiveScopeTelemetry(
    val hasFrame: Boolean = false,
    val lumaHistogram: IntArray = IntArray(64),
    val redHistogram: IntArray = IntArray(64),
    val greenHistogram: IntArray = IntArray(64),
    val blueHistogram: IntArray = IntArray(64),
    val waveformColumns: FloatArray = FloatArray(48),
    val vectorscopePoints: List<Pair<Float, Float>> = emptyList(), // normalized (-1..1) U/V
    val zebraGrid: Array<BooleanArray> = Array(12) { BooleanArray(16) },
    val peakingGrid: Array<FloatArray> = Array(16) { FloatArray(24) },
    val falseColorGrid: Array<IntArray> = Array(12) { IntArray(16) },
    val averageIre: Int = 0,
    val highlightClipPercent: Float = 0f,
    val shadowCrushPercent: Float = 0f,
    val sceneContrastStops: Float = 0f,
    val colorTempEstimateK: Int = 0
)

/** What the attached camera can do, probed once per bind from CameraX and Camera2 characteristics. */
data class CameraCapabilities(
    val cameraLabel: String = "",
    val isFront: Boolean = false,
    val hasFrontCamera: Boolean = false,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val hasFlash: Boolean = false,
    val manualSensor: Boolean = false,
    val isoMin: Int = 100,
    val isoMax: Int = 800,
    val exposureMinNs: Long = 100_000L,
    val exposureMaxNs: Long = 100_000_000L,
    val manualFocus: Boolean = false,
    val minFocusDiopters: Float = 0f,
    val evIndexMin: Int = 0,
    val evIndexMax: Int = 0,
    val evStep: Float = 0f,
    val photoFormats: Set<PhotoFormat> = setOf(PhotoFormat.JPEG),
    val videoResolutions: List<VideoResolution> = listOf(VideoResolution.FHD_1080P),
    val videoFps: List<VideoFps> = listOf(VideoFps.FPS_30),
    val hdrVideo: Boolean = false,
    val previewStabilization: Boolean = false,
    val videoStabilization: Boolean = false,
    val sensorMegapixels: Float = 0f,
    val apertureLabel: String = "",
    val focalEquivMm: Int = 0,
    val lensOptions: List<LensOption> = emptyList(),
    val activeCameraId: String = "",
    val cameraKind: LensKind = LensKind.MAIN,
    /** Standard-session frame rates per resolution (Camera2 stream map + AE ranges). */
    val fpsByResolution: Map<VideoResolution, List<VideoFps>> = emptyMap(),
    /** High-speed (slow-motion) frame rates per resolution. */
    val highSpeedFpsByResolution: Map<VideoResolution, List<VideoFps>> = emptyMap(),
    val zslSupported: Boolean = false,
    val antiBandingModes: Set<AntiBanding> = setOf(AntiBanding.AUTO),
    val noiseReductionModes: Set<ProcessingMode> = setOf(ProcessingMode.AUTO),
    val edgeModes: Set<ProcessingMode> = setOf(ProcessingMode.AUTO),
    val availableExtensions: Set<SceneExtension> = setOf(SceneExtension.NONE)
) {
    val evRange: Float get() = if (evStep > 0f) evIndexMax * evStep else 0f

    /** Frame rates this resolution can record, standard first, then slow-motion. */
    fun fpsFor(resolution: VideoResolution): List<VideoFps> {
        if (fpsByResolution.isEmpty() && highSpeedFpsByResolution.isEmpty()) return videoFps
        return (fpsByResolution[resolution].orEmpty() + highSpeedFpsByResolution[resolution].orEmpty())
            .distinct()
            .sortedBy { it.fps }
    }

    fun supports(resolution: VideoResolution, fps: VideoFps): Boolean = fpsFor(resolution).contains(fps)

    /** Resolutions that can record at the given frame rate, highest first. */
    fun resolutionsFor(fps: VideoFps): List<VideoResolution> = VideoResolution.entries.filter { supports(it, fps) }

    val allVideoFps: List<VideoFps>
        get() = VideoResolution.entries.flatMap { fpsFor(it) }.distinct().sortedBy { it.fps }.ifEmpty { videoFps }
}

sealed class CameraStatus {
    object NoPermission : CameraStatus()
    object Starting : CameraStatus()
    data class Ready(val detail: String) : CameraStatus()
    data class Error(val message: String) : CameraStatus()
}

/** Live values reported by the camera pipeline for the current frame. */
data class LiveExposureInfo(
    val iso: Int = 0,
    val exposureNs: Long = 0L,
    val focusDistanceDiopters: Float = 0f,
    val afLocked: Boolean = false,
    val aeConverged: Boolean = false
)

/** A file written to MediaStore by this app, with the metadata read back from it. */
data class SavedCapture(
    val uri: String,
    val isVideo: Boolean,
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val durationMs: Long,
    val iso: Int?,
    val exposureTimeSec: Double?,
    val fNumber: Double?,
    val focalLengthMm: Double?,
    val lutBaked: Boolean,
    val histogram16: IntArray
)
