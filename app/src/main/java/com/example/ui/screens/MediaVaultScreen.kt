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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CapturedShotEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaVaultScreen(
    shots: List<CapturedShotEntity>,
    inspectedShot: CapturedShotEntity?,
    onSelectShot: (CapturedShotEntity?) -> Unit,
    onDeleteShot: (Int) -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (inspectedShot != null) {
            onSelectShot(null)
        } else {
            onBackToCamera()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(16.dp)
            .testTag("media_vault_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (inspectedShot != null) onSelectShot(null) else onBackToCamera()
                    },
                    modifier = Modifier.testTag("vault_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = NothingWhite
                    )
                }
                Column {
                    Text(
                        text = "EXIF & ТЕЛЕМЕТРИЯ // АРХИВ",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NothingWhite
                    )
                    Text(
                        text = "ВСЕГО КАДРОВ И ДУБЛЕЙ: ${shots.size} • DNG RAW & 4K CINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Detailed EXIF + Histogram Inspector Card when a shot is selected
        val activeShot = inspectedShot ?: shots.firstOrNull()
        if (activeShot != null) {
            ExifTelemetryLoupeCard(shot = activeShot)
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
            text = "ЖУРНАЛ ОТНЯТЫХ ДУБЛЕЙ И КАДРОВ",
            style = MaterialTheme.typography.labelLarge,
            color = NothingSilver
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shots, key = { it.id }) { shot ->
                val isSelected = activeShot?.id == shot.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NothingSurfaceHigh else NothingObsidian)
                        .border(
                            1.dp,
                            if (isSelected) NothingRed else NothingBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectShot(shot) }
                        .padding(12.dp)
                        .testTag("vault_shot_${shot.id}"),
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (shot.isVideo) NothingRed.copy(alpha = 0.22f) else ScopeCyan.copy(alpha = 0.18f))
                                .border(
                                    1.dp,
                                    if (shot.isVideo) NothingRed else ScopeCyan,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (shot.isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (shot.isVideo) NothingRed else ScopeCyan
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = shot.titleCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NothingWhite
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NothingSurface)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = shot.formatBadge,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ScopeAmber,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Text(
                                text = "${shot.lensInfo} • ISO ${shot.isoUsed} • ${shot.shutterLabel} • ${shot.wbKelvin}K",
                                style = MaterialTheme.typography.labelMedium,
                                color = NothingSilver,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDeleteShot(shot.id) },
                        modifier = Modifier.testTag("delete_shot_${shot.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить кадр",
                            tint = NothingSilver
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExifTelemetryLoupeCard(shot: CapturedShotEntity) {
    val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(shot.timestamp))
    val histValues = shot.histogramSummary.split(",").mapNotNull { it.trim().toIntOrNull() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NothingObsidian)
            .border(1.dp, NothingRed.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("exif_inspector_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "МЕТАДАННЫЕ КАДРА // ${shot.titleCode}",
                    style = MaterialTheme.typography.labelLarge,
                    color = NothingRed
                )
                Text(
                    text = "${shot.lensInfo} • $dateFmt",
                    style = MaterialTheme.typography.labelMedium,
                    color = NothingSilver
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NothingSurfaceHigh)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = shot.lutApplied,
                    style = MaterialTheme.typography.labelLarge,
                    color = ScopeCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4x2 EXIF Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExifMetricCell("РАЗРЕШЕНИЕ", shot.resolutionLabel, Modifier.weight(1f))
            ExifMetricCell("ВЫДЕРЖКА", shot.shutterLabel, Modifier.weight(1f))
            ExifMetricCell("ISO / ДИАФР.", "ISO ${shot.isoUsed} ${shot.apertureLabel}", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExifMetricCell("БАЛАНС БЕЛОГО", "${shot.wbKelvin}K (EV ${String.format(Locale.US, "%+.1f", shot.evValue)})", Modifier.weight(1f))
            ExifMetricCell("ДИНАМ. ДИАПАЗОН", "${shot.dynamicRangeStops} STOPS (${shot.averageIre} IRE)", Modifier.weight(1f))
            ExifMetricCell(
                if (shot.isVideo) "ПОТОК / ЗВУК" else "GLYPH СВЕТ",
                if (shot.isVideo) "${shot.bitrateMbps}M / ${String.format(Locale.US, "%.1fdB", shot.audioPeakDb)}" else shot.glyphModeUsed,
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "ОТik-СПЕКТР ЯРКОСТИ КАДРА (16-BIN IRE SIGNATURE)",
            style = MaterialTheme.typography.labelMedium,
            color = NothingSilver,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NothingBlack)
                .padding(4.dp)
        ) {
            if (histValues.isEmpty()) return@Canvas
            val barW = size.width / histValues.size
            histValues.forEachIndexed { idx, v ->
                val hNorm = (v.coerceIn(2, 100) / 100f) * size.height
                drawRect(
                    color = if (idx >= 14) NothingRed else if (idx in 6..10) ScopeGreen else NothingWhite.copy(alpha = 0.75f),
                    topLeft = Offset(idx * barW + 2f, size.height - hNorm),
                    size = Size((barW - 4f).coerceAtLeast(2f), hNorm)
                )
            }
        }
    }
}

@Composable
private fun ExifMetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NothingSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = NothingSilver, fontSize = 8.sp)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = NothingWhite, fontSize = 10.sp)
    }
}
