package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix

enum class AppMode(val titleRu: String, val badgeCode: String) {
    PHOTO_PRO("ФОТО PRO", "STILL // RAW"),
    CINE_VIDEO("КИНО ВИДЕО", "CINE // LOG"),
    GLYPH_STUDIO("ГЛИФ-СВЕТ", "GLYPH // 33Z"),
    LUT_LAB("LUT & ЦВЕТ", "COLOR // 3D"),
    MEDIA_VAULT("АРХИВ EXIF", "VAULT // DNG")
}

enum class NothingLens(
    val id: String,
    val focalLabel: String,
    val sensorName: String,
    val aperture: String,
    val zoomRatio: Float,
    val isFront: Boolean = false,
    val supportsMacro: Boolean = false
) {
    ULTRA_WIDE(
        id = "ultra",
        focalLabel = "14mm",
        sensorName = "SAMSUNG JN1 • 50MP",
        aperture = "f/2.2",
        zoomRatio = 0.6f,
        supportsMacro = true
    ),
    MAIN_WIDE(
        id = "main",
        focalLabel = "24mm",
        sensorName = "SONY IMX890 • 50MP OIS",
        aperture = "f/1.88",
        zoomRatio = 1.0f
    ),
    CROP_2X(
        id = "tele2x",
        focalLabel = "48mm",
        sensorName = "IMX890 IN-SENSOR 2X",
        aperture = "f/1.88",
        zoomRatio = 2.0f
    ),
    CINE_35MM(
        id = "cine35",
        focalLabel = "35mm",
        sensorName = "IMX890 CINE PRIME",
        aperture = "f/1.88",
        zoomRatio = 1.45f
    ),
    FRONT_32MP(
        id = "front",
        focalLabel = "22mm",
        sensorName = "SONY IMX615 • 32MP",
        aperture = "f/2.45",
        zoomRatio = 1.0f,
        isFront = true
    )
}

enum class PhotoFormat(val label: String, val badge: String, val bitDepth: String) {
    JPEG_FINE("JPEG FINE", "JPG", "8-BIT"),
    RAW_DNG("RAW DNG", "DNG", "12-BIT"),
    RAW_PLUS_JPEG("RAW + JPG", "R+J", "12-BIT"),
    HIRES_50MP("50MP ULTRA", "50M", "10-BIT")
}

enum class PhotoDriveMode(val labelRu: String, val code: String, val burstCount: Int = 1) {
    SINGLE("ОДИНОЧНЫЙ КАДР", "SGL", 1),
    BURST_FAST("СЕРИЯ 5 КАДРОВ", "BRS", 5),
    AEB_BRACKET("БРЕКЕТИНГ AEB ±2EV", "AEB", 3),
    INTERVAL("ИНТЕРВАЛОМЕТР 3с", "INT", 1),
    LONG_EXP_ND("ND-ФИЛЬТР ДЛИН. ВЫД.", "ND+", 1)
}

enum class VideoResolution(val label: String, val dimensions: String, val widthPx: Int, val heightPx: Int) {
    DCI_4K("4K DCI", "4096×2160", 4096, 2160),
    UHD_4K("4K UHD", "3840×2160", 3840, 2160),
    ANAMORPHIC_2_8K("2.8K ANA", "2880×2160", 2880, 2160),
    FHD_1080P("1080P PRO", "1920×1080", 1920, 1080)
}

enum class VideoFps(val fps: Int, val label: String, val cinema180ShutterNs: Long) {
    FPS_24(24, "24.00p CINE", 20_833_333L),     // 1/48s
    FPS_25(25, "25.00p PAL", 20_000_000L),      // 1/50s
    FPS_30(30, "29.97p NTSC", 16_666_667L),     // 1/60s
    FPS_60(60, "59.94p HIGH", 8_333_333L),      // 1/120s
    FPS_120(120, "120p S&Q", 4_166_667L)        // 1/240s
}

enum class VideoCodecProfile(val label: String, val bitrateMbps: Int, val dynamicRange: String) {
    HEVC_ALL_I("H.265 ALL-I 10-BIT", 180, "N-LOG 12-STOP"),
    HEVC_LONG_GOP("H.265 4:2:0 10-BIT", 100, "HLG BT.2020"),
    AVC_REC709("H.264 HIGH PROFILE", 60, "REC.709 STD")
}

