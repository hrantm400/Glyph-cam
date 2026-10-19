package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnamorphicDeSqueeze
import com.example.model.AppMode
import com.example.model.AudioDirectionMode
import com.example.model.FocusPeakingColor
import com.example.model.FrameAspectGuide
import com.example.model.MeteringMode
import com.example.model.NothingLens
import com.example.model.PhotoDriveMode
import com.example.model.PhotoFormat
import com.example.model.StabilizationMode
import com.example.model.ZebraThreshold
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingSurfaceHigh
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.ScopeAmber
import com.example.ui.theme.ScopeCyan
import com.example.ui.theme.ScopeGreen
import com.example.viewmodel.ManualDialTab
import com.example.viewmodel.ProCameraUiState
import java.util.Locale
import kotlin.math.abs

// ---------------------------------------------------------------------------------------------
// Top HUD: status row + quick-settings chips
// ---------------------------------------------------------------------------------------------

@Composable
fun TopTelemetryHudBar(
    state: ProCameraUiState,
    onCycleFormatOrRes: () -> Unit,
    onCycleFpsOrDrive: () -> Unit,
    onCycleCodecOrNd: () -> Unit,
    onCycleAnamorphic: () -> Unit,
    onCycleAspectGuide: () -> Unit,
    onCycleZebra: () -> Unit,
    onCyclePeaking: () -> Unit,
    onToggleFalseColor: () -> Unit,
    onCycleGrid: () -> Unit,
    onToggleHorizon: () -> Unit,
    onToggleAeAfLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 8.dp)
    ) {
        // Row 1: REC / timecode · roll · AE/AF lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusDot(color = if (state.isRecording) NothingRed else ScopeGreen)
                Text(
                    text = state.formattedTimecode,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = if (state.isRecording) NothingRed else NothingWhite
                )
                Text(
                    text = if (state.isRecording) "REC" else state.activeMode.badgeCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.isRecording) NothingRed else NothingSilver
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isLevel = abs(state.rollDegrees) < 1.0f
                Text(
                    text = String.format(Locale.US, "ROLL %+.1f°", state.rollDegrees),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLevel) ScopeGreen else NothingSilver
                )
                Row(
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(if (state.aeAfLocked) ScopeAmber.copy(alpha = 0.2f) else NothingSurface)
                        .border(1.dp, if (state.aeAfLocked) ScopeAmber else NothingBorder, ChipShape)
                        .clickable { onToggleAeAfLock() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_ae_af_lock"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (state.aeAfLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "AE/AF Lock",
                        tint = if (state.aeAfLocked) ScopeAmber else NothingSilver,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (state.aeAfLocked) "AE/AF LOCK" else "AF-C",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.aeAfLocked) ScopeAmber else NothingWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: scrollable quick settings (fades at the edges while more chips are hidden)
        FadingScrollRow(modifier = Modifier.fillMaxWidth(), spacing = 6.dp) {
            if (state.activeMode == AppMode.PHOTO_PRO) {
                HudQuickChip(
                    title = "ФОРМАТ",
                    value = "${state.photoFormat.badge} ${state.photoFormat.bitDepth}",
                    active = state.photoFormat != PhotoFormat.JPEG_FINE,
                    accentColor = ScopeCyan,
                    tag = "chip_photo_format",
                    onClick = onCycleFormatOrRes
                )
                HudQuickChip(
                    title = "СЕРИЯ",
                    value = state.photoDriveMode.code,
                    active = state.photoDriveMode != PhotoDriveMode.SINGLE,
                    accentColor = ScopeAmber,
                    tag = "chip_photo_drive",
                    onClick = onCycleFpsOrDrive
                )
                HudQuickChip(
                    title = "ND",
                    value = if (state.ndFilterStops == 0) "OFF" else "-${state.ndFilterStops} EV",
                    active = state.ndFilterStops > 0,
                    accentColor = NothingRed,
                    tag = "chip_nd_filter",
                    onClick = onCycleCodecOrNd
                )
            } else {
                HudQuickChip(
                    title = "РАЗРЕШЕНИЕ",
                    value = state.videoResolution.label,
                    active = true,
                    accentColor = NothingWhite,
                    tag = "chip_video_res",
                    onClick = onCycleFormatOrRes
                )
                HudQuickChip(
                    title = "КАДРЫ/С",
                    value = state.videoFps.label,
                    active = true,
                    accentColor = ScopeGreen,
                    tag = "chip_video_fps",
                    onClick = onCycleFpsOrDrive
                )
                HudQuickChip(
                    title = "КОДЕК",
                    value = "${state.videoCodec.bitrateMbps}M ${state.videoCodec.dynamicRange.substringBefore(' ')}",
                    active = true,
                    accentColor = ScopeCyan,
                    tag = "chip_video_codec",
                    onClick = onCycleCodecOrNd
                )
            }

            HudQuickChip(
                title = "ДЕ-СКВИЗ",
                value = state.anamorphicDeSqueeze.label,
                active = state.anamorphicDeSqueeze != AnamorphicDeSqueeze.NONE,
                accentColor = ScopeAmber,
                tag = "chip_anamorphic",
                onClick = onCycleAnamorphic
            )
            HudQuickChip(
                title = "РАМКА",
                value = state.aspectGuide.label,
                active = state.aspectGuide != FrameAspectGuide.OFF,
                accentColor = NothingWhite,
                tag = "chip_aspect_guide",
                onClick = onCycleAspectGuide
            )
            HudQuickChip(
                title = "ПИКИНГ",
                value = state.focusPeakingColor.labelRu.substringBefore(' '),
                active = state.focusPeakingColor != FocusPeakingColor.OFF,
                accentColor = if (state.focusPeakingColor == FocusPeakingColor.OFF) NothingSilver else state.focusPeakingColor.color,
                tag = "chip_focus_peaking",
                onClick = onCyclePeaking
            )
            HudQuickChip(
                title = "ЗЕБРА",
                value = state.zebraThreshold.label.substringBefore(' '),
                active = state.zebraThreshold != ZebraThreshold.OFF,
                accentColor = ScopeAmber,
                tag = "chip_zebra",
                onClick = onCycleZebra
            )
            HudQuickChip(
                title = "FALSE COLOR",
                value = if (state.showFalseColor) "ON" else "OFF",
                active = state.showFalseColor,
                accentColor = ScopeCyan,
                tag = "chip_false_color",
                onClick = onToggleFalseColor
            )
            HudQuickChip(
                title = "СЕТКА",
                value = state.gridOverlayMode.code,
                active = state.gridOverlayMode.code != "OFF",
                accentColor = NothingWhite,
                tag = "chip_grid_mode",
                onClick = onCycleGrid
            )
            HudQuickChip(
                title = "ГОРИЗОНТ",
                value = if (state.showHorizonLevel) "ON" else "OFF",
                active = state.showHorizonLevel,
                accentColor = ScopeGreen,
                tag = "chip_horizon_gyro",
                onClick = onToggleHorizon
            )
        }
    }
}

