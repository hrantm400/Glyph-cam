package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CapturedShotEntity
import com.example.ui.components.CodeBadge
import com.example.ui.components.NothingCard
import com.example.ui.components.SectionLabel
import com.example.ui.components.StudioHeader
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingSurface
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
    val goBack: () -> Unit = {
        if (inspectedShot != null) onSelectShot(null) else onBackToCamera()
    }
    BackHandler(onBack = goBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .testTag("media_vault_screen")
    ) {
        StudioHeader(
            title = "Архив",
            subtitle = "${shots.size} ${pluralShots(shots.size)} · DNG RAW · 4K CINE",
            onBack = goBack,
            backTag = "vault_back_btn"
        )

        if (shots.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = NothingSilver,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Пока нет кадров",
                    style = MaterialTheme.typography.titleMedium,
                    color = NothingWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Снимки и дубли появятся здесь вместе с их EXIF-телеметрией.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NothingSilver,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val activeShot = inspectedShot ?: shots.first()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "loupe") {
                ExifTelemetryLoupeCard(shot = activeShot)
                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel(text = "Снятые кадры и дубли")
            }
            items(shots, key = { it.id }) { shot ->
                ShotRow(
                    shot = shot,
                    selected = activeShot.id == shot.id,
                    onSelect = { onSelectShot(shot) },
                    onDelete = { onDeleteShot(shot.id) }
                )
            }
        }
    }
}

private fun pluralShots(count: Int): String {
    val n = count % 100
    val n1 = count % 10
    return when {
        n in 11..19 -> "КАДРОВ"
        n1 == 1 -> "КАДР"
        n1 in 2..4 -> "КАДРА"
        else -> "КАДРОВ"
    }
}

@Composable
private fun ShotRow(
    shot: CapturedShotEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    NothingCard(
        selected = selected,
        accent = NothingRed,
        onClick = onSelect,
        contentPadding = PaddingValues(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_shot_${shot.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = if (shot.isVideo) NothingRed else ScopeCyan
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.16f))
                    .border(1.dp, tint.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (shot.isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = shot.titleCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = NothingWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    CodeBadge(text = shot.formatBadge, color = ScopeAmber)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${shot.lensInfo} · ISO ${shot.isoUsed} · ${shot.shutterLabel} · ${shot.wbKelvin}K",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDelete,
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

@Composable
private fun ExifTelemetryLoupeCard(shot: CapturedShotEntity) {
    val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(shot.timestamp))
    val histValues = shot.histogramSummary.split(",").mapNotNull { it.trim().toIntOrNull() }

    NothingCard(
        selected = true,
        accent = NothingRed.copy(alpha = 0.7f),
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exif_inspector_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shot.titleCode,
                    style = MaterialTheme.typography.labelLarge,
                    color = NothingWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${shot.lensInfo} · $dateFmt",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            CodeBadge(text = shot.lutApplied, color = ScopeCyan)
        }

        Spacer(modifier = Modifier.height(12.dp))

        val resolutionMain = shot.resolutionLabel.substringBefore(" (")
        val resolutionSub = shot.resolutionLabel.substringAfter(" (", "").removeSuffix(")").ifBlank { null }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExifMetricCell("РАЗРЕШЕНИЕ", resolutionMain, resolutionSub, Modifier.weight(1f))
            ExifMetricCell("ВЫДЕРЖКА", shot.shutterLabel, null, Modifier.weight(1f))
            ExifMetricCell("ISO · ДИАФР.", "ISO ${shot.isoUsed}", shot.apertureLabel, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExifMetricCell(
                "БАЛАНС БЕЛОГО",
                "${shot.wbKelvin}K",
                String.format(Locale.US, "EV %+.1f", shot.evValue),
                Modifier.weight(1f)
            )
            ExifMetricCell(
                "ДИАПАЗОН",
                String.format(Locale.US, "%.1f STOPS", shot.dynamicRangeStops),
                "${shot.averageIre} IRE",
                Modifier.weight(1f)
            )
            if (shot.isVideo) {
                ExifMetricCell(
                    "ПОТОК · ЗВУК",
                    "${shot.bitrateMbps} Mbps",
                    String.format(Locale.US, "%.1f dB", shot.audioPeakDb),
                    Modifier.weight(1f)
                )
            } else {
                ExifMetricCell("GLYPH", shot.glyphModeUsed, null, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SectionLabel(text = "Гистограмма яркости · 16 bin")
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
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
private fun ExifMetricCell(
    label: String,
    value: String,
    sub: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(NothingSurface)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = NothingWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (sub != null) {
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = NothingSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