enum class AnamorphicDeSqueeze(val label: String, val scaleX: Float, val descRu: String) {
    NONE("1.00x SPH", 1.0f, "Сферическая оптика"),
    ANA_133("1.33x ANA", 1.33f, "Анаморфотный адаптер 1.33x"),
    ANA_155("1.55x SCOPE", 1.55f, "Широкоэкранный кино-анаморф 1.55x"),
    ANA_180("1.80x FULL", 1.80f, "Полный анаморфот 1.8x на 4:3")
}

enum class FrameAspectGuide(val label: String, val ratio: Float?, val descRu: String) {
    OFF("БЕЗ РАМКИ", null, "Полное поле сенсора"),
    CINE_239("2.39:1 SCOPE", 2.39f, "Широкий киноформат Anamorphic Scope"),
    FLAT_185("1.85:1 FLAT", 1.85f, "Американский кашетированный киноформат"),
    UNIVISIUM_200("2.00:1 NETFLIX", 2.0f, "Формат Univisium 2:1"),
    HD_16_9("16:9 UHD", 16f / 9f, "Стандарт широкого вещания"),
    VERTICAL_9_16("9:16 REELS", 9f / 16f, "Вертикальный формат с центральным маркером"),
    SQUARE_1_1("1:1 MEDIUM", 1.0f, "Средний формат 6×6")
}

enum class GridOverlayMode(val labelRu: String, val code: String) {
    NONE("ВЫКЛ", "OFF"),
    RULE_OF_THIRDS("ТРЕТИ 3×3", "3X3"),
    GOLDEN_RATIO("ЗОЛОТОЕ СЕЧЕНИЕ", "PHI"),
    DIAGONAL_CROSS("ДИАГОНАЛИ + ЦЕНТР", "DIAG"),
    CINE_CROSSHAIR("КИНО-ПЕРЕКРЕСТИЕ", "XHAIR")
}

enum class FocusPeakingColor(val labelRu: String, val color: Color) {
    OFF("ВЫКЛ", Color.Transparent),
    NOTHING_RED("КРАСНЫЙ NOTHING", Color(0xFFFF2A32)),
    SCOPE_AMBER("ЯНТАРНЫЙ ARRI", Color(0xFFFFB800)),
    CYAN_SHARP("ГОЛУБОЙ CYAN", Color(0xFF00E5FF)),
    CHALK_WHITE("БЕЛЫЙ КОНТУР", Color(0xFFFFFFFF))
}

enum class ZebraThreshold(val label: String, val irePercent: Int?) {
    OFF("ВЫКЛ", null),
    IRE_95("95% ПЕРЕСВЕТ", 95),
    IRE_85("85% СВЕТА", 85),
    IRE_70("70% КОЖА (SKIN)", 70)
}

enum class MeteringMode(val labelRu: String, val code: String) {
    MATRIX("МАТРИЧНЫЙ", "MTX"),
    CENTER_WEIGHTED("ЦЕНТРОВЗВЕШ.", "CTR"),
    SPOT("ТОЧЕЧНЫЙ SPOT", "SPT"),
    HIGHLIGHT_PROTECT("ПО СВЕТАМ (ETTR)", "HLT")
}

enum class StabilizationMode(val labelRu: String, val code: String) {
    HYBRID_OIS_EIS("OIS + EIS ГИБРИД", "HYB"),
    OPTICAL_OIS("ТОЛЬКО OIS ОПТИКА", "OIS"),
    TRIPOD_LOCK("ШТАТИВ (ВЫКЛ EIS)", "TRPD"),
    HORIZON_LOCK("ГОРИЗОНТ 360°", "HRZ")
}

enum class AudioDirectionMode(val labelRu: String, val code: String) {
    STEREO_WIDE("СТЕРЕО 360° (3 МИК.)", "WIDE"),
    FRONT_CARDIOID("ФРОНТ КАРДИОИДА", "FRNT"),
    REAR_NARRATOR("ТЫЛОВОЙ ГОЛОС ОПЕРАТОРА", "REAR"),
    ZOOM_AUDIO("АУДИО-ЗУМ ПО ФОКУСУ", "ZOOM")
}

