package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.FocusPeakingColor
import com.example.model.FrameAspectGuide
import com.example.model.GridOverlayMode
import com.example.model.LiveScopeTelemetry
import com.example.model.ZebraThreshold
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingObsidian
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.ScopeAmber
import com.example.ui.theme.ScopeBlue
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.ScopeGreen
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ViewfinderOpticalOverlays(
    telemetry: LiveScopeTelemetry,
    gridMode: GridOverlayMode,
    aspectGuide: FrameAspectGuide,
    maskOpacity: Float,
    zebraThreshold: ZebraThreshold,
    peakingColor: FocusPeakingColor,
    showFalseColor: Boolean,
    showHorizon: Boolean,
    rollDegrees: Float,
    pitchDegrees: Float,
    tapFocusPoint: Pair<Float, Float>?,
    aeAfLocked: Boolean,
    splitLutActive: Boolean,
    lutName: String,
    filmGrainAmount: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. False Color IRE Exposure Map
        if (showFalseColor) {
            drawFalseColorMap(telemetry.falseColorGrid, w, h)
        }

        // 2. Zebra Highlight Warning Stripes
        if (zebraThreshold != ZebraThreshold.OFF) {
            drawZebraOverlay(telemetry.zebraGrid, w, h)
        }

        // 3. Focus Peaking Edge Contours
        if (peakingColor != FocusPeakingColor.OFF) {
            drawFocusPeakingOverlay(telemetry.peakingGrid, peakingColor.color, w, h)
        }

        // 4. Split-screen LUT comparison divider
        if (splitLutActive) {
            val midX = w * 0.5f
            drawLine(
                color = NothingWhite.copy(alpha = 0.85f),
                start = Offset(midX, 0f),
                end = Offset(midX, h),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
            drawCircle(
                color = NothingRed,
                radius = 6.dp.toPx(),
                center = Offset(midX, h * 0.5f)
            )
        }

        // 5. Cinema Aspect Ratio Framing Guides & Safe Areas
        aspectGuide.ratio?.let { targetRatio ->
            drawAspectMaskAndSafeZones(targetRatio, maskOpacity, w, h)
        }

        // 6. Composition Grid Overlays
        drawCompositionGrid(gridMode, w, h)

        // 7. Dual-Axis Gyroscope Horizon Leveler
        if (showHorizon) {
            drawGyroHorizonLeveler(rollDegrees, pitchDegrees, w, h)
        }

        // 8. Tap-to-Focus & AE/AF Lock Bracket
        tapFocusPoint?.let { (nx, ny) ->
            drawFocusReticle(
                cx = nx * w,
                cy = ny * h,
                locked = aeAfLocked
            )
        }
    }
}

private fun DrawScope.drawFalseColorMap(grid: Array<IntArray>, w: Float, h: Float) {
    val rows = grid.size
    val cols = if (rows > 0) grid[0].size else 0
    if (rows == 0 || cols == 0) return
    val cellW = w / cols
    val cellH = h / rows

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val ire = grid[r][c]
            val ireColor = when {
                ire < 8 -> Color(0xAA651FFF)     // Deep Purple: Shadow Crush
                ire < 22 -> Color(0x882979FF)    // Blue: Deep Shadows
                ire in 38..48 -> Color(0x9900E676) // Green: 18% Middle Gray
                ire in 52..62 -> Color(0x99FF80AB) // Pink: Caucasian / Warm Skin Tone IRE
                ire in 78..90 -> Color(0xAAFFB800) // Yellow: High Key
                ire > 90 -> Color(0xCCFF1744)    // Red: Highlight Clipping
                else -> Color.Transparent
            }
            if (ireColor != Color.Transparent) {
                drawRect(
                    color = ireColor,
                    topLeft = Offset(c * cellW, r * cellH),
                    size = Size(cellW + 1f, cellH + 1f)
                )
            }
        }
    }
}

