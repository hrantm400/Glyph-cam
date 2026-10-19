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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.example.model.VideoCodecProfile
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.model.ZebraThreshold
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
import com.example.viewmodel.ManualDialTab
import com.example.viewmodel.ProCameraUiState
import java.util.Locale
import kotlin.math.abs

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
            .background(NothingBlack.copy(alpha = 0.88f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Row 1: Nothing Phone (2) Hardware Badge + Timecode + Gyro Readout + AE/AF Lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (state.isRecording) NothingRed else ScopeGreen)
                )
                Text(
                    text = if (state.isRecording) "REC // ${state.formattedTimecode}" else "NP(2) // ${state.formattedTimecode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.isRecording) NothingRed else NothingWhite,
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isLevel = abs(state.rollDegrees) < 1.0f
                Text(
                    text = String.format(Locale.US, "ROLL %+.1f°", state.rollDegrees),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLevel) ScopeGreen else NothingSilver,
                    fontSize = 10.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.aeAfLocked) ScopeAmber.copy(alpha = 0.22f) else NothingSurface)
                        .border(
                            1.dp,
                            if (state.aeAfLocked) ScopeAmber else NothingBorder,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onToggleAeAfLock() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("toggle_ae_af_lock")
                ) {
                    Row(
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
                            color = if (state.aeAfLocked) ScopeAmber else NothingWhite,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Scrollable Cinema & Optical Assist Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    title = "ПРИВОД",
                    value = state.photoDriveMode.code,
                    active = state.photoDriveMode != PhotoDriveMode.SINGLE,
                    accentColor = ScopeAmber,
                    tag = "chip_photo_drive",
                    onClick = onCycleFpsOrDrive
                )
                HudQuickChip(
                    title = "ND ФИЛЬТР",
                    value = if (state.ndFilterStops == 0) "OFF" else "ND -${state.ndFilterStops}EV",
                    active = state.ndFilterStops > 0,
                    accentColor = NothingRed,
                    tag = "chip_nd_filter",
                    onClick = onCycleCodecOrNd
                )
            } else {
                HudQuickChip(
                    title = "МАТРИЦА",
                    value = state.videoResolution.label,
                    active = true,
                    accentColor = NothingRed,
                    tag = "chip_video_res",
                    onClick = onCycleFormatOrRes
                )
                HudQuickChip(
                    title = "ЧАСТОТА",
                    value = state.videoFps.label,
                    active = true,
                    accentColor = ScopeGreen,
                    tag = "chip_video_fps",
                    onClick = onCycleFpsOrDrive
                )
                HudQuickChip(
                    title = "ПОТОК",
                    value = "${state.videoCodec.bitrateMbps}M ${state.videoCodec.dynamicRange.take(5)}",
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
                value = state.focusPeakingColor.labelRu.take(7),
                active = state.focusPeakingColor != FocusPeakingColor.OFF,
                accentColor = if (state.focusPeakingColor == FocusPeakingColor.OFF) NothingSilver else state.focusPeakingColor.color,
                tag = "chip_focus_peaking",
                onClick = onCyclePeaking
            )

            HudQuickChip(
                title = "ЗЕБРА",
                value = state.zebraThreshold.label.take(6),
                active = state.zebraThreshold != ZebraThreshold.OFF,
                accentColor = ScopeAmber,
                tag = "chip_zebra",
                onClick = onCycleZebra
            )

            HudQuickChip(
                title = "FALSE CLR",
                value = if (state.showFalseColor) "IRE ON" else "OFF",
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
                value = if (state.showHorizonLevel) "GYRO ON" else "OFF",
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
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) NothingSurfaceHigh else NothingObsidian)
            .border(
                width = 1.dp,
                color = if (active) accentColor.copy(alpha = 0.7f) else NothingBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = NothingSilver,
            fontSize = 8.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) accentColor else NothingWhite,
            fontSize = 11.sp
        )
    }
}

