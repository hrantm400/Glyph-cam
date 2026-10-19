package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CapturedShotEntity
import com.example.ui.components.ChipShape
import com.example.ui.components.CodeBadge
import com.example.ui.components.NothingCard
import com.example.ui.components.SectionLabel
import com.example.ui.components.StudioHeader
import com.example.ui.components.rememberMediaThumbnail
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingObsidian
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
    onDeleteShot: (CapturedShotEntity) -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goBack: () -> Unit = {
        if (inspectedShot != null) onSelectShot(null) else onBackToCamera()
    }
    BackHandler(onBack = goBack)

    val totalBytes = shots.sumOf { it.sizeBytes }
    val openShot: (CapturedShotEntity) -> Unit = { shot ->
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(shot.uri), shot.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // No viewer for this type: nothing to do, the file is still in the gallery.
        }
    }
    val shareShot: (CapturedShotEntity) -> Unit = { shot ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = shot.mimeType
            putExtra(Intent.EXTRA_STREAM, Uri.parse(shot.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Поделиться")) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .testTag("media_vault_screen")
    ) {
        StudioHeader(
            title = "Архив",
            subtitle = if (shots.isEmpty()) "DCIM/GlyphCam" else "${shots.size} ${pluralFiles(shots.size)} · ${formatBytes(totalBytes)} · DCIM/GlyphCam",
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
                Text(text = "Пока нет кадров", style = MaterialTheme.typography.titleMedium, color = NothingWhite)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Фото и видео, снятые в GlyphCam, сохраняются в галерею (папка DCIM/GlyphCam) и появляются здесь с реальными данными EXIF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NothingSilver,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val activeShot = inspectedShot ?: shots.first()
        val rows = remember(shots) { shots.chunked(3) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "inspector") {
                ShotInspectorCard(
                    shot = activeShot,
                    onOpen = { openShot(activeShot) },
                    onShare = { shareShot(activeShot) },
                    onDelete = { onDeleteShot(activeShot) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel(text = "Все кадры", trailing = "${shots.size}", trailingColor = NothingWhite)
                Spacer(modifier = Modifier.height(2.dp))
            }
            items(rows, key = { row -> row.joinToString(",") { it.id.toString() } }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { shot ->
                        ShotGridCell(
                            shot = shot,
                            selected = shot.id == activeShot.id,
                            onClick = { onSelectShot(shot) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

private fun pluralFiles(count: Int): String {
    val n = count % 100
    val n1 = count % 10
    return when {
        n in 11..19 -> "файлов"
        n1 == 1 -> "файл"
        n1 in 2..4 -> "файла"
        else -> "файлов"
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f ГБ", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.US, "%.1f МБ", bytes / 1_048_576.0)
    bytes >= 1024L -> String.format(Locale.US, "%d КБ", bytes / 1024)
    else -> "$bytes Б"
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
}

@Composable
private fun ShotGridCell(
    shot: CapturedShotEntity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumb by rememberMediaThumbnail(uri = shot.uri, isVideo = shot.isVideo, sizePx = 256)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(NothingObsidian)
            .border(if (selected) 2.dp else 1.dp, if (selected) NothingRed else NothingBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("vault_shot_${shot.id}")
    ) {
        val bitmap = thumb
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = shot.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (shot.isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = NothingSilver,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.Center)
            )
        }
        if (shot.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(NothingBlack.copy(alpha = 0.7f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(10.dp))
                Text(text = formatDuration(shot.durationMs), style = MaterialTheme.typography.labelSmall, color = NothingWhite)
            }
        }
        if (shot.mimeType.contains("dng")) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                CodeBadge(text = "DNG", color = ScopeAmber, filled = true)
            }
        }
    }
}

@Composable
private fun ShotInspectorCard(
    shot: CapturedShotEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(shot.timestamp))
    val histValues = shot.histogramSummary.split(",").mapNotNull { it.trim().toIntOrNull() }
    val thumb by rememberMediaThumbnail(uri = shot.uri, isVideo = shot.isVideo, sizePx = 768)
    var confirmDelete by remember(shot.id) { mutableStateOf(false) }
    val ratio = if (shot.width > 0 && shot.height > 0) shot.width.toFloat() / shot.height else 4f / 3f

    NothingCard(
        selected = true,
        accent = NothingRed.copy(alpha = 0.7f),
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exif_inspector_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio.coerceIn(0.5f, 2.2f))
                .heightIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NothingBlack)
                .clickable { onOpen() },
            contentAlignment = Alignment.Center
        ) {
            val bitmap = thumb
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = shot.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (shot.isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = NothingSilver,
                    modifier = Modifier.size(32.dp)
                )
            }
            if (shot.isVideo) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(NothingBlack.copy(alpha = 0.6f))
                        .border(1.dp, NothingWhite.copy(alpha = 0.7f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Воспроизвести", tint = NothingWhite, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shot.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = NothingWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateFmt · ${shot.cameraLabel.ifEmpty { if (shot.isVideo) "видео" else "фото" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            CodeBadge(text = shot.lutApplied + if (shot.lutBaked) " ●" else "", color = ScopeCyan)
        }

        Spacer(modifier = Modifier.height(10.dp))
        val megapixels = if (shot.width > 0) String.format(Locale.US, "%.1f МП", shot.width * shot.height / 1_000_000f) else null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExifMetricCell(
                "РАЗРЕШЕНИЕ",
                if (shot.width > 0) "${shot.width}×${shot.height}" else "—",
                if (shot.isVideo) formatDuration(shot.durationMs) else megapixels,
                Modifier.weight(1f)
            )
            ExifMetricCell("ВЫДЕРЖКА", shot.shutterLabel, null, Modifier.weight(1f))
            ExifMetricCell(
                "ISO · ДИАФР.",
                if (shot.isoUsed > 0) "ISO ${shot.isoUsed}" else "ISO —",
                shot.apertureLabel.ifEmpty { null },
                Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExifMetricCell(
                "ОБЪЕКТИВ",
                String.format(Locale.US, "%.1f×", shot.zoomRatio),
                shot.focalLabel.ifEmpty { null },
                Modifier.weight(1f)
            )
            ExifMetricCell(
                "ББ · EV",
                shot.wbLabel,
                String.format(Locale.US, "EV %+.1f", shot.evValue),
                Modifier.weight(1f)
            )
            ExifMetricCell(
                "ФАЙЛ",
                formatBytes(shot.sizeBytes),
                shot.mimeType.substringAfter('/').uppercase().removePrefix("X-ADOBE-"),
                Modifier.weight(1f)
            )
        }

        if (histValues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel(text = "Гистограмма яркости файла")
            Spacer(modifier = Modifier.height(6.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingBlack)
                    .padding(4.dp)
            ) {
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

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionChip(
                icon = Icons.Default.OpenInNew,
                label = if (shot.isVideo) "СМОТРЕТЬ" else "ОТКРЫТЬ",
                color = NothingWhite,
                onClick = onOpen,
                tag = "btn_open_shot",
                modifier = Modifier.weight(1f)
            )
            ActionChip(
                icon = Icons.Default.Share,
                label = "ПОДЕЛИТЬСЯ",
                color = ScopeCyan,
                onClick = onShare,
                tag = "btn_share_shot",
                modifier = Modifier.weight(1f)
            )
            ActionChip(
                icon = Icons.Default.Delete,
                label = if (confirmDelete) "ТОЧНО?" else "УДАЛИТЬ",
                color = NothingRed,
                onClick = {
                    if (confirmDelete) onDelete() else confirmDelete = true
                },
                tag = "delete_shot_${shot.id}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.7f), ChipShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
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