private fun DrawScope.drawZebraOverlay(zebraGrid: Array<BooleanArray>, w: Float, h: Float) {
    val rows = zebraGrid.size
    val cols = if (rows > 0) zebraGrid[0].size else 0
    if (rows == 0 || cols == 0) return
    val cellW = w / cols
    val cellH = h / rows
    val stripeColor = ScopeAmber.copy(alpha = 0.72f)

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (zebraGrid[r][c]) {
                val x0 = c * cellW
                val y0 = r * cellH
                val step = cellW / 3f
                for (s in 0..2) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(x0 + s * step, y0 + cellH),
                        end = Offset((x0 + (s + 1) * step).coerceAtMost(x0 + cellW), y0),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFocusPeakingOverlay(
    peakingGrid: Array<FloatArray>,
    color: Color,
    w: Float,
    h: Float
) {
    val rows = peakingGrid.size
    val cols = if (rows > 0) peakingGrid[0].size else 0
    if (rows == 0 || cols == 0) return
    val cellW = w / cols
    val cellH = h / rows

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val strength = peakingGrid[r][c]
            if (strength > 0.42f) {
                val cx = (c + 0.5f) * cellW
                val cy = (r + 0.5f) * cellH
                drawCircle(
                    color = color.copy(alpha = (strength * 0.9f).coerceIn(0.3f, 0.95f)),
                    radius = (strength * 2.4f).dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

private fun DrawScope.drawAspectMaskAndSafeZones(
    targetRatio: Float,
    maskOpacity: Float,
    w: Float,
    h: Float
) {
    val currentRatio = w / h.coerceAtLeast(1f)
    val maskColor = Color.Black.copy(alpha = maskOpacity.coerceIn(0.2f, 0.9f))
    var frameLeft = 0f
    var frameTop = 0f
    var frameW = w
    var frameH = h

    if (targetRatio > currentRatio) {
        // Letterbox top & bottom
        frameH = w / targetRatio
        frameTop = (h - frameH) * 0.5f
        drawRect(maskColor, Offset(0f, 0f), Size(w, frameTop))
        drawRect(maskColor, Offset(0f, frameTop + frameH), Size(w, frameTop))
    } else {
        // Pillarbox left & right
        frameW = h * targetRatio
        frameLeft = (w - frameW) * 0.5f
        drawRect(maskColor, Offset(0f, 0f), Size(frameLeft, h))
        drawRect(maskColor, Offset(frameLeft + frameW, 0f), Size(frameLeft, h))
    }

    // Active Cinema Gate Frame Border
    drawRect(
        color = NothingWhite.copy(alpha = 0.55f),
        topLeft = Offset(frameLeft, frameTop),
        size = Size(frameW, frameH),
        style = Stroke(width = 1.dp.toPx())
    )

    // 90% Broadcast Action Safe Zone dashed box
    val safeMarginX = frameW * 0.05f
    val safeMarginY = frameH * 0.05f
    drawRect(
        color = NothingSilver.copy(alpha = 0.28f),
        topLeft = Offset(frameLeft + safeMarginX, frameTop + safeMarginY),
        size = Size(frameW - safeMarginX * 2, frameH - safeMarginY * 2),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )
    )
}

private fun DrawScope.drawCompositionGrid(mode: GridOverlayMode, w: Float, h: Float) {
    val lineColor = NothingWhite.copy(alpha = 0.22f)
    val strokePx = 1.dp.toPx()

    when (mode) {
        GridOverlayMode.NONE -> Unit
        GridOverlayMode.RULE_OF_THIRDS -> {
            for (i in 1..2) {
                val x = w * (i / 3f)
                val y = h * (i / 3f)
                drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokePx)
                drawLine(lineColor, Offset(0f, y), Offset(w, y), strokePx)
            }
        }
        GridOverlayMode.GOLDEN_RATIO -> {
            val phi1 = 0.382f
            val phi2 = 0.618f
            listOf(phi1, phi2).forEach { p ->
                drawLine(ScopeAmber.copy(alpha = 0.32f), Offset(w * p, 0f), Offset(w * p, h), strokePx)
                drawLine(ScopeAmber.copy(alpha = 0.32f), Offset(0f, h * p), Offset(w, h * p), strokePx)
            }
        }
        GridOverlayMode.DIAGONAL_CROSS -> {
            drawLine(lineColor, Offset(0f, 0f), Offset(w, h), strokePx)
            drawLine(lineColor, Offset(w, 0f), Offset(0f, h), strokePx)
            drawLine(lineColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokePx)
            drawLine(lineColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokePx)
        }
        GridOverlayMode.CINE_CROSSHAIR -> {
            val cx = w * 0.5f
            val cy = h * 0.5f
            val arm = 24.dp.toPx()
            val gap = 6.dp.toPx()
            drawLine(NothingWhite.copy(alpha = 0.6f), Offset(cx - arm, cy), Offset(cx - gap, cy), 1.5.dp.toPx())
            drawLine(NothingWhite.copy(alpha = 0.6f), Offset(cx + gap, cy), Offset(cx + arm, cy), 1.5.dp.toPx())
            drawLine(NothingWhite.copy(alpha = 0.6f), Offset(cx, cy - arm), Offset(cx, cy - gap), 1.5.dp.toPx())
            drawLine(NothingWhite.copy(alpha = 0.6f), Offset(cx, cy + gap), Offset(cx, cy + arm), 1.5.dp.toPx())
            drawCircle(NothingRed, radius = 2.dp.toPx(), center = Offset(cx, cy))
        }
    }
}

private fun DrawScope.drawGyroHorizonLeveler(
    rollDegrees: Float,
    pitchDegrees: Float,
    w: Float,
    h: Float
) {
    val cx = w * 0.5f
    val cy = h * 0.5f
    val isLevel = abs(rollDegrees) < 1.0f && abs(pitchDegrees) < 2.5f
    val activeColor = if (isLevel) ScopeGreen else NothingWhite.copy(alpha = 0.65f)
    val wingSpan = 56.dp.toPx()
    val innerGap = 18.dp.toPx()

    // Fixed reference ticks
    drawLine(
        color = NothingWhite.copy(alpha = 0.35f),
        start = Offset(cx - wingSpan - 12.dp.toPx(), cy),
        end = Offset(cx - wingSpan - 4.dp.toPx(), cy),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = NothingWhite.copy(alpha = 0.35f),
        start = Offset(cx + wingSpan + 4.dp.toPx(), cy),
        end = Offset(cx + wingSpan + 12.dp.toPx(), cy),
        strokeWidth = 2.dp.toPx()
    )

    val pitchOffsetPx = (pitchDegrees * 1.4f).coerceIn(-36f, 36f)
    rotate(degrees = -rollDegrees, pivot = Offset(cx, cy)) {
        drawLine(
            color = activeColor,
            start = Offset(cx - wingSpan, cy + pitchOffsetPx),
            end = Offset(cx - innerGap, cy + pitchOffsetPx),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = activeColor,
            start = Offset(cx + innerGap, cy + pitchOffsetPx),
            end = Offset(cx + wingSpan, cy + pitchOffsetPx),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFocusReticle(cx: Float, cy: Float, locked: Boolean) {
    val boxSize = 58.dp.toPx()
    val half = boxSize * 0.5f
    val corner = 12.dp.toPx()
    val color = if (locked) ScopeAmber else NothingRed
    val stroke = 2.dp.toPx()

    val left = (cx - half).coerceIn(8f, size.width - boxSize - 8f)
    val top = (cy - half).coerceIn(8f, size.height - boxSize - 8f)
    val right = left + boxSize
    val bottom = top + boxSize

    // Top-left corner
    drawLine(color, Offset(left, top), Offset(left + corner, top), stroke)
    drawLine(color, Offset(left, top), Offset(left, top + corner), stroke)
    // Top-right corner
    drawLine(color, Offset(right, top), Offset(right - corner, top), stroke)
    drawLine(color, Offset(right, top), Offset(right, top + corner), stroke)
    // Bottom-left corner
    drawLine(color, Offset(left, bottom), Offset(left + corner, bottom), stroke)
    drawLine(color, Offset(left, bottom), Offset(left, bottom - corner), stroke)
    // Bottom-right corner
    drawLine(color, Offset(right, bottom), Offset(right - corner, bottom), stroke)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - corner), stroke)

    drawCircle(color, radius = 2.5.dp.toPx(), center = Offset(left + half, top + half))
}

// ---------------------------------------------------------------------------------------------
// Floating scopes dock (top of the viewfinder)
// ---------------------------------------------------------------------------------------------

@Composable
fun FloatingScopesHudDock(
    telemetry: LiveScopeTelemetry,
    showHistogram: Boolean,
    showWaveform: Boolean,
    showVectorscope: Boolean,
    showAudio: Boolean,
    leftAudioDb: Float,
    rightAudioDb: Float,
    peakAudioDb: Float,
    onToggleHistogram: () -> Unit,
    onToggleWaveform: () -> Unit,
    onToggleVectorscope: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showHistogram && !showWaveform && !showVectorscope && !showAudio) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left: scopes scroll if they do not fit; the audio meter stays pinned on the right
        FadingScrollRow(
            modifier = Modifier.weight(1f),
            spacing = 8.dp,
            verticalAlignment = Alignment.Top,
            fade = false
        ) {
            if (showHistogram) {
                RgbHistogramCard(telemetry = telemetry, onClick = onToggleHistogram)
            }
            if (showWaveform) {
                LumaWaveformCard(telemetry = telemetry, onClick = onToggleWaveform)
            }
            if (showVectorscope) {
                ChromaVectorscopeCard(telemetry = telemetry, onClick = onToggleVectorscope)
            }
        }
        if (showAudio) {
            Spacer(modifier = Modifier.width(8.dp))
            StereoAudioMeterCard(
                leftDb = leftAudioDb,
                rightDb = rightAudioDb,
                peakDb = peakAudioDb
            )
        }
    }
}

@Composable
private fun ScopeCard(
    width: Dp,
    tag: String,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Column(
        modifier = Modifier
            .width(width)
            .clip(shape)
            .background(NothingBlack.copy(alpha = 0.82f))
            .border(1.dp, NothingBorder.copy(alpha = 0.7f), shape)
            .then(clickModifier)
            .padding(8.dp)
            .testTag(tag)
    ) {
        content()
    }
}

@Composable
private fun ScopeHeader(title: String, titleColor: Color, trailing: String?, trailingColor: Color = NothingWhite) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = titleColor, maxLines = 1)
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.labelSmall, color = trailingColor, maxLines = 1)
        }
    }
}