@Composable
fun LensAndManualControlDeck(
    state: ProCameraUiState,
    onSelectLens: (NothingLens) -> Unit,
    onFineZoomChange: (Float) -> Unit,
    onToggleDialTab: (ManualDialTab) -> Unit,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NothingBlack.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 1. Expandable Mechanical Dial Drawer for active parameter
        AnimatedVisibility(
            visible = state.selectedDialTab != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            state.selectedDialTab?.let { tab ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingObsidian)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
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

        // 2. Nothing Phone (2) Multi-Lens Turret Selector + Fine Zoom Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NothingLens.entries.forEach { lens ->
                    val selected = state.selectedLens == lens
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) NothingRed else NothingSurface)
                            .border(
                                1.dp,
                                if (selected) NothingRed else NothingBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectLens(lens) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("lens_btn_${lens.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lens.focalLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = NothingWhite,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                text = String.format(Locale.US, "%.1f× ZOOM", state.fineZoomRatio),
                style = MaterialTheme.typography.labelLarge,
                color = ScopeAmber,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. 6-Channel Pro Parameter Strip (ISO | SHTR | MF | WB | EV | AUD)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ParameterDialButton(
                label = "ISO",
                value = state.isoDisplayLabel,
                isManual = state.isoValue > 0,
                isSelected = state.selectedDialTab == ManualDialTab.ISO,
                tag = "dial_tab_iso",
                onClick = { onToggleDialTab(ManualDialTab.ISO) }
            )
            ParameterDialButton(
                label = if (state.activeMode == AppMode.CINE_VIDEO && state.useShutterAngleInVideo) "SHUTTER °" else "SHUTTER",
                value = state.shutterDisplayLabel,
                isManual = state.shutterDenominator > 0,
                isSelected = state.selectedDialTab == ManualDialTab.SHUTTER,
                tag = "dial_tab_shutter",
                onClick = { onToggleDialTab(ManualDialTab.SHUTTER) }
            )
            ParameterDialButton(
                label = "ФОКУС MF",
                value = state.focusDisplayLabel,
                isManual = state.manualFocusDiopters != null,
                isSelected = state.selectedDialTab == ManualDialTab.FOCUS,
                tag = "dial_tab_focus",
                onClick = { onToggleDialTab(ManualDialTab.FOCUS) }
            )
            ParameterDialButton(
                label = "ББ KELVIN",
                value = state.wbDisplayLabel,
                isManual = state.wbKelvin > 0,
                isSelected = state.selectedDialTab == ManualDialTab.WB,
                tag = "dial_tab_wb",
                onClick = { onToggleDialTab(ManualDialTab.WB) }
            )
            ParameterDialButton(
                label = "ЭКСПО EV",
                value = String.format(Locale.US, "%+.1f", state.evCompensation),
                isManual = abs(state.evCompensation) > 0.05f,
                isSelected = state.selectedDialTab == ManualDialTab.EV,
                tag = "dial_tab_ev",
                onClick = { onToggleDialTab(ManualDialTab.EV) }
            )
            ParameterDialButton(
                label = "АУДИО 24B",
                value = String.format(Locale.US, "%+.0fdB %s", state.audioGainDb, state.audioDirection.code),
                isManual = state.windCutEnabled,
                isSelected = state.selectedDialTab == ManualDialTab.AUDIO,
                tag = "dial_tab_audio",
                onClick = { onToggleDialTab(ManualDialTab.AUDIO) }
            )
        }
    }
}

