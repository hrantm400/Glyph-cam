package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingObsidian
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingSurfaceHigh
import com.example.ui.theme.NothingWhite

/**
 * Shared Nothing-OS flavoured building blocks: one visual language for every screen
 * instead of ad-hoc Box/border combinations.
 */

val CardShape = RoundedCornerShape(16.dp)
val ChipShape = RoundedCornerShape(10.dp)
val PillShape = RoundedCornerShape(50)

/**
 * Fades the horizontal edges of a scrollable row into [fadeColor] (the row's background) so
 * cut-off content reads as scrollable. Plain SrcOver gradients: no offscreen layers or blend
 * modes, so it renders identically on every GPU / software path.
 */
fun Modifier.horizontalEdgeFade(start: Dp, end: Dp, fadeColor: Color): Modifier = this
    .drawWithContent {
        drawContent()
        val startPx = start.toPx()
        val endPx = end.toPx()
        if (startPx > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to fadeColor,
                    1f to fadeColor.copy(alpha = 0f),
                    startX = 0f,
                    endX = startPx
                ),
                topLeft = Offset.Zero,
                size = Size(startPx, size.height)
            )
        }
        if (endPx > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to fadeColor.copy(alpha = 0f),
                    1f to fadeColor,
                    startX = size.width - endPx,
                    endX = size.width
                ),
                topLeft = Offset(size.width - endPx, 0f),
                size = Size(endPx, size.height)
            )
        }
    }

/** Horizontally scrollable row that fades its edges only while there is more to scroll. */
@Composable
fun FadingScrollRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    spacing: Dp = 6.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    fade: Boolean = true,
    fadeColor: Color = NothingBlack,
    content: @Composable RowScope.() -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalEdgeFade(
                start = if (fade && scroll.canScrollBackward) 20.dp else 0.dp,
                end = if (fade && scroll.canScrollForward) 28.dp else 0.dp,
                fadeColor = fadeColor
            )
            .horizontalScroll(scroll)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = verticalAlignment,
        content = content
    )
}

@Composable
fun StudioHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backTag: String = "studio_back_btn",
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag(backTag)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = NothingWhite
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = NothingWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = NothingSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/** Small uppercase mono section label. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingColor: Color = NothingSilver
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = NothingSilver,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = trailingColor,
                maxLines = 1
            )
        }
    }
}

/** Surface card. Selected cards get a 1dp accent outline; others sit flat on black. */
@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = NothingWhite,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(if (selected) NothingSurfaceHigh else NothingObsidian)
            .border(1.dp, if (selected) accent else NothingBorder.copy(alpha = 0.6f), CardShape)
            .then(clickModifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Selectable chip. Selected = solid white pill with black text (Nothing style),
 * or accent-tinted when an accent is supplied.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    tag: String? = null,
    compact: Boolean = false
) {
    val bg = when {
        selected && accent != null -> accent.copy(alpha = 0.18f)
        selected -> NothingWhite
        else -> NothingSurface
    }
    val fg = when {
        selected && accent != null -> accent
        selected -> NothingBlack
        else -> NothingSilver
    }
    val borderColor = when {
        selected && accent != null -> accent
        selected -> NothingWhite
        else -> NothingBorder
    }
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(bg)
            .border(1.dp, borderColor, ChipShape)
            .clickable { onClick() }
            .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 7.dp else 9.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1
        )
    }
}

/** Scrollable row of chips for an enum-like choice. */
@Composable
fun <T> ChoiceChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    tag: ((T) -> String)? = null,
    compact: Boolean = false,
    fadeColor: Color = NothingObsidian
) {
    FadingScrollRow(modifier = modifier.fillMaxWidth(), spacing = 6.dp, fadeColor = fadeColor) {
        items.forEach { item ->
            ChoiceChip(
                label = label(item),
                selected = item == selected,
                onClick = { onSelect(item) },
                accent = accent,
                tag = tag?.invoke(item),
                compact = compact
            )
        }
    }
}

/** Material slider restyled: thin track, round 18dp thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NothingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    accent: Color = NothingWhite,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = NothingSurfaceHigh,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interaction,
        modifier = modifier.height(32.dp),
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interaction,
                colors = colors,
                thumbSize = DpSize(18.dp, 18.dp)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 2.dp,
                drawStopIndicator = null,
                modifier = Modifier.height(4.dp)
            )
        }
    )
}

/** Labelled slider row: "LABEL ........ value" on top, slider below. */
@Composable
fun LabelledSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    accent: Color = NothingWhite,
    steps: Int = 0,
    tag: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = NothingSilver
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
        NothingSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            accent = accent,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (tag != null) Modifier.testTag(tag) else Modifier)
        )
    }
}

/** Small status dot used for REC / level indicators. */
@Composable
fun StatusDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/** Tiny code badge, e.g. LUT code or format. */
@Composable
fun CodeBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled) color else color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = if (filled) 1f else 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (filled) NothingBlack else color,
            maxLines = 1
        )
    }
}

/** Accent helper for the brand red used on primary actions. */
val PrimaryRed: Color get() = NothingRed