enum class GlyphLightingMode(
    val titleRu: String,
    val subtitleRu: String,
    val code: String,
    val activeZones: Set<Int>
) {
    OFF(
        "ВЫКЛЮЧЕНО",
        "Подсветка Glyph не используется",
        "OFF",
        emptySet()
    ),
    STUDIO_SOFT_FILL(
        "МЯГКИЙ ЗАПОЛНЯЮЩИЙ СВЕТ",
        "Все 33 зоны Glyph работают как рассеянный студийный софтбокс без резких теней вспышки",
        "FILL 100%",
        setOf(0, 1, 2, 3, 4)
    ),
    CAMERA_RING_MACRO(
        "КОЛЬЦЕВОЙ МАКРО-СВЕТ",
        "Только верхнее кольцо вокруг объективов IMX890/JN1 для бестеневого макро с 4 см",
        "RING",
        setOf(0, 1)
    ),
    PORTRAIT_CATCHLIGHT(
        "ПОРТРЕТНЫЙ БЛИК (C-RING)",
        "Центральное полукольцо + диагональ создают фирменный блик Nothing в глазах модели",
        "CATCH",
        setOf(0, 2)
    ),
    TALLY_AND_AUDIO_VU(
        "ИНДИКАТОР ЗАПИСИ + АУДИО VU",
        "Нижняя полоса работает как живой пикметр микрофона, красная точка сигнализирует REC",
        "VU+REC",
        setOf(1, 3, 4)
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
        subtitleRu = "Заводская калибровка сенсора Sony IMX890 с точной цветопередачей",
        accentHex = 0xFFF5F5F7,
        baseContrast = 1.0f,
        baseSaturation = 1.0f,
        warmShift = 0f,
        tealShift = 0f
    ),
    N_LOG_FLAT(
        id = "n_log_flat",
        nameRu = "NOTHING N-LOG 12-STOP",
        code = "N-LOG",
        subtitleRu = "Логарифмический плоский профиль для сохранения максимума деталей в тенях и светах",
        accentHex = 0xFFA0A0AB,
        baseContrast = 0.72f,
        baseSaturation = 0.62f,
        warmShift = 0.02f,
        tealShift = 0.02f
    ),
    KODAK_2383_TEAL(
        id = "kodak_2383",
        nameRu = "CINE 2383 TEAL & ORANGE",
        code = "K-2383",
        subtitleRu = "Киноплёнка Kodak Vision3: холодные циановые тени и тёплые скинтоны",
        accentHex = 0xFF00E5FF,
        baseContrast = 1.15f,
        baseSaturation = 1.12f,
        warmShift = 0.12f,
        tealShift = 0.16f
    ),
    NOTHING_MONO_RED(
        id = "mono_red",
        nameRu = "NOTHING MONO // RED FILTER",
        code = "B&W-R",
        subtitleRu = "Высококонтрастный чёрно-белый режим с симуляцией красного оптического фильтра",
        accentHex = 0xFFD71921,
        baseContrast = 1.32f,
        baseSaturation = 0.0f,
        warmShift = 0f,
        tealShift = 0f
    ),
    CYBER_HALATION(
        id = "cyber_2099",
        nameRu = "NEON HALATION 800T",
        code = "800T",
        subtitleRu = "Ночная киноплёнка Cinestill 800T для неоновых огней и ночного города",
        accentHex = 0xFFFF4081,
        baseContrast = 1.18f,
        baseSaturation = 1.24f,
        warmShift = -0.08f,
        tealShift = 0.22f
    ),
    BLEACH_BYPASS(
        id = "bleach_bypass",
        nameRu = "SILVER RETENTION GRIT",
        code = "BLCH",
        subtitleRu = "Удержание серебра в эмульсии: жёсткий металлический микроконтраст и приглушённый цвет",
        accentHex = 0xFFFFB800,
        baseContrast = 1.28f,
        baseSaturation = 0.48f,
        warmShift = -0.04f,
        tealShift = 0.06f
    );

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
    val distanceDiopters: Float, // 0f = Infinity, 10f = 0.1m macro
    val labelMeters: String
)

data class LiveScopeTelemetry(
    val lumaHistogram: IntArray = IntArray(64) { 10 },
    val redHistogram: IntArray = IntArray(64) { 8 },
    val greenHistogram: IntArray = IntArray(64) { 10 },
    val blueHistogram: IntArray = IntArray(64) { 9 },
    val waveformColumns: FloatArray = FloatArray(48) { 0.45f },
    val vectorscopePoints: List<Pair<Float, Float>> = emptyList(), // normalized (-1..1, -1..1) U/V
    val zebraGrid: Array<BooleanArray> = Array(12) { BooleanArray(16) },
    val peakingGrid: Array<FloatArray> = Array(16) { FloatArray(24) },
    val falseColorGrid: Array<IntArray> = Array(12) { IntArray(16) { 50 } },
    val averageIre: Int = 48,
    val highlightClipPercent: Float = 0.4f,
    val shadowCrushPercent: Float = 0.8f,
    val dynamicRangeStops: Float = 11.8f,
    val colorTempEstimateK: Int = 5400
)