@Composable
private fun HudQuickChip(
    title: String,
    value: String,
    active: Boolean,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(ChipShape)
            .background(if (active) NothingSurfaceHigh else NothingSurface)
            .border(
                width = 1.dp,
                color = if (active) accentColor.copy(alpha = 0.6f) else NothingBorder.copy(alpha = 0.6f),
                shape = ChipShape
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) accentColor else NothingWhite,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Control deck: lens turret, parameter strip, dial drawer
// ---------------------------------------------------------------------------------------------

@Composable
fun LensTurretRow(
    state: ProCameraUiState,
    onSelectLens: (NothingLens) -> Unit,
    modifier: Modifier = Modifier
) {
    val rearLenses = NothingLens.entries.filter { !it.isFront }.sortedBy { it.zoomRatio }
    val frontLens = NothingLens.entries.firstOrNull { it.isFront }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rearLenses.forEach { lens ->
                val selected = state.selectedLens == lens
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (selected) NothingRed else NothingSurface)
                        .border(1.dp, if (selected) NothingRed else NothingBorder, PillShape)
                        .clickable { onSelectLens(lens) }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                        .testTag("lens_btn_${lens.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lens.focalLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = NothingWhite
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.US, "%.1f×", state.fineZoomRatio),
                style = MaterialTheme.typography.labelLarge,
                color = ScopeAmber
            )
            if (frontLens != null) {
                val selected = state.selectedLens == frontLens
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (selected) NothingWhite else NothingSurface)
                        .border(1.dp, if (selected) NothingWhite else NothingBorder, CircleShape)
                        .clickable { onSelectLens(frontLens) }
                        .testTag("lens_btn_${frontLens.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Фронтальная камера",
                        tint = if (selected) androidx.compose.ui.graphics.Color.Black else NothingWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ParameterStrip(
    state: ProCameraUiState,
    onToggleDialTab: (ManualDialTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = state.activeMode == AppMode.CINE_VIDEO
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ParameterDialTile(
            label = "ISO",
            value = if (state.isoValue > 0) state.isoValue.toString() else "AUTO",
            isManual = state.isoValue > 0,
            isSelected = state.selectedDialTab == ManualDialTab.ISO,
            tag = "dial_tab_iso",
            onClick = { onToggleDialTab(ManualDialTab.ISO) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = if (isVideo && state.useShutterAngleInVideo) "УГОЛ" else "ЗАТВОР",
            value = state.shutterDisplayLabel,
            isManual = state.shutterDenominator > 0,
            isSelected = state.selectedDialTab == ManualDialTab.SHUTTER,
            tag = "dial_tab_shutter",
            onClick = { onToggleDialTab(ManualDialTab.SHUTTER) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = "ФОКУС",
            value = state.focusDisplayLabel,
            isManual = state.manualFocusDiopters != null,
            isSelected = state.selectedDialTab == ManualDialTab.FOCUS,
            tag = "dial_tab_focus",
            onClick = { onToggleDialTab(ManualDialTab.FOCUS) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = "ББ",
            value = state.wbDisplayLabel,
            isManual = state.wbKelvin > 0,
            isSelected = state.selectedDialTab == ManualDialTab.WB,
            tag = "dial_tab_wb",
            onClick = { onToggleDialTab(ManualDialTab.WB) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = "EV",
            value = String.format(Locale.US, "%+.1f", state.evCompensation),
            isManual = abs(state.evCompensation) > 0.05f,
            isSelected = state.selectedDialTab == ManualDialTab.EV,
            tag = "dial_tab_ev",
            onClick = { onToggleDialTab(ManualDialTab.EV) },
            modifier = Modifier.weight(1f)
        )
        if (isVideo) {
            ParameterDialTile(
                label = "ЗВУК",
                value = String.format(Locale.US, "%+.0fdB", state.audioGainDb),
                isManual = abs(state.audioGainDb) > 0.05f,
                isSelected = state.selectedDialTab == ManualDialTab.AUDIO,
                tag = "dial_tab_audio",
                onClick = { onToggleDialTab(ManualDialTab.AUDIO) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ParameterDialTile(
    label: String,
    value: String,
    isManual: Boolean,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> NothingRed
        isManual -> ScopeAmber.copy(alpha = 0.6f)
        else -> NothingBorder.copy(alpha = 0.5f)
    }
    Column(
        modifier = modifier
            .clip(ChipShape)
            .background(if (isSelected) NothingSurfaceHigh else NothingSurface)
            .border(1.dp, borderColor, ChipShape)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) NothingWhite else NothingSilver,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = if (isManual) ScopeAmber else NothingWhite,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun ManualDialDrawer(
    state: ProCameraUiState,
    onIsoChange: (Int) -> Unit,
    onShutterDenomChange: (Int) -> Unit,
    onShutterAngleChange: (Float) -> Unit,
    onToggleShutterAngleMode: () -> Unit,
    onManualFocusChange: (Float?) -> Unit,
    onTriggerFocusPull: (String) -> Unit,
    onSaveFocusPullPoint: (String) -> Unit,
    onFocusPullSpeedChange: (Long) -> Unit,
    onWbChange: (Int, Int) -> Unit,
    onEvChange: (Float) -> Unit,
    onMeteringChange: (MeteringMode) -> Unit,
    onStabilizationChange: (StabilizationMode) -> Unit,
    onAudioGainChange: (Float) -> Unit,
    onToggleWindCut: () -> Unit,
    onAudioDirectionChange: (AudioDirectionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.selectedDialTab != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val tab = state.selectedDialTab
        if (tab != null) {
            NothingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                when (tab) {
                    ManualDialTab.ISO -> IsoDialDrawer(
                        currentIso = state.isoValue,
                        onSelectIso = onIsoChange
                    )
                    ManualDialTab.SHUTTER -> ShutterDialDrawer(
                        state = state,
                        onSelectDenom = onShutterDenomChange,
                        onSelectAngle = onShutterAngleChange,
                        onToggleAngleMode = onToggleShutterAngleMode
                    )
                    ManualDialTab.FOCUS -> FocusAndPullerDrawer(
                        state = state,
                        onManualFocusChange = onManualFocusChange,
                        onTriggerPull = onTriggerFocusPull,
                        onSavePoint = onSaveFocusPullPoint,
                        onSpeedChange = onFocusPullSpeedChange
                    )
                    ManualDialTab.WB -> WhiteBalanceDrawer(
                        wbKelvin = state.wbKelvin,
                        wbTint = state.wbTint,
                        onWbChange = onWbChange
                    )
                    ManualDialTab.EV -> ExposureAndMeteringDrawer(
                        evValue = state.evCompensation,
                        meteringMode = state.meteringMode,
                        stabilizationMode = state.stabilizationMode,
                        onEvChange = onEvChange,
                        onMeteringChange = onMeteringChange,
                        onStabilizationChange = onStabilizationChange
                    )
                    ManualDialTab.AUDIO -> AudioStudioDrawer(
                        gainDb = state.audioGainDb,
                        windCut = state.windCutEnabled,
                        direction = state.audioDirection,
                        onGainChange = onAudioGainChange,
                        onToggleWindCut = onToggleWindCut,
                        onDirectionChange = onAudioDirectionChange
                    )
                }
            }
        }
    }
}

@Composable
private fun IsoDialDrawer(
    currentIso: Int,
    onSelectIso: (Int) -> Unit
) {
    val isoStops = listOf(0, 50, 100, 160, 200, 400, 800, 1600, 3200, 6400, 12800)
    Column {
        SectionLabel(
            text = "Чувствительность ISO · Sony IMX890",
            trailing = when (currentIso) {
                0 -> "AUTO"
                100, 800 -> "DUAL NATIVE"
                else -> "РУЧНОЙ"
            },
            trailingColor = ScopeGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        ChoiceChipRow(
            items = isoStops,
            selected = currentIso,
            label = { if (it == 0) "AUTO" else it.toString() },
            onSelect = onSelectIso,
            tag = { "iso_opt_$it" },
            compact = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TactileRulerTickStrip(activeFraction = isoStops.indexOf(currentIso).coerceAtLeast(0) / (isoStops.size - 1f))
    }
}

@Composable
private fun ShutterDialDrawer(
    state: ProCameraUiState,
    onSelectDenom: (Int) -> Unit,
    onSelectAngle: (Float) -> Unit,
    onToggleAngleMode: () -> Unit
) {
    val shutterDenoms = listOf(0, 8000, 4000, 2000, 1000, 500, 250, 125, 60, 50, 48, 30, 15, 4, 1)
    val shutterAngles = listOf(45f, 90f, 144f, 172.8f, 180.0f, 270f, 360f)
    val isVideo = state.activeMode == AppMode.CINE_VIDEO
    val angleMode = isVideo && state.useShutterAngleInVideo

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(
                text = if (angleMode) "Угол затвора · правило 180°" else "Выдержка затвора",
                modifier = Modifier.weight(1f)
            )
            if (isVideo) {
                Spacer(modifier = Modifier.width(8.dp))
                ChoiceChip(
                    label = if (state.useShutterAngleInVideo) "ГРАДУСЫ" else "СЕКУНДЫ",
                    selected = true,
                    accent = ScopeAmber,
                    onClick = onToggleAngleMode,
                    tag = "toggle_shutter_angle_mode",
                    compact = true
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (angleMode) {
            ChoiceChipRow(
                items = shutterAngles,
                selected = shutterAngles.firstOrNull { abs(state.shutterAngleDeg - it) < 1f } ?: -1f,
                label = { String.format(Locale.US, "%.1f°", it) },
                onSelect = onSelectAngle,
                tag = { "shutter_angle_${it.toInt()}" },
                compact = true
            )
        } else {
            ChoiceChipRow(
                items = shutterDenoms,
                selected = state.shutterDenominator,
                label = { denom ->
                    when (denom) {
                        0 -> "AUTO"
                        1 -> "1.0\""
                        else -> "1/$denom"
                    }
                },
                onSelect = onSelectDenom,
                tag = { "shutter_denom_$it" },
                compact = true
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val fraction = if (angleMode) {
            (state.shutterAngleDeg / 360f).coerceIn(0f, 1f)
        } else {
            shutterDenoms.indexOf(state.shutterDenominator).coerceAtLeast(0) / (shutterDenoms.size - 1f)
        }
        TactileRulerTickStrip(activeFraction = fraction)
    }
}

@Composable
private fun FocusAndPullerDrawer(
    state: ProCameraUiState,
    onManualFocusChange: (Float?) -> Unit,
    onTriggerPull: (String) -> Unit,
    onSavePoint: (String) -> Unit,
    onSpeedChange: (Long) -> Unit
) {
    val isManual = state.manualFocusDiopters != null
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(text = "Фокус", modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceChip(
                    label = "AF-C",
                    selected = !isManual,
                    accent = ScopeGreen,
                    onClick = { if (isManual) onManualFocusChange(null) },
                    compact = true
                )
                ChoiceChip(
                    label = "MF",
                    selected = isManual,
                    accent = ScopeAmber,
                    onClick = { if (!isManual) onManualFocusChange(2.0f) },
                    tag = "btn_toggle_af_mf",
                    compact = true
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LabelledSlider(
            label = "Дистанция",
            valueText = state.focusDisplayLabel,
            value = state.manualFocusDiopters ?: 2.0f,
            onValueChange = { onManualFocusChange(it) },
            valueRange = 0.0f..10.0f,
            accent = ScopeAmber,
            tag = "slider_manual_focus"
        )

        Spacer(modifier = Modifier.height(6.dp))
        SectionLabel(text = "Перевод фокуса A / B / C")
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            state.focusPullPoints.forEach { pt ->
                val isSlotActive = state.activeFocusPullSlot == pt.slot
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ChipShape)
                        .background(if (isSlotActive) NothingRed.copy(alpha = 0.22f) else NothingSurface)
                        .border(1.dp, if (isSlotActive) NothingRed else NothingBorder, ChipShape)
                        .clickable { onTriggerPull(pt.slot) }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag("focus_pull_slot_${pt.slot}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pt.slot,
                            style = MaterialTheme.typography.labelLarge,
                            color = ScopeAmber
                        )
                        Text(
                            text = pt.labelMeters,
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ЗАПИСАТЬ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScopeCyan,
                        modifier = Modifier
                            .clickable { onSavePoint(pt.slot) }
                            .padding(vertical = 2.dp)
                            .testTag("focus_save_slot_${pt.slot}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(600L to "0.6с", 1400L to "1.4с КИНО", 2800L to "2.8с").forEach { (ms, lbl) ->
                ChoiceChip(
                    label = lbl,
                    selected = state.focusPullDurationMs == ms,
                    accent = ScopeCyan,
                    onClick = { onSpeedChange(ms) },
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun WhiteBalanceDrawer(
    wbKelvin: Int,
    wbTint: Int,
    onWbChange: (Int, Int) -> Unit
) {
    val presets = listOf(
        0 to "AWB",
        3200 to "3200K ЛАМПА",
        4300 to "4300K НЕОН",
        5600 to "5600K ДЕНЬ",
        6500 to "6500K ОБЛАКА",
        7500 to "7500K ТЕНЬ"
    )
    Column {
        SectionLabel(text = "Баланс белого")
        Spacer(modifier = Modifier.height(8.dp))
        ChoiceChipRow(
            items = presets,
            selected = presets.firstOrNull { it.first == wbKelvin } ?: (-1 to ""),
            label = { it.second },
            onSelect = { onWbChange(it.first, wbTint) },
            tag = { "wb_preset_${it.first}" },
            compact = true
        )
        Spacer(modifier = Modifier.height(6.dp))
        LabelledSlider(
            label = "Температура",
            valueText = "${if (wbKelvin == 0) 5600 else wbKelvin}K",
            value = (if (wbKelvin == 0) 5600 else wbKelvin).toFloat(),
            onValueChange = { onWbChange((it.toInt() / 100) * 100, wbTint) },
            valueRange = 2500f..10000f,
            accent = ScopeAmber
        )
        LabelledSlider(
            label = "Оттенок",
            valueText = String.format(Locale.US, "%+d", wbTint),
            value = wbTint.toFloat(),
            onValueChange = { onWbChange(wbKelvin, it.toInt()) },
            valueRange = -50f..50f,
            accent = ScopeGreen
        )
    }
}

@Composable
private fun ExposureAndMeteringDrawer(
    evValue: Float,
    meteringMode: MeteringMode,
    stabilizationMode: StabilizationMode,
    onEvChange: (Float) -> Unit,
    onMeteringChange: (MeteringMode) -> Unit,
    onStabilizationChange: (StabilizationMode) -> Unit
) {
    Column {
        LabelledSlider(
            label = "Экспокоррекция",
            valueText = String.format(Locale.US, "EV %+.1f", evValue),
            value = evValue,
            onValueChange = onEvChange,
            valueRange = -3.0f..3.0f,
            steps = 59,
            accent = ScopeAmber,
            tag = "slider_ev_compensation"
        )
        Spacer(modifier = Modifier.height(6.dp))
        SectionLabel(text = "Замер экспозиции")
        Spacer(modifier = Modifier.height(6.dp))
        ChoiceChipRow(
            items = MeteringMode.entries,
            selected = meteringMode,
            label = { it.labelRu },
            onSelect = onMeteringChange,
            accent = ScopeCyan,
            compact = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        SectionLabel(text = "Стабилизация")
        Spacer(modifier = Modifier.height(6.dp))
        ChoiceChipRow(
            items = StabilizationMode.entries,
            selected = stabilizationMode,
            label = { it.labelRu },
            onSelect = onStabilizationChange,
            accent = ScopeGreen,
            compact = true
        )
    }
}

@Composable
private fun AudioStudioDrawer(
    gainDb: Float,
    windCut: Boolean,
    direction: AudioDirectionMode,
    onGainChange: (Float) -> Unit,
    onToggleWindCut: () -> Unit,
    onDirectionChange: (AudioDirectionMode) -> Unit
) {
    Column {
        LabelledSlider(
            label = "Усиление микрофона",
            valueText = String.format(Locale.US, "%+.1f dB", gainDb),
            value = gainDb,
            onValueChange = onGainChange,
            valueRange = -12f..12f,
            accent = ScopeGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        ChoiceChip(
            label = if (windCut) "ВЕТРОЗАЩИТА 80 Hz · ВКЛ" else "ВЕТРОЗАЩИТА 80 Hz · ВЫКЛ",
            selected = windCut,
            accent = ScopeGreen,
            onClick = onToggleWindCut,
            tag = "btn_toggle_wind_cut",
            compact = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        SectionLabel(text = "Направленность микрофонов")
        Spacer(modifier = Modifier.height(6.dp))
        ChoiceChipRow(
            items = AudioDirectionMode.entries,
            selected = direction,
            label = { it.labelRu },
            onSelect = onDirectionChange,
            accent = ScopeAmber,
            compact = true
        )
    }
}

@Composable
private fun TactileRulerTickStrip(activeFraction: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val totalTicks = 41
        val stepX = size.width / (totalTicks - 1f)
        val activeIndex = (activeFraction.coerceIn(0f, 1f) * (totalTicks - 1)).toInt()

        for (i in 0 until totalTicks) {
            val x = i * stepX
            val isMajor = i % 5 == 0
            val isCurrent = abs(i - activeIndex) <= 1
            val tickH = when {
                isCurrent -> size.height
                isMajor -> size.height * 0.72f
                else -> size.height * 0.42f
            }
            val color = when {
                i == activeIndex -> NothingRed
                isMajor -> NothingWhite.copy(alpha = 0.75f)
                else -> NothingBorder
            }
            drawLine(
                color = color,
                start = Offset(x, size.height - tickH),
                end = Offset(x, size.height),
                strokeWidth = if (i == activeIndex) 2.5.dp.toPx() else 1.2.dp.toPx()
            )
        }
    }
}
