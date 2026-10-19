package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CameraPresetEntity
import com.example.model.LutProfile
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingObsidian
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("lut_and_presets_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToCamera,
                    modifier = Modifier.testTag("lut_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад к камере",
                        tint = NothingWhite
                    )
                }
                Column {
                    Text(
                        text = "3D LUT LAB & ПРЕСЕТЫ C1-C4",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NothingWhite
                    )
                    Text(
                        text = "ЦВЕТОКОРРЕКЦИЯ В РЕАЛЬНОМ ВРЕМЕНИ И ПАМЯТЬ РИГОВ",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onToggleSplitCompare,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.splitScreenLutCompare) NothingRed else NothingSurface)
                        .testTag("btn_split_lut_compare")
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Сравнить ДО/ПОСЛЕ",
                        tint = NothingWhite
                    )
                }
                IconButton(
                    onClick = onResetGrade,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingSurface)
                        .testTag("btn_reset_color_grade")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Сбросить грейдинг",
                        tint = NothingSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Color Calibration Chart Canvas Preview
        LutCalibrationPreviewCard(state = state)

        Spacer(modifier = Modifier.height(14.dp))

        // 1. 3D LUT Film Emulation Profiles
        Text(
            text = "КИНОПРОФИЛИ И ЭМУЛЯЦИЯ ПЛЁНКИ (3D LUT)",
            style = MaterialTheme.typography.labelLarge,
            color = NothingSilver
        )
        Spacer(modifier = Modifier.height(8.dp))

        LutProfile.entries.forEach { lut ->
            val selected = state.activeLut == lut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) NothingSurfaceHigh else NothingObsidian)
                    .border(
                        1.dp,
                        if (selected) Color(lut.accentHex) else NothingBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectLut(lut) }
                    .padding(12.dp)
                    .testTag("lut_card_${lut.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(lut.accentHex).copy(alpha = 0.22f))
                                .border(1.dp, Color(lut.accentHex), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = lut.code,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(lut.accentHex),
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = lut.nameRu,
                            style = MaterialTheme.typography.titleMedium,
                            color = NothingWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lut.subtitleRu,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NothingSilver,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Color Matrix Fine-Tuning Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingObsidian)
                .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "ТОНКАЯ НАСТРОЙКА МАТРИЦЫ ЦВЕТА",
                style = MaterialTheme.typography.labelLarge,
                color = ScopeCyan
            )
            Spacer(modifier = Modifier.height(8.dp))

            GradeSliderRow(
                label = "КОНТРАСТ",
                valueText = String.format(Locale.US, "%d%%", (state.userContrast * 100).toInt()),
                value = state.userContrast,
                range = 0.5f..1.5f,
                accent = NothingWhite,
                onValueChange = {
                    onUpdateGrade(it, state.userSaturation, state.shadowLift, state.highlightRollOff, state.filmGrainAmount)
                }
            )
            GradeSliderRow(
                label = "НАСЫЩЕННОСТЬ",
                valueText = String.format(Locale.US, "%d%%", (state.userSaturation * 100).toInt()),
                value = state.userSaturation,
                range = 0.0f..1.8f,
                accent = ScopeCyan,
                onValueChange = {
                    onUpdateGrade(state.userContrast, it, state.shadowLift, state.highlightRollOff, state.filmGrainAmount)
                }
            )
            GradeSliderRow(
                label = "ПОДЪЁМ ТЕНЕЙ (LIFT)",
                valueText = String.format(Locale.US, "%+.2f", state.shadowLift),
                value = state.shadowLift,
                range = -0.4f..0.4f,
                accent = ScopeGreen,
                onValueChange = {
                    onUpdateGrade(state.userContrast, state.userSaturation, it, state.highlightRollOff, state.filmGrainAmount)
                }
            )
            GradeSliderRow(
                label = "МЯГКИЕ СВЕТА (ROLL-OFF)",
                valueText = String.format(Locale.US, "%d%%", (state.highlightRollOff * 100).toInt()),
                value = state.highlightRollOff,
                range = 0.0f..1.0f,
                accent = ScopeAmber,
                onValueChange = {
                    onUpdateGrade(state.userContrast, state.userSaturation, state.shadowLift, it, state.filmGrainAmount)
                }
            )
            GradeSliderRow(
                label = "ПЛЁНОЧНОЕ ЗЕРНО 35MM",
                valueText = String.format(Locale.US, "%d%%", (state.filmGrainAmount * 100).toInt()),
                value = state.filmGrainAmount,
                range = 0.0f..1.0f,
                accent = NothingRed,
                onValueChange = {
                    onUpdateGrade(state.userContrast, state.userSaturation, state.shadowLift, state.highlightRollOff, it)
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Room-backed Custom Camera Rig Presets (C1, C2, C3, C4...)
        Text(
            text = "ПОЛЬЗОВАТЕЛЬСКИЕ ПРЕСЕТЫ КАМЕРЫ (ПАМЯТЬ C1–C9)",
            style = MaterialTheme.typography.labelLarge,
            color = NothingSilver
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
                        text = "Имя рига (напр. NIGHT ANAMORPHIC)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NothingSilver
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingRed,
                    unfocusedBorderColor = NothingBorder,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
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
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                modifier = Modifier.testTag("btn_save_preset")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("СОХР.", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        presets.forEach { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingObsidian)
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .clickable { onApplyPreset(preset) }
                    .padding(12.dp)
                    .testTag("preset_item_${preset.slotCode}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingRed.copy(alpha = 0.2f))
                            .border(1.dp, NothingRed, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.slotCode,
                            style = MaterialTheme.typography.titleMedium,
                            color = NothingWhite
                        )
                    }
                    Column {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = NothingWhite
                        )
                        Text(
                            text = "${if (preset.isVideoPreset) "CINE" else "PHOTO"} • ISO ${if (preset.isoValue == 0) "AUTO" else preset.isoValue} • ${preset.wbKelvin}K • ${preset.anamorphicLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NothingSilver,
                            fontSize = 10.sp
                        )
                    }
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

@Composable
private fun LutCalibrationPreviewCard(state: ProCameraUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NothingObsidian)
            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "КАЛИБРОВОЧНАЯ ШКАЛА // ${state.activeLut.nameRu}",
                style = MaterialTheme.typography.labelMedium,
                color = NothingWhite
            )
            Text(
                text = if (state.splitScreenLutCompare) "SPLIT WIPE ACTIVE" else "FULL LUT MATRIX",
                style = MaterialTheme.typography.labelMedium,
                color = ScopeAmber
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            val w = size.width
            val h = size.height
            // ColorChecker 8-patch cinema swatches + grayscale ramp
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
            // Bottom 12-stop luma ramp
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
    }
}

@Composable
private fun GradeSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = NothingSilver)
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = accent)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = NothingSurfaceHigh
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}