@Composable
private fun ParameterDialButton(
    label: String,
    value: String,
    isManual: Boolean,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NothingSurfaceHigh else NothingSurface)
            .border(
                width = 1.dp,
                color = when {
                    isSelected -> NothingRed
                    isManual -> ScopeAmber.copy(alpha = 0.65f)
                    else -> NothingBorder
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = NothingSilver,
                fontSize = 8.sp
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isManual) ScopeAmber else ScopeGreen)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) NothingWhite else if (isManual) ScopeAmber else NothingWhite,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun IsoDialDrawer(
    currentIso: Int,
    onSelectIso: (Int) -> Unit
) {
    val isoStops = listOf(0, 50, 100, 160, 200, 400, 800, 1600, 3200, 6400, 12800)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ЧУВСТВИТЕЛЬНОСТЬ СЕНСОРА SONY IMX890",
                style = MaterialTheme.typography.labelMedium,
                color = NothingSilver
            )
            Text(
                text = if (currentIso == 800 || currentIso == 100) "DUAL NATIVE ISO БАЗА" else "РУЧНОЙ ISO",
                style = MaterialTheme.typography.labelMedium,
                color = ScopeGreen
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            isoStops.forEach { iso ->
                val selected = currentIso == iso
                val label = if (iso == 0) "AUTO" else iso.toString()
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) NothingRed else NothingSurface)
                        .border(1.dp, if (selected) NothingRed else NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { onSelectIso(iso) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("iso_opt_$iso"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = NothingWhite
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
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

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.useShutterAngleInVideo && state.activeMode == AppMode.CINE_VIDEO)
                    "КИНО-УГОЛ ЗАТВОРА (ПРАВИЛО 180°)"
                else
                    "ВЫДЕРЖКА ЭЛЕКТРОННОГО ЗАТВОРА",
                style = MaterialTheme.typography.labelMedium,
                color = NothingSilver
            )
            if (state.activeMode == AppMode.CINE_VIDEO) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NothingSurfaceHigh)
                        .border(1.dp, ScopeAmber, RoundedCornerShape(4.dp))
                        .clickable { onToggleAngleMode() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("toggle_shutter_angle_mode")
                ) {
                    Text(
                        text = if (state.useShutterAngleInVideo) "УГОЛ ° -> СЕК" else "СЕК -> УГОЛ °",
                        style = MaterialTheme.typography.labelMedium,
                        color = ScopeAmber,
                        fontSize = 9.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (state.activeMode == AppMode.CINE_VIDEO && state.useShutterAngleInVideo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                shutterAngles.forEach { angle ->
                    val selected = abs(state.shutterAngleDeg - angle) < 1f
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) NothingRed else NothingSurface)
                            .border(1.dp, if (selected) NothingRed else NothingBorder, RoundedCornerShape(6.dp))
                            .clickable { onSelectAngle(angle) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("shutter_angle_${angle.toInt()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${angle}°",
                            style = MaterialTheme.typography.labelLarge,
                            color = NothingWhite
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                shutterDenoms.forEach { denom ->
                    val selected = state.shutterDenominator == denom
                    val label = when (denom) {
                        0 -> "AUTO"
                        1 -> "1.0\""
                        else -> "1/$denom"
                    }
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) NothingRed else NothingSurface)
                            .border(1.dp, if (selected) NothingRed else NothingBorder, RoundedCornerShape(6.dp))
                            .clickable { onSelectDenom(denom) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("shutter_denom_$denom"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = NothingWhite
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TactileRulerTickStrip(activeFraction = 0.5f)
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "РУЧНОЙ ФОКУС И МОТОРИЗОВАННЫЙ ПУЛЛЕР A/B/C",
                style = MaterialTheme.typography.labelMedium,
                color = NothingSilver
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (state.manualFocusDiopters == null) ScopeGreen.copy(alpha = 0.2f) else NothingSurface)
                    .border(
                        1.dp,
                        if (state.manualFocusDiopters == null) ScopeGreen else NothingBorder,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        onManualFocusChange(if (state.manualFocusDiopters == null) 2.0f else null)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("btn_toggle_af_mf")
            ) {
                Text(
                    text = if (state.manualFocusDiopters == null) "AF-C АКТИВЕН (НАЖМИ ДЛЯ MF)" else "ВЕРНУТЬ AF-C",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.manualFocusDiopters == null) ScopeGreen else NothingWhite,
                    fontSize = 9.sp
                )
            }
        }

        Slider(
            value = state.manualFocusDiopters ?: 2.0f,
            onValueChange = { onManualFocusChange(it) },
            valueRange = 0.0f..10.0f,
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = ScopeAmber,
                inactiveTrackColor = NothingSurfaceHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_manual_focus")
        )

        // Focus Puller A / B / C Programmable Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.focusPullPoints.forEach { pt ->
                val isSlotActive = state.activeFocusPullSlot == pt.slot
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSlotActive) NothingRed.copy(alpha = 0.28f) else NothingSurface)
                        .border(
                            1.dp,
                            if (isSlotActive) NothingRed else NothingBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTriggerPull(pt.slot) }
                            .testTag("focus_pull_slot_${pt.slot}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ПУЛ [${pt.slot}]",
                            style = MaterialTheme.typography.labelLarge,
                            color = ScopeAmber
                        )
                        Text(
                            text = pt.labelMeters,
                            style = MaterialTheme.typography.labelMedium,
                            color = NothingWhite,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ЗАПИСАТЬ ТЕКУЩ.",
                        style = MaterialTheme.typography.labelMedium,
                        color = ScopeCyan,
                        fontSize = 8.sp,
                        modifier = Modifier
                            .clickable { onSavePoint(pt.slot) }
                            .padding(vertical = 2.dp)
                            .testTag("focus_save_slot_${pt.slot}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(600L to "0.6с БЫСТРО", 1400L to "1.4с КИНО", 2800L to "2.8с ПЛАВНО").forEach { (ms, lbl) ->
                val selected = state.focusPullDurationMs == ms
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) NothingSurfaceHigh else NothingObsidian)
                        .border(1.dp, if (selected) ScopeCyan else NothingBorder, RoundedCornerShape(4.dp))
                        .clickable { onSpeedChange(ms) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) ScopeCyan else NothingSilver,
                        fontSize = 9.sp
                    )
                }
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
        0 to "AWB АВТО",
        3200 to "3200K ЛАМПА",
        4300 to "4300K НЕОН",
        5600 to "5600K ДЕНЬ",
        6500 to "6500K ОБЛАКА",
        7500 to "7500K ТЕНЬ"
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (k, label) ->
                val selected = wbKelvin == k
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) NothingRed else NothingSurface)
                        .border(1.dp, if (selected) NothingRed else NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { onWbChange(k, wbTint) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("wb_preset_$k"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelMedium, color = NothingWhite)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ТЕМП: ${if (wbKelvin == 0) 5600 else wbKelvin}K",
                style = MaterialTheme.typography.labelMedium,
                color = ScopeAmber,
                modifier = Modifier.width(86.dp)
            )
            Slider(
                value = (if (wbKelvin == 0) 5600 else wbKelvin).toFloat(),
                onValueChange = { onWbChange((it.toInt() / 100) * 100, wbTint) },
                valueRange = 2500f..10000f,
                colors = SliderDefaults.colors(
                    thumbColor = ScopeAmber,
                    activeTrackColor = ScopeAmber
                ),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ОТТЕНОК: ${String.format(Locale.US, "%+d", wbTint)}",
                style = MaterialTheme.typography.labelMedium,
                color = ScopeGreen,
                modifier = Modifier.width(86.dp)
            )
            Slider(
                value = wbTint.toFloat(),
                onValueChange = { onWbChange(wbKelvin, it.toInt()) },
                valueRange = -50f..50f,
                colors = SliderDefaults.colors(
                    thumbColor = ScopeGreen,
                    activeTrackColor = ScopeGreen
                ),
                modifier = Modifier.weight(1f)
            )
        }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = String.format(Locale.US, "EV %+.1f", evValue),
                style = MaterialTheme.typography.labelLarge,
                color = ScopeAmber,
                modifier = Modifier.width(68.dp)
            )
            Slider(
                value = evValue,
                onValueChange = onEvChange,
                valueRange = -3.0f..3.0f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = NothingRed,
                    activeTrackColor = ScopeAmber
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("slider_ev_compensation")
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MeteringMode.entries.forEach { m ->
                val sel = meteringMode == m
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) NothingSurfaceHigh else NothingSurface)
                        .border(1.dp, if (sel) ScopeCyan else NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { onMeteringChange(m) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = m.labelRu,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sel) ScopeCyan else NothingSilver
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StabilizationMode.entries.forEach { stab ->
                val sel = stabilizationMode == stab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) NothingSurfaceHigh else NothingSurface)
                        .border(1.dp, if (sel) ScopeGreen else NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { onStabilizationChange(stab) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stab.labelRu,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sel) ScopeGreen else NothingSilver
                    )
                }
            }
        }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.US, "УСИЛЕНИЕ МИКРОФОНОВ: %+.1f dB", gainDb),
                style = MaterialTheme.typography.labelMedium,
                color = ScopeGreen
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (windCut) ScopeGreen.copy(alpha = 0.2f) else NothingSurface)
                    .border(1.dp, if (windCut) ScopeGreen else NothingBorder, RoundedCornerShape(4.dp))
                    .clickable { onToggleWindCut() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("btn_toggle_wind_cut")
            ) {
                Text(
                    text = if (windCut) "80Hz HPF ВЕТРОЗАЩИТА ON" else "80Hz HPF OFF",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (windCut) ScopeGreen else NothingSilver,
                    fontSize = 9.sp
                )
            }
        }
        Slider(
            value = gainDb,
            onValueChange = onGainChange,
            valueRange = -12f..12f,
            colors = SliderDefaults.colors(
                thumbColor = ScopeGreen,
                activeTrackColor = ScopeGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AudioDirectionMode.entries.forEach { dir ->
                val sel = direction == dir
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) NothingSurfaceHigh else NothingSurface)
                        .border(1.dp, if (sel) ScopeAmber else NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { onDirectionChange(dir) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dir.labelRu,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sel) ScopeAmber else NothingSilver
                    )
                }
            }
        }
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
