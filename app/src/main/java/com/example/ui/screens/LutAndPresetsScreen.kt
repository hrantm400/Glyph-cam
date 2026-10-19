package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CameraPresetEntity
import com.example.model.LutProfile
import com.example.ui.components.ChipShape
import com.example.ui.components.CodeBadge
import com.example.ui.components.LabelledSlider
import com.example.ui.components.NothingCard
import com.example.ui.components.SectionLabel
import com.example.ui.components.StudioHeader
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingSurfaceHigh
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.ScopeAmber
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.ScopeGreen
import com.example.viewmodel.ProCameraUiState
import java.util.Locale

@Composable
fun LutAndPresetsScreen(
    state: ProCameraUiState,
    presets: List<CameraPresetEntity>,
    onSelectLut: (LutProfile) -> Unit,
    onUpdateGrade: (Float, Float, Float, Float) -> Unit,
    onToggleBake: () -> Unit,
    onResetGrade: () -> Unit,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (CameraPresetEntity) -> Unit,
    onDeletePreset: (Int) -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToCamera)
    var newPresetName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .testTag("lut_and_presets_screen")
    ) {
        StudioHeader(
            title = "LUT и цвет",
            subtitle = "ЦВЕТОВЫЕ ПРОФИЛИ · ГРЕЙДИНГ · ПРЕСЕТЫ",
            onBack = onBackToCamera,
            backTag = "lut_back_btn"
        ) {
            HeaderIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Сбросить грейдинг",
                active = false,
                onClick = onResetGrade,
                tag = "btn_reset_color_grade"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            LutCalibrationPreviewCard(state = state)

            Spacer(modifier = Modifier.height(10.dp))
            NothingCard(
                onClick = onToggleBake,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("switch_bake_lut")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Впечатывать LUT в JPEG",
                            style = MaterialTheme.typography.titleMedium,
                            color = NothingWhite
                        )
                        Text(
                            text = if (state.bakeLutIntoPhotos) {
                                "Фото сохраняются уже с цветом профиля (EXIF сохраняется). RAW и видео остаются чистыми: для них LUT только мониторинг."
                            } else {
                                "Фото сохраняются без обработки, LUT виден только в видоискателе."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NothingSilver
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = state.bakeLutIntoPhotos,
                        onCheckedChange = { onToggleBake() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingBlack,
                            checkedTrackColor = NothingWhite,
                            uncheckedThumbColor = NothingSilver,
                            uncheckedTrackColor = NothingSurfaceHigh,
                            uncheckedBorderColor = NothingBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel(text = "Цветовые профили")
            Spacer(modifier = Modifier.height(8.dp))

            LutProfile.entries.forEach { lut ->
                val selected = state.activeLut == lut
                val accent = Color(lut.accentHex)
                NothingCard(
                    selected = selected,
                    accent = accent,
                    onClick = { onSelectLut(lut) },
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("lut_card_${lut.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LutSwatchStrip(lut = lut, modifier = Modifier.size(width = 44.dp, height = 36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = lut.nameRu,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NothingWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CodeBadge(text = lut.code, color = accent)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lut.subtitleRu,
                                style = MaterialTheme.typography.bodySmall,
                                color = NothingSilver
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            NothingCard(contentPadding = PaddingValues(14.dp), modifier = Modifier.fillMaxWidth()) {
                SectionLabel(text = "Тонкая настройка цвета")
                Spacer(modifier = Modifier.height(6.dp))
                LabelledSlider(
                    label = "Контраст",
                    valueText = String.format(Locale.US, "%d%%", (state.userContrast * 100).toInt()),
                    value = state.userContrast,
                    onValueChange = { onUpdateGrade(it, state.userSaturation, state.shadowLift, state.highlightRollOff) },
                    valueRange = 0.5f..1.5f,
                    accent = NothingWhite
                )
                LabelledSlider(
                    label = "Насыщенность",
                    valueText = String.format(Locale.US, "%d%%", (state.userSaturation * 100).toInt()),
                    value = state.userSaturation,
                    onValueChange = { onUpdateGrade(state.userContrast, it, state.shadowLift, state.highlightRollOff) },
                    valueRange = 0.0f..1.8f,
                    accent = ScopeCyan
                )
                LabelledSlider(
                    label = "Подъём теней",
                    valueText = String.format(Locale.US, "%+.2f", state.shadowLift),
                    value = state.shadowLift,
                    onValueChange = { onUpdateGrade(state.userContrast, state.userSaturation, it, state.highlightRollOff) },
                    valueRange = -0.4f..0.4f,
                    accent = ScopeGreen
                )
                LabelledSlider(
                    label = "Мягкие света",
                    valueText = String.format(Locale.US, "%d%%", (state.highlightRollOff * 100).toInt()),
                    value = state.highlightRollOff,
                    onValueChange = { onUpdateGrade(state.userContrast, state.userSaturation, state.shadowLift, it) },
                    valueRange = 0.0f..1.0f,
                    accent = ScopeAmber
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(
                text = "Пресеты камеры",
                trailing = "${presets.size}",
                trailingColor = NothingWhite
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    placeholder = {
                        Text(
                            text = "Название пресета",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NothingSilver
                        )
                    },
                    singleLine = true,
                    shape = ChipShape,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingWhite,
                        unfocusedBorderColor = NothingBorder,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite,
                        cursorColor = NothingRed,
                        focusedContainerColor = NothingSurface,
                        unfocusedContainerColor = NothingSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_preset_name")
                )
                Button(
                    onClick = {
                        onSavePreset(newPresetName)
                        newPresetName = ""
                    },
                    shape = ChipShape,
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = NothingWhite),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("btn_save_preset")
                ) {
                    Text("СОХРАНИТЬ", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Пресет запоминает объектив, ISO, выдержку, баланс белого, EV, профиль, рамку, Glyph и параметры видео.",
                style = MaterialTheme.typography.labelSmall,
                color = NothingSilver
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (presets.isEmpty()) {
                Text(
                    text = "Пресетов пока нет. Настройте камеру и сохраните её состояние под своим именем.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NothingSilver
                )
            }

            presets.forEach { preset ->
                NothingCard(
                    onClick = { onApplyPreset(preset) },
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("preset_item_${preset.slotCode}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingRed.copy(alpha = 0.18f))
                                .border(1.dp, NothingRed, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = preset.slotCode, style = MaterialTheme.typography.labelLarge, color = NothingWhite)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = NothingWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = presetSummary(preset),
                                style = MaterialTheme.typography.labelSmall,
                                color = NothingSilver,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onDeletePreset(preset.id) },
                            modifier = Modifier.testTag("delete_preset_${preset.slotCode}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить пресет",
                                tint = NothingSilver
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun presetSummary(preset: CameraPresetEntity): String {
    val lut = LutProfile.entries.find { it.id == preset.lutId }?.code ?: preset.lutId
    val iso = if (preset.isoValue == 0) "ISO AUTO" else "ISO ${preset.isoValue}"
    val zoom = String.format(Locale.US, "%.1f×", preset.zoomRatio)
    val wb = if (preset.awbPreset == "AUTO") "AWB" else preset.awbPreset
    return if (preset.isVideoPreset) {
        val res = preset.videoResolution.removePrefix("UHD_").removePrefix("FHD_").removePrefix("HD_")
        "ВИДЕО · $res/${preset.videoFps} · $zoom · $iso · $wb · $lut"
    } else {
        "ФОТО · $zoom · $iso · $wb · EV ${String.format(Locale.US, "%+.1f", preset.evCompensation)} · $lut"
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(ChipShape)
            .background(if (active) NothingRed else NothingSurface)
            .border(1.dp, if (active) NothingRed else NothingBorder, ChipShape)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) NothingWhite else NothingSilver,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** ColorChecker-like patches pushed through the real 4×5 matrix the preview and JPEGs use. */
private val ReferencePatches = listOf(
    Color(0xFFC29682), // light skin
    Color(0xFF735244), // dark skin
    Color(0xFF627A9D), // sky
    Color(0xFF00A6B4), // teal
    Color(0xFFE07A3C), // amber
    Color(0xFFD71921), // red
    Color(0xFF479458), // foliage
    Color(0xFF383D96), // cobalt
    Color(0xFF1A1A1A),
    Color(0xFF555555),
    Color(0xFF9A9A9A),
    Color(0xFFE8E8E8)
)

/** Applies an Android-layout colour matrix (row-major 4×5, translation in 0..255) to one colour. */
fun applyColorMatrix(color: Color, m: FloatArray?): Color {
    if (m == null || m.size < 20) return color
    val r = color.red * 255f
    val g = color.green * 255f
    val b = color.blue * 255f
    val a = color.alpha * 255f
    val nr = (m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4]) / 255f
    val ng = (m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9]) / 255f
    val nb = (m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14]) / 255f
    return Color(nr.coerceIn(0f, 1f), ng.coerceIn(0f, 1f), nb.coerceIn(0f, 1f), color.alpha)
}

@Composable
private fun LutSwatchStrip(lut: LutProfile, modifier: Modifier = Modifier) {
    val matrix = remember(lut) { if (lut.isIdentity) null else lut.buildColorMatrix().values }
    Canvas(modifier = modifier.clip(RoundedCornerShape(6.dp))) {
        val patches = ReferencePatches.take(8)
        val cellW = size.width / 4
        val cellH = size.height / 2
        patches.forEachIndexed { idx, base ->
            drawRect(
                color = applyColorMatrix(base, matrix),
                topLeft = Offset((idx % 4) * cellW, (idx / 4) * cellH),
                size = Size(cellW + 0.5f, cellH + 0.5f)
            )
        }
    }
}

@Composable
private fun LutCalibrationPreviewCard(state: ProCameraUiState) {
    val matrix = state.lutColorMatrix
    NothingCard(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${state.activeLut.nameRu} · ${state.activeLut.code}",
                style = MaterialTheme.typography.labelMedium,
                color = NothingWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (matrix == null) "БЕЗ ИЗМЕНЕНИЙ" else if (state.bakeLutIntoPhotos) "ПРЕВЬЮ + JPEG" else "ТОЛЬКО ПРЕВЬЮ",
                style = MaterialTheme.typography.labelMedium,
                color = ScopeAmber
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            val w = size.width
            val h = size.height
            val colorPatches = ReferencePatches.take(8)
            val patchW = w / colorPatches.size
            val topH = h * 0.6f
            colorPatches.forEachIndexed { idx, base ->
                drawRect(
                    color = applyColorMatrix(base, matrix),
                    topLeft = Offset(idx * patchW, 0f),
                    size = Size(patchW + 1f, topH)
                )
            }
            val steps = 16
            val stepW = w / steps
            for (i in 0 until steps) {
                val v = i / (steps - 1f)
                val gray = Color(v, v, v)
                drawRect(
                    color = applyColorMatrix(gray, matrix),
                    topLeft = Offset(i * stepW, topH),
                    size = Size(stepW + 1f, h - topH)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Сверху: контрольные цвета, снизу: серая шкала. Так же профиль ляжет на живое превью и на JPEG.",
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