@Composable
private fun RgbHistogramCard(
    telemetry: LiveScopeTelemetry,
    onClick: () -> Unit
) {
    ScopeCard(width = 120.dp, tag = "scope_rgb_histogram", onClick = onClick) {
        ScopeHeader(title = "HIST", titleColor = NothingSilver, trailing = "${telemetry.averageIre} IRE")
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val w = size.width
            val h = size.height
            val bins = telemetry.lumaHistogram.size
            if (bins < 2) return@Canvas

            fun buildCurve(data: IntArray): Path {
                val path = Path()
                path.moveTo(0f, h)
                for (i in 0 until bins) {
                    val x = (i / (bins - 1f)) * w
                    val y = h - (data[i].coerceIn(0, 100) / 100f) * h
                    path.lineTo(x, y)
                }
                path.lineTo(w, h)
                path.close()
                return path
            }

            drawPath(buildCurve(telemetry.redHistogram), NothingRed.copy(alpha = 0.35f))
            drawPath(buildCurve(telemetry.greenHistogram), ScopeGreen.copy(alpha = 0.32f))
            drawPath(buildCurve(telemetry.blueHistogram), ScopeBlue.copy(alpha = 0.35f))

            for (i in 0 until bins - 1) {
                val x1 = (i / (bins - 1f)) * w
                val y1 = h - (telemetry.lumaHistogram[i] / 100f) * h
                val x2 = ((i + 1) / (bins - 1f)) * w
                val y2 = h - (telemetry.lumaHistogram[i + 1] / 100f) * h
                drawLine(NothingWhite.copy(alpha = 0.9f), Offset(x1, y1), Offset(x2, y2), 1.2.dp.toPx())
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DR ${String.format(Locale.US, "%.1f", telemetry.dynamicRangeStops)}",
                style = MaterialTheme.typography.labelSmall,
                color = ScopeGreen,
                maxLines = 1
            )
            Text(
                text = "CLIP ${String.format(Locale.US, "%.1f%%", telemetry.highlightClipPercent)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (telemetry.highlightClipPercent > 3f) NothingRed else NothingSilver,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LumaWaveformCard(
    telemetry: LiveScopeTelemetry,
    onClick: () -> Unit
) {
    ScopeCard(width = 104.dp, tag = "scope_luma_waveform", onClick = onClick) {
        ScopeHeader(title = "WFM", titleColor = ScopeGreen, trailing = "IRE", trailingColor = NothingSilver)
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val w = size.width
            val h = size.height
            listOf(0.05f, 0.25f, 0.5f, 0.75f).forEach { yNorm ->
                drawLine(
                    color = NothingBorder.copy(alpha = 0.7f),
                    start = Offset(0f, h * yNorm),
                    end = Offset(w, h * yNorm),
                    strokeWidth = 0.8.dp.toPx()
                )
            }

            val cols = telemetry.waveformColumns
            for (i in cols.indices) {
                val x = (i / (cols.size - 1f).coerceAtLeast(1f)) * w
                val v = cols[i].coerceIn(0.05f, 0.98f)
                val centerY = h * (1f - v)
                val spread = h * 0.18f
                drawLine(
                    color = ScopeGreen.copy(alpha = 0.42f),
                    start = Offset(x, (centerY - spread).coerceAtLeast(0f)),
                    end = Offset(x, (centerY + spread).coerceAtMost(h)),
                    strokeWidth = 1.6.dp.toPx()
                )
                drawCircle(
                    color = ScopeGreen,
                    radius = 1.3.dp.toPx(),
                    center = Offset(x, centerY)
                )
            }
        }
    }
}

@Composable
private fun ChromaVectorscopeCard(
    telemetry: LiveScopeTelemetry,
    onClick: () -> Unit
) {
    ScopeCard(width = 78.dp, tag = "scope_vectorscope", onClick = onClick) {
        ScopeHeader(title = "VEC", titleColor = ScopeCyan, trailing = null)
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .size(54.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            val r = size.minDimension * 0.48f
            val c = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(NothingBorder, radius = r, center = c, style = Stroke(1.dp.toPx()))
            drawCircle(NothingBorder.copy(alpha = 0.5f), radius = r * 0.5f, center = c, style = Stroke(0.8.dp.toPx()))

            val skinRad = Math.toRadians(-123.0)
            drawLine(
                color = ScopeAmber.copy(alpha = 0.65f),
                start = c,
                end = Offset(
                    c.x + (cos(skinRad) * r).toFloat(),
                    c.y + (sin(skinRad) * r).toFloat()
                ),
                strokeWidth = 1.2.dp.toPx()
            )

            telemetry.vectorscopePoints.forEach { (u, v) ->
                val px = c.x + u * r * 0.85f
                val py = c.y - v * r * 0.85f
                drawCircle(
                    color = ScopeCyan.copy(alpha = 0.75f),
                    radius = 1.6.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}

@Composable
private fun StereoAudioMeterCard(
    leftDb: Float,
    rightDb: Float,
    peakDb: Float
) {
    ScopeCard(width = 100.dp, tag = "audio_meter_card") {
        ScopeHeader(
            title = "AUDIO",
            titleColor = NothingSilver,
            trailing = String.format(Locale.US, "%.0f dB", peakDb),
            trailingColor = if (peakDb > -3f) NothingRed else ScopeGreen
        )
        Spacer(modifier = Modifier.height(6.dp))
        AudioChannelBar(channelLabel = "L", dbValue = leftDb)
        Spacer(modifier = Modifier.height(4.dp))
        AudioChannelBar(channelLabel = "R", dbValue = rightDb)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-48", style = MaterialTheme.typography.labelSmall, color = NothingSilver)
            Text("-12", style = MaterialTheme.typography.labelSmall, color = ScopeAmber)
            Text("0", style = MaterialTheme.typography.labelSmall, color = NothingRed)
        }
    }
}

@Composable
private fun AudioChannelBar(channelLabel: String, dbValue: Float) {
    val norm = ((dbValue + 54f) / 54f).coerceIn(0.04f, 1.0f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = channelLabel,
            style = MaterialTheme.typography.labelSmall,
            color = NothingWhite,
            modifier = Modifier.width(14.dp)
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NothingObsidian)
        ) {
            val segments = 16
            val segW = size.width / segments
            val activeSegs = (norm * segments).toInt()
            for (i in 0 until segments) {
                val segColor = when {
                    i >= 14 -> NothingRed
                    i >= 11 -> ScopeAmber
                    else -> ScopeGreen
                }
                drawRect(
                    color = if (i < activeSegs) segColor else segColor.copy(alpha = 0.14f),
                    topLeft = Offset(i * segW + 1f, 0f),
                    size = Size((segW - 2f).coerceAtLeast(1f), size.height)
                )
            }
        }
    }
}
