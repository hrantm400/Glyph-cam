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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.example.model.GlyphLightingMode
import com.example.ui.components.CodeBadge
import com.example.ui.components.NothingCard
import com.example.ui.components.NothingSlider
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatusDot
import com.example.ui.components.StudioHeader
import com.example.ui.theme.GlyphLedWarm
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingSurfaceHigh
import com.example.ui.theme.NothingWhite
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
            .testTag("glyph_studio_screen")
    ) {
        StudioHeader(
            title = "Glyph-студия",
            subtitle = "NOTHING PHONE (2) · 11 СЕГМЕНТОВ · 33 ЗОНЫ",
            onBack = onBackToCamera,
            backTag = "glyph_back_btn"
        ) {
            CodeBadge(text = "${state.glyphBrightness}%", color = NothingRed)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left: interactive rear-panel schematic (tap a segment to toggle it)
                NothingPhone2GlyphSchematic(
                    activeSegments = state.activeGlyphSegments,
                    brightness = state.glyphBrightness,
                    isRecording = state.isRecording,
                    onTapSegment = onToggleSegment,
                    modifier = Modifier
                        .width(148.dp)
                        .height(304.dp)
                )

                // Right: brightness, LED flash link, manual segments
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NothingCard(contentPadding = PaddingValues(12.dp)) {
                        SectionLabel(
                            text = "Яркость",
                            trailing = "${state.glyphBrightness}%",
                            trailingColor = NothingWhite
                        )
                        NothingSlider(
                            value = state.glyphBrightness.toFloat(),
                            onValueChange = { onBrightnessChange(it.toInt()) },
                            valueRange = 0f..100f,
                            accent = NothingWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_glyph_brightness")
                        )
                        Text(
                            text = "${3300 + state.glyphBrightness * 12}K · мягкий свет",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingSilver
                        )
                    }

                    NothingCard(
                        onClick = onToggleTorchHardware,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("switch_glyph_torch")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LED-вспышка",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NothingWhite
                                )
                                Text(
                                    text = "Включать вместе с Glyph",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NothingSilver
                                )
                            }
                            Switch(
                                checked = state.glyphTorchHardwareSync,
                                onCheckedChange = { onToggleTorchHardware() },
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

                    NothingCard(contentPadding = PaddingValues(10.dp)) {
                        SectionLabel(
                            text = "Сегменты",
                            trailing = "${state.activeGlyphSegments.size}/11",
                            trailingColor = NothingWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val zoneLabels = listOf(
                            0 to "LENS L", 1 to "LENS R", 2 to "SLASH",
                            3 to "C-TOP L", 4 to "C-TOP R", 5 to "C-MID L",
                            6 to "C-MID R", 7 to "C-BOT L", 8 to "C-BOT R",
                            9 to "USB VU", 10 to "DOT REC"
                        )
                        zoneLabels.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowItems.forEach { (idx, lbl) ->
                                    val isOn = state.activeGlyphSegments.contains(idx)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isOn) NothingWhite else NothingSurface)
                                            .border(
                                                1.dp,
                                                if (isOn) NothingWhite else NothingBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onToggleSegment(idx) }
                                            .padding(vertical = 9.dp)
                                            .testTag("glyph_seg_$idx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOn) NothingBlack else NothingSilver,
                                            maxLines = 1
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(text = "Сценарии подсветки")
            Spacer(modifier = Modifier.height(8.dp))

            GlyphLightingMode.entries.forEach { mode ->
                val selected = state.glyphMode == mode
                NothingCard(
                    selected = selected,
                    accent = NothingRed,
                    onClick = { onSelectGlyphMode(mode) },
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("glyph_mode_${mode.name}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = mode.titleRu,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) NothingWhite else NothingSilver,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                CodeBadge(
                                    text = mode.code,
                                    color = if (selected) NothingRed else NothingSilver
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.subtitleRu,
                                style = MaterialTheme.typography.bodySmall,
                                color = NothingSilver
                            )
                        }
                        if (selected) {
                            Spacer(modifier = Modifier.width(12.dp))
                            StatusDot(color = NothingRed, size = 10.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NothingPhone2GlyphSchematic(
    activeSegments: Set<Int>,
    brightness: Int,
    isRecording: Boolean,
    onTapSegment: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaLum = (brightness / 100f).coerceIn(0.15f, 1.0f)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF121216))
            .border(1.dp, NothingBorder, RoundedCornerShape(26.dp))
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

        // Chassis outline
        drawRoundRect(
            color = NothingBorder.copy(alpha = 0.4f),
            topLeft = Offset(w * 0.08f, h * 0.04f),
            size = Size(w * 0.84f, h * 0.92f),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )

        // Dual camera pill (IMX890 + JN1)
        val camCenterX = w * 0.27f
        val camTopY = h * 0.14f
        drawRoundRect(
            color = NothingBlack,
            topLeft = Offset(w * 0.16f, h * 0.08f),
            size = Size(w * 0.22f, h * 0.20f),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )
        drawCircle(NothingBorder, radius = 10.dp.toPx(), center = Offset(camCenterX, camTopY))
        drawCircle(NothingBorder, radius = 10.dp.toPx(), center = Offset(camCenterX, camTopY + 26.dp.toPx()))

        fun segColor(idx: Int): Color {
            val active = activeSegments.contains(idx)
            return if (active) GlyphLedWarm.copy(alpha = alphaLum) else NothingBorder.copy(alpha = 0.45f)
        }
        val glowStroke = 5.dp.toPx()

        // Segments 0 & 1: camera pill arcs
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

        // Segment 2: top-right diagonal slash
        drawLine(
            color = segColor(2),
            start = Offset(w * 0.72f, h * 0.18f),
            end = Offset(w * 0.85f, h * 0.09f),
            strokeWidth = glowStroke,
            cap = StrokeCap.Round
        )

        // Segments 3..8: six-part broken C-ring
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

        // Segment 9: bottom USB-C VU strip + dot
        val vuX = w * 0.5f
        drawLine(
            color = segColor(9),
            start = Offset(vuX, h * 0.73f),
            end = Offset(vuX, h * 0.88f),
            strokeWidth = glowStroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = segColor(9),
            radius = 3.5.dp.toPx(),
            center = Offset(vuX, h * 0.92f)
        )

        // Segment 10: red tally LED
        val tallyActive = activeSegments.contains(10) || isRecording
        drawCircle(
            color = if (tallyActive) NothingRed else NothingBorder,
            radius = 5.5.dp.toPx(),
            center = Offset(w * 0.78f, h * 0.72f)
        )
    }
}
