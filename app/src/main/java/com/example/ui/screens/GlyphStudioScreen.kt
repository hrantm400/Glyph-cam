package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlyphLightingMode
import com.example.ui.theme.GlyphLedWarm
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingObsidian
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingSurfaceHigh
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.ScopeAmber
import com.example.ui.theme.ScopeGreen
import com.example.viewmodel.ProCameraUiState

@Composable
fun GlyphStudioScreen(
    state: ProCameraUiState,
    onSelectGlyphMode: (GlyphLightingMode) -> Unit,
    onToggleSegment: (Int) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onToggleTorchHardware: () -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToCamera)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("glyph_studio_screen")
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
                    modifier = Modifier.testTag("glyph_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад к камере",
                        tint = NothingWhite
                    )
                }
                Column {
                    Text(
                        text = "GLYPH STUDIO // NOTHING (2)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NothingWhite
                    )
                    Text(
                        text = "11 LED СЕГМЕНТОВ • 33 АДРЕСУЕМЫЕ ЗОНЫ ПОДСВЕТКИ",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NothingRed.copy(alpha = 0.2f))
                    .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${state.glyphBrightness}% LUM",
                    style = MaterialTheme.typography.labelLarge,
                    color = NothingRed
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Nothing Phone (2) Rear Chassis Schematic + Zone Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Interactive Nothing Phone (2) Rear Glyph Schematic
            NothingPhone2GlyphSchematic(
                activeSegments = state.activeGlyphSegments,
                brightness = state.glyphBrightness,
                audioDb = state.peakAudioDb,
                isRecording = state.isRecording,
                onTapSegment = onToggleSegment,
                modifier = Modifier
                    .width(175.dp)
                    .height(330.dp)
            )

            // Right: Dimmer, Torch Link, and Quick Zone Toggles
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingObsidian)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "ЯРКОСТЬ GLYPH МАТРИЦЫ",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver
                    )
                    Text(
                        text = "${state.glyphBrightness}% (${3300 + state.glyphBrightness * 12}K SOFT)",
                        style = MaterialTheme.typography.titleMedium,
                        color = NothingWhite
                    )
                    Slider(
                        value = state.glyphBrightness.toFloat(),
                        onValueChange = { onBrightnessChange(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NothingWhite,
                            activeTrackColor = NothingRed,
                            inactiveTrackColor = NothingSurfaceHigh
                        ),
                        modifier = Modifier.testTag("slider_glyph_brightness")
                    )
                }

                // Hardware Torch Synchronization Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingObsidian)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .clickable { onToggleTorchHardware() }
                        .padding(12.dp)
                        .testTag("switch_glyph_torch"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "СИНХР. СО СВЕТОДИОДОМ КАМЕРЫ",
                            style = MaterialTheme.typography.labelLarge,
                            color = NothingWhite,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Включает аппаратный заполняющий свет вместе с Glyph",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NothingSilver,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = state.glyphTorchHardwareSync,
                        onCheckedChange = { onToggleTorchHardware() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingWhite,
                            checkedTrackColor = NothingRed
                        )
                    )
                }

                // Individual Zone Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingObsidian)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "РУЧНОЙ ВЫБОР 11 СЕГМЕНТОВ",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val zoneLabels = listOf(
                        0 to "LENS L", 1 to "LENS R", 2 to "SLASH /",
                        3 to "C-TOP L", 4 to "C-TOP R", 5 to "C-MID L",
                        6 to "C-MID R", 7 to "C-BOT L", 8 to "C-BOT R",
                        9 to "USB VU", 10 to "DOT REC"
                    )
                    for (rowIdx in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val slice = zoneLabels.drop(rowIdx * 3).take(3)
                            slice.forEach { (idx, lbl) ->
                                val isOn = state.activeGlyphSegments.contains(idx)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isOn) NothingWhite else NothingSurface)
                                        .border(
                                            1.dp,
                                            if (isOn) NothingWhite else NothingBorder,
                                            RoundedCornerShape(5.dp)
                                        )
                                        .clickable { onToggleSegment(idx) }
                                        .padding(vertical = 6.dp)
                                        .testTag("glyph_seg_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lbl,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isOn) NothingBlack else NothingSilver,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "СТУДИЙНЫЕ СЦЕНАРИИ ПОДСВЕТКИ NOTHING (2)",
            style = MaterialTheme.typography.labelLarge,
            color = NothingSilver
        )
        Spacer(modifier = Modifier.height(8.dp))

        GlyphLightingMode.entries.forEach { mode ->
            val selected = state.glyphMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) NothingSurfaceHigh else NothingObsidian)
                    .border(
                        1.dp,
                        if (selected) NothingRed else NothingBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectGlyphMode(mode) }
                    .padding(12.dp)
                    .testTag("glyph_mode_${mode.name}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = mode.titleRu,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) NothingWhite else NothingSilver
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selected) NothingRed else NothingSurface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = mode.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = NothingWhite,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = mode.subtitleRu,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NothingSilver,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NothingPhone2GlyphSchematic(
    activeSegments: Set<Int>,
    brightness: Int,
    audioDb: Float,
    isRecording: Boolean,
    onTapSegment: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaLum = (brightness / 100f).coerceIn(0.15f, 1.0f)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF121216))
            .border(2.dp, NothingBorder, RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val yNorm = offset.y / size.height
                    val xNorm = offset.x / size.width
                    val tappedIdx = when {
                        yNorm < 0.28f && xNorm < 0.5f -> 0
                        yNorm < 0.28f && xNorm >= 0.5f -> 2
                        yNorm in 0.28f..0.45f && xNorm < 0.5f -> 3
                        yNorm in 0.28f..0.45f && xNorm >= 0.5f -> 4
                        yNorm in 0.45f..0.60f && xNorm < 0.5f -> 5
                        yNorm in 0.45f..0.60f && xNorm >= 0.5f -> 6
                        yNorm in 0.60f..0.72f -> 7
                        yNorm > 0.72f && xNorm > 0.68f -> 10
                        else -> 9
                    }
                    onTapSegment(tappedIdx)
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Transparent engineering chassis lines
        drawRoundRect(
            color = NothingBorder.copy(alpha = 0.4f),
            topLeft = Offset(w * 0.08f, h * 0.04f),
            size = Size(w * 0.84f, h * 0.92f),
            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )

        // Dual Camera Pill (IMX890 + JN1)
        val camCenterX = w * 0.27f
        val camTopY = h * 0.14f
        drawRoundRect(
            color = NothingBlack,
            topLeft = Offset(w * 0.16f, h * 0.08f),
            size = Size(w * 0.22f, h * 0.20f),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )
        drawCircle(NothingBorder, radius = 12.dp.toPx(), center = Offset(camCenterX, camTopY))
        drawCircle(NothingBorder, radius = 12.dp.toPx(), center = Offset(camCenterX, camTopY + 30.dp.toPx()))

        fun segColor(idx: Int): Color {
            val active = activeSegments.contains(idx)
            return if (active) GlyphLedWarm.copy(alpha = alphaLum) else NothingBorder.copy(alpha = 0.45f)
        }
        val glowStroke = 6.dp.toPx()

        // Segment 0 & 1: Camera Pill Left & Right Arcs
        drawArc(
            color = segColor(0),
            startAngle = 110f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.13f, h * 0.065f),
            size = Size(w * 0.28f, h * 0.23f),
            style = Stroke(width = glowStroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = segColor(1),
            startAngle = 290f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.13f, h * 0.065f),
            size = Size(w * 0.28f, h * 0.23f),
            style = Stroke(width = glowStroke, cap = StrokeCap.Round)
        )

        // Segment 2: Top-Right Diagonal Nothing Slash
        drawLine(
            color = segColor(2),
            start = Offset(w * 0.72f, h * 0.18f),
            end = Offset(w * 0.85f, h * 0.09f),
            strokeWidth = glowStroke,
            cap = StrokeCap.Round
        )

        // Segments 3..8: 6-Part Broken Center Wireless Charging C-Ring (Nothing Phone 2 signature!)
        val cRingTopLeft = Offset(w * 0.20f, h * 0.33f)
        val cRingSize = Size(w * 0.60f, h * 0.33f)
        val arcDefs = listOf(
            3 to Pair(195f, 45f),
            4 to Pair(255f, 70f),
            5 to Pair(135f, 45f),
            6 to Pair(340f, 50f),
            7 to Pair(75f, 45f),
            8 to Pair(15f, 45f)
        )
        arcDefs.forEach { (idx, angles) ->
            drawArc(
                color = segColor(idx),
                startAngle = angles.first,
                sweepAngle = angles.second,
                useCenter = false,
                topLeft = cRingTopLeft,
                size = cRingSize,
                style = Stroke(width = glowStroke, cap = StrokeCap.Round)
            )
        }

        // Segment 9: Bottom USB-C Vertical VU / Timer Strip + Dot
        val vuX = w * 0.5f
        val vuTop = h * 0.73f
        val vuBottom = h * 0.88f
        drawLine(
            color = segColor(9),
            start = Offset(vuX, vuTop),
            end = Offset(vuX, vuBottom),
            strokeWidth = glowStroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = segColor(9),
            radius = 3.5.dp.toPx(),
            center = Offset(vuX, h * 0.92f)
        )

        // Segment 10: Red Tally Recording Indicator LED on Right
        val tallyActive = activeSegments.contains(10) || isRecording
        drawCircle(
            color = if (tallyActive) NothingRed else NothingBorder,
            radius = 5.5.dp.toPx(),
            center = Offset(w * 0.78f, h * 0.72f)
        )
    }
}
