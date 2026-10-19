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
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
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
    onUpdateGrade: (Float, Float, Float, Float, Float) -> Unit,
    onToggleSplitCompare: () -> Unit,
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
            subtitle = "3D LUT · ГРЕЙДИНГ · ПРЕСЕТЫ C1–C9",
            onBack = onBackToCamera,
            backTag = "lut_back_btn"
        ) {
            HeaderIconButton(
                icon = Icons.Default.Compare,
                contentDescription = "Сравнить до и после",
                active = state.splitScreenLutCompare,
                onClick = onToggleSplitCompare,
                tag = "btn_split_lut_compare"
            )
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

            Spacer(modifier = Modifier.height(18.dp))
            SectionLabel(text = "Кинопрофили · 3D LUT")
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
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (selected) accent else accent.copy(alpha = 0.45f))
                        )
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
                    onValueChange = {
                        onUpdateGrade(it, state.userSaturation, state.shadowLift, state.highlightRollOff, state.filmGrainAmount)
                    },
                    valueRange = 0.5f..1.5f,
                    accent = NothingWhite
                )
                LabelledSlider(
                    label = "Насыщенность",
                    valueText = String.format(Locale.US, "%d%%", (state.userSaturation * 100).toInt()),
                    value = state.userSaturation,
                    onValueChange = {
                        onUpdateGrade(state.userContrast, it, state.shadowLift, state.highlightRollOff, state.filmGrainAmount)
                    },
                    valueRange = 0.0f..1.8f,
                    accent = ScopeCyan
                )
                LabelledSlider(
                    label = "Подъём теней",
                    valueText = String.format(Locale.US, "%+.2f", state.shadowLift),
                    value = state.shadowLift,
                    onValueChange = {
                        onUpdateGrade(state.userContrast, state.userSaturation, it, state.highlightRollOff, state.filmGrainAmount)
                    },
                    valueRange = -0.4f..0.4f,
                    accent = ScopeGreen
                )
                LabelledSlider(
                    label = "Мягкие света",
                    valueText = String.format(Locale.US, "%d%%", (state.highlightRollOff * 100).toInt()),
                    value = state.highlightRollOff,
                    onValueChange = {
                        onUpdateGrade(state.userContrast, state.userSaturation, state.shadowLift, it, state.filmGrainAmount)
                    },
                    valueRange = 0.0f..1.0f,
                    accent = ScopeAmber
                )
                LabelledSlider(
                    label = "Плёночное зерно",
                    valueText = String.format(Locale.US, "%d%%", (state.filmGrainAmount * 100).toInt()),
                    value = state.filmGrainAmount,
                    onValueChange = {
                        onUpdateGrade(state.userContrast, state.userSaturation, state.shadowLift, state.highlightRollOff, it)
                    },
                    valueRange = 0.0f..1.0f,
                    accent = NothingRed
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(
                text = "Пресеты камеры",
                trailing = "${presets.size} / 9",
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
                            text = "Название рига",
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

            Spacer(modifier = Modifier.height(10.dp))

            if (presets.isEmpty()) {
                Text(
                    text = "Пресетов пока нет. Настройте камеру и сохраните риг под своим именем.",
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
                            Text(
                                text = preset.slotCode,
                                style = MaterialTheme.typography.labelLarge,
                                color = NothingWhite
                            )
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
                                text = "${if (preset.isVideoPreset) "CINE" else "PHOTO"} · ISO ${if (preset.isoValue == 0) "AUTO" else preset.isoValue} · ${preset.wbKelvin}K · ${preset.anamorphicLabel}",
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

@Composable
private fun LutCalibrationPreviewCard(state: ProCameraUiState) {
    NothingCard(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "КАЛИБРОВКА · ${state.activeLut.code}",
                style = MaterialTheme.typography.labelMedium,
                color = NothingWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.splitScreenLutCompare) "SPLIT" else "FULL",
                style = MaterialTheme.typography.labelMedium,
                color = ScopeAmber
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            val w = size.width
            val h = size.height
            // ColorChecker-style cinema swatches + grayscale ramp
            val patches = listOf(
                Color(0xFFC29682), // Light Skin
                Color(0xFF735244), // Dark Skin
                Color(0xFF627A9D), // Foliage/Sky
                Color(0xFF00A6B4), // Teal
                Color(0xFFE07A3C), // Warm Amber
                Color(0xFFD71921), // Nothing Crimson
                Color(0xFF479458), // Green
                Color(0xFF383D96)  // Cobalt
            )
            val patchW = w / patches.size
            val topH = h * 0.65f
            patches.forEachIndexed { idx, baseColor ->
                val sat = (state.activeLut.baseSaturation * state.userSaturation).coerceIn(0f, 1.8f)
                val gray = baseColor.red * 0.299f + baseColor.green * 0.587f + baseColor.blue * 0.114f
                val r = (gray + (baseColor.red - gray) * sat + state.activeLut.warmShift * 0.2f).coerceIn(0f, 1f)
                val g = (gray + (baseColor.green - gray) * sat).coerceIn(0f, 1f)
                val b = (gray + (baseColor.blue - gray) * sat + state.activeLut.tealShift * 0.2f).coerceIn(0f, 1f)
                drawRect(
                    color = Color(r, g, b),
                    topLeft = Offset(idx * patchW, 0f),
                    size = Size(patchW + 1f, topH)
                )
            }
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = (1f - state.shadowLift.coerceAtLeast(0f)).coerceIn(0.4f, 1f)),
                        Color.Gray,
                        Color.White.copy(alpha = (1f - state.highlightRollOff * 0.25f).coerceIn(0.6f, 1f))
                    )
                ),
                topLeft = Offset(0f, topH),
                size = Size(w, h - topH)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.activeLut.nameRu,
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
