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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnamorphicDeSqueeze
import com.example.model.AwbPreset
import com.example.model.CameraStatus
import com.example.model.FlashMode
import com.example.model.FocusPeakingColor
import com.example.model.FrameAspectGuide
import com.example.model.GridOverlayMode
import com.example.model.LensOption
import com.example.model.PhotoDriveMode
import com.example.model.PhotoFormat
import com.example.model.SelfTimer
import com.example.model.VideoDynamicRange
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
import com.example.viewmodel.formatExposure
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Every control callback of the camera deck in one place, so the composables stay readable. */
class CameraDeckActions(
    val cyclePhotoFormat: () -> Unit,
    val cyclePhotoDrive: () -> Unit,
    val cycleSelfTimer: () -> Unit,
    val cycleFlash: () -> Unit,
    val cycleVideoResolution: () -> Unit,
    val cycleVideoFps: () -> Unit,
    val cycleDynamicRange: () -> Unit,
    val toggleStabilization: () -> Unit,
    val cycleAnamorphic: () -> Unit,
    val cycleAspectGuide: () -> Unit,
    val cycleZebra: () -> Unit,
    val cyclePeaking: () -> Unit,
    val toggleFalseColor: () -> Unit,
    val cycleGrid: () -> Unit,
    val toggleHorizon: () -> Unit,
    val toggleAeAfLock: () -> Unit,
    val selectLens: (LensOption) -> Unit,
    val toggleFrontCamera: () -> Unit,
    val toggleDialTab: (ManualDialTab) -> Unit,
    val setIso: (Int) -> Unit,
    val setShutterNs: (Long) -> Unit,
    val setShutterAngle: (Float) -> Unit,
    val toggleShutterAngleMode: () -> Unit,
    val setManualFocus: (Float?) -> Unit,
    val triggerFocusPull: (String) -> Unit,
    val saveFocusPoint: (String) -> Unit,
    val setFocusPullSpeed: (Long) -> Unit,
    val setWhiteBalance: (AwbPreset) -> Unit,
    val setEv: (Float) -> Unit,
    val toggleAudio: () -> Unit
)

// ---------------------------------------------------------------------------------------------
// Top HUD: live readout row + quick-settings chips
// ---------------------------------------------------------------------------------------------

@Composable
fun TopTelemetryHudBar(
    state: ProCameraUiState,
    actions: CameraDeckActions,
    modifier: Modifier = Modifier
) {
    val caps = state.capabilities
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 8.dp)
    ) {
        // Row 1: REC / mode · live sensor readout · roll · AE/AF lock
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusDot(color = if (state.isRecording) NothingRed else ScopeGreen)
                if (state.isVideoMode) {
                    Text(
                        text = state.formattedTimecode,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = if (state.isRecording) NothingRed else NothingWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Text(
                        text = state.activeMode.badgeCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver
                    )
                }
                val live = state.liveExposure
                val readout = if (live.iso <= 0 && live.exposureNs <= 0L) {
                    when (state.cameraStatus) {
                        is CameraStatus.Ready -> caps.cameraLabel
                        is CameraStatus.Error -> "ОШИБКА КАМЕРЫ"
                        is CameraStatus.NoPermission -> "НЕТ ДОСТУПА"
                        else -> "ЗАПУСК…"
                    }
                } else {
                    buildString {
                        append("ISO ")
                        append(state.liveIsoLabel)
                        append(" · ")
                        append(state.liveShutterLabel)
                        if (caps.apertureLabel.isNotEmpty()) {
                            append(" · ")
                            append(caps.apertureLabel)
                        }
                    }
                }
                Text(
                    text = readout,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.isManualExposure) ScopeAmber else NothingWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("hud_live_readout")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (state.showHorizonLevel) {
                val isLevel = abs(state.rollDegrees) < 1.0f
                Text(
                    text = String.format(Locale.US, "%+.1f°", state.rollDegrees),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLevel) ScopeGreen else NothingSilver,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.widthIn(min = 44.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Row(
                modifier = Modifier
                    .clip(ChipShape)
                    .background(if (state.aeAfLocked) ScopeAmber.copy(alpha = 0.2f) else NothingSurface)
                    .border(1.dp, if (state.aeAfLocked) ScopeAmber else NothingBorder, ChipShape)
                    .clickable { actions.toggleAeAfLock() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("toggle_ae_af_lock"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (state.aeAfLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Блокировка AE/AF",
                    tint = if (state.aeAfLocked) ScopeAmber else NothingSilver,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = when {
                        state.aeAfLocked -> "AE/AF LOCK"
                        state.manualFocusDiopters != null -> "MF"
                        state.liveExposure.afLocked -> "AF ●"
                        else -> "AF-C"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.aeAfLocked) ScopeAmber else NothingWhite,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: scrollable quick settings
        FadingScrollRow(modifier = Modifier.fillMaxWidth(), spacing = 6.dp) {
            if (!state.isVideoMode) {
                HudQuickChip(
                    title = "ФОРМАТ",
                    value = state.photoFormat.badge,
                    active = state.photoFormat != PhotoFormat.JPEG,
                    accentColor = ScopeCyan,
                    tag = "chip_photo_format",
                    onClick = actions.cyclePhotoFormat
                )
                HudQuickChip(
                    title = "СЕРИЯ",
                    value = state.photoDriveMode.code,
                    active = state.photoDriveMode != PhotoDriveMode.SINGLE,
                    accentColor = ScopeAmber,
                    tag = "chip_photo_drive",
                    onClick = actions.cyclePhotoDrive
                )
                HudQuickChip(
                    title = "ТАЙМЕР",
                    value = state.selfTimer.label,
                    active = state.selfTimer != SelfTimer.OFF,
                    accentColor = ScopeGreen,
                    tag = "chip_self_timer",
                    onClick = actions.cycleSelfTimer
                )
                HudQuickChip(
                    title = "ВСПЫШКА",
                    value = if (caps.hasFlash) state.flashMode.code else "НЕТ",
                    active = caps.hasFlash && state.flashMode != FlashMode.OFF,
                    accentColor = ScopeAmber,
                    tag = "chip_flash",
                    enabled = caps.hasFlash,
                    onClick = actions.cycleFlash
                )
            } else {
                HudQuickChip(
                    title = "РАЗРЕШЕНИЕ",
                    value = state.videoResolution.label,
                    active = true,
                    accentColor = NothingWhite,
                    tag = "chip_video_res",
                    onClick = actions.cycleVideoResolution
                )
                HudQuickChip(
                    title = "КАДРЫ/С",
                    value = state.videoFps.label,
                    active = true,
                    accentColor = ScopeGreen,
                    tag = "chip_video_fps",
                    onClick = actions.cycleVideoFps
                )
                HudQuickChip(
                    title = "HDR",
                    value = if (!caps.hdrVideo) "НЕТ" else if (state.videoDynamicRange == VideoDynamicRange.HLG_10) "HLG10" else "SDR",
                    active = state.videoDynamicRange == VideoDynamicRange.HLG_10,
                    accentColor = ScopeCyan,
                    tag = "chip_video_hdr",
                    enabled = caps.hdrVideo,
                    onClick = actions.cycleDynamicRange
                )
                val stabAvailable = caps.videoStabilization || caps.previewStabilization
                HudQuickChip(
                    title = "СТАБ",
                    value = if (!stabAvailable) "НЕТ" else if (state.stabilization) "ON" else "OFF",
                    active = stabAvailable && state.stabilization,
                    accentColor = ScopeGreen,
                    tag = "chip_stabilization",
                    enabled = stabAvailable,
                    onClick = actions.toggleStabilization
                )
            }

            HudQuickChip(
                title = "ДЕ-СКВИЗ",
                value = state.anamorphicDeSqueeze.label,
                active = state.anamorphicDeSqueeze != AnamorphicDeSqueeze.NONE,
                accentColor = ScopeAmber,
                tag = "chip_anamorphic",
                onClick = actions.cycleAnamorphic
            )
            HudQuickChip(
                title = "РАМКА",
                value = state.aspectGuide.label,
                active = state.aspectGuide != FrameAspectGuide.OFF,
                accentColor = NothingWhite,
                tag = "chip_aspect_guide",
                onClick = actions.cycleAspectGuide
            )
            HudQuickChip(
                title = "ПИКИНГ",
                value = state.focusPeakingColor.labelRu,
                active = state.focusPeakingColor != FocusPeakingColor.OFF,
                accentColor = if (state.focusPeakingColor == FocusPeakingColor.OFF) NothingSilver else state.focusPeakingColor.color,
                tag = "chip_focus_peaking",
                onClick = actions.cyclePeaking
            )
            HudQuickChip(
                title = "ЗЕБРА",
                value = state.zebraThreshold.label,
                active = state.zebraThreshold != ZebraThreshold.OFF,
                accentColor = ScopeAmber,
                tag = "chip_zebra",
                onClick = actions.cycleZebra
            )
            HudQuickChip(
                title = "FALSE COLOR",
                value = if (state.showFalseColor) "ON" else "OFF",
                active = state.showFalseColor,
                accentColor = ScopeCyan,
                tag = "chip_false_color",
                onClick = actions.toggleFalseColor
            )
            HudQuickChip(
                title = "СЕТКА",
                value = state.gridOverlayMode.code,
                active = state.gridOverlayMode != GridOverlayMode.NONE,
                accentColor = NothingWhite,
                tag = "chip_grid_mode",
                onClick = actions.cycleGrid
            )
            HudQuickChip(
                title = "ГОРИЗОНТ",
                value = if (state.showHorizonLevel) "ON" else "OFF",
                active = state.showHorizonLevel,
                accentColor = ScopeGreen,
                tag = "chip_horizon_gyro",
                onClick = actions.toggleHorizon
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
    onClick: () -> Unit,
    enabled: Boolean = true
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
            color = when {
                !enabled -> NothingSilver.copy(alpha = 0.6f)
                active -> accentColor
                else -> NothingWhite
            },
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Control deck: lens row, parameter strip, dial drawer
// ---------------------------------------------------------------------------------------------

/** Lens pills come from the real camera (zoom range + physical lenses), plus the front toggle. */
@Composable
fun LensTurretRow(
    state: ProCameraUiState,
    actions: CameraDeckActions,
    modifier: Modifier = Modifier
) {
    val caps = state.capabilities
    val lenses = caps.lensOptions
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FadingScrollRow(modifier = Modifier.weight(1f), spacing = 6.dp) {
            if (lenses.isEmpty()) {
                Text(
                    text = if (caps.cameraLabel.isEmpty()) "Объективы появятся после запуска камеры" else caps.cameraLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            lenses.forEach { lens ->
                val selected = state.selectedLensId == lens.id
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (selected) NothingRed else NothingSurface)
                        .border(1.dp, if (selected) NothingRed else NothingBorder, PillShape)
                        .clickable { actions.selectLens(lens) }
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                        .testTag("lens_btn_${lens.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lens.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = NothingWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = state.zoomLabel,
            style = MaterialTheme.typography.labelLarge,
            color = ScopeAmber,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.testTag("zoom_ratio_label")
        )
        if (caps.hasFrontCamera || state.useFrontCamera) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (state.useFrontCamera) NothingWhite else NothingSurface)
                    .border(1.dp, if (state.useFrontCamera) NothingWhite else NothingBorder, CircleShape)
                    .clickable { actions.toggleFrontCamera() }
                    .testTag("lens_btn_front"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Переключить на фронтальную камеру",
                    tint = if (state.useFrontCamera) NothingBlack else NothingWhite,
                    modifier = Modifier.size(18.dp)
                )
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
    val isVideo = state.isVideoMode
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ParameterDialTile(
            label = "ISO",
            value = state.isoDisplayLabel,
            sub = if (state.isoValue <= 0) state.liveIsoLabel else null,
            isManual = state.isoValue > 0,
            isSelected = state.selectedDialTab == ManualDialTab.ISO,
            tag = "dial_tab_iso",
            onClick = { onToggleDialTab(ManualDialTab.ISO) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = if (isVideo && state.useShutterAngleInVideo) "УГОЛ" else "ЗАТВОР",
            value = state.shutterDisplayLabel,
            sub = if (state.shutterNs <= 0L) state.liveShutterLabel else null,
            isManual = state.shutterNs > 0L,
            isSelected = state.selectedDialTab == ManualDialTab.SHUTTER,
            tag = "dial_tab_shutter",
            onClick = { onToggleDialTab(ManualDialTab.SHUTTER) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = "ФОКУС",
            value = state.focusDisplayLabel,
            sub = null,
            isManual = state.manualFocusDiopters != null,
            isSelected = state.selectedDialTab == ManualDialTab.FOCUS,
            tag = "dial_tab_focus",
            onClick = { onToggleDialTab(ManualDialTab.FOCUS) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = "ББ",
            value = state.wbDisplayLabel,
            sub = null,
            isManual = state.awbPreset != AwbPreset.AUTO,
            isSelected = state.selectedDialTab == ManualDialTab.WB,
            tag = "dial_tab_wb",
            onClick = { onToggleDialTab(ManualDialTab.WB) },
            modifier = Modifier.weight(1f)
        )
        ParameterDialTile(
            label = "EV",
            value = String.format(Locale.US, "%+.1f", state.evCompensation),
            sub = null,
            isManual = abs(state.evCompensation) > 0.05f,
            isSelected = state.selectedDialTab == ManualDialTab.EV,
            tag = "dial_tab_ev",
            onClick = { onToggleDialTab(ManualDialTab.EV) },
            modifier = Modifier.weight(1f)
        )
        if (isVideo) {
            ParameterDialTile(
                label = "ЗВУК",
                value = if (!state.audioEnabled) "ВЫКЛ" else String.format(Locale.US, "%.0f", state.peakAudioDb),
                sub = if (state.audioEnabled) "dB" else null,
                isManual = !state.audioEnabled,
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
    sub: String?,
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
            .padding(horizontal = 4.dp, vertical = 7.dp)
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
        Text(
            text = sub ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun ManualDialDrawer(
    state: ProCameraUiState,
    actions: CameraDeckActions,
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
                contentPadding = PaddingValues(12.dp)
            ) {
                when (tab) {
                    ManualDialTab.ISO -> IsoDialDrawer(state = state, onSelectIso = actions.setIso)
                    ManualDialTab.SHUTTER -> ShutterDialDrawer(
                        state = state,
                        onSelectNs = actions.setShutterNs,
                        onSelectAngle = actions.setShutterAngle,
                        onToggleAngleMode = actions.toggleShutterAngleMode
                    )
                    ManualDialTab.FOCUS -> FocusAndPullerDrawer(
                        state = state,
                        onManualFocusChange = actions.setManualFocus,
                        onTriggerPull = actions.triggerFocusPull,
                        onSavePoint = actions.saveFocusPoint,
                        onSpeedChange = actions.setFocusPullSpeed
                    )
                    ManualDialTab.WB -> WhiteBalanceDrawer(
                        selected = state.awbPreset,
                        estimateK = state.scopeTelemetry.colorTempEstimateK,
                        onSelect = actions.setWhiteBalance
                    )
                    ManualDialTab.EV -> ExposureDrawer(state = state, onEvChange = actions.setEv)
                    ManualDialTab.AUDIO -> AudioDrawer(state = state, onToggleAudio = actions.toggleAudio)
                }
            }
        }
    }
}

private val IsoLadder = listOf(50, 100, 160, 200, 320, 400, 640, 800, 1600, 3200, 6400, 12800, 25600)

@Composable
private fun IsoDialDrawer(
    state: ProCameraUiState,
    onSelectIso: (Int) -> Unit
) {
    val caps = state.capabilities
    if (!caps.manualSensor) {
        UnsupportedNote(
            title = "Чувствительность ISO",
            text = "Эта камера не отдаёт ручное управление сенсором: ISO выбирает автоматика. Сейчас ISO ${state.liveIsoLabel}."
        )
        return
    }
    val options = buildList {
        add(0)
        if (IsoLadder.none { it == caps.isoMin }) add(caps.isoMin)
        addAll(IsoLadder.filter { it in caps.isoMin..caps.isoMax })
        if (IsoLadder.none { it == caps.isoMax }) add(caps.isoMax)
    }.distinct().sorted()
    Column {
        SectionLabel(
            text = "Чувствительность ISO · ${caps.isoMin}–${caps.isoMax}",
            trailing = if (state.isoValue == 0) "AUTO · сейчас ${state.liveIsoLabel}" else "РУЧНОЙ",
            trailingColor = if (state.isoValue == 0) ScopeGreen else ScopeAmber
        )
        Spacer(modifier = Modifier.height(8.dp))
        ChoiceChipRow(
            items = options,
            selected = state.isoValue,
            label = { if (it == 0) "AUTO" else it.toString() },
            onSelect = onSelectIso,
            tag = { "iso_opt_$it" },
            compact = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TactileRulerTickStrip(activeFraction = options.indexOf(state.isoValue).coerceAtLeast(0) / (options.size - 1f).coerceAtLeast(1f))
        if (state.isoValue > 0 && state.shutterNs <= 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Выдержка зафиксирована на текущем значении автоматики (${state.liveShutterLabel}): откройте ЗАТВОР, чтобы задать её вручную.",
                style = MaterialTheme.typography.bodySmall,
                color = NothingSilver
            )
        }
    }
}

private data class ShutterOption(val ns: Long, val label: String)

private val ShutterLadder: List<ShutterOption> = buildList {
    listOf(8000, 4000, 2000, 1000, 500, 250, 125, 100, 60, 50, 48, 30, 25, 15, 8, 4, 2).forEach {
        add(ShutterOption(1_000_000_000L / it, "1/$it"))
    }
    listOf(1, 2, 4, 8, 15, 30).forEach { add(ShutterOption(it * 1_000_000_000L, "$it\"")) }
}

@Composable
private fun ShutterDialDrawer(
    state: ProCameraUiState,
    onSelectNs: (Long) -> Unit,
    onSelectAngle: (Float) -> Unit,
    onToggleAngleMode: () -> Unit
) {
    val caps = state.capabilities
    if (!caps.manualSensor) {
        UnsupportedNote(
            title = "Выдержка",
            text = "Ручная выдержка недоступна на этой камере. Сейчас автоматика держит ${state.liveShutterLabel}."
        )
        return
    }
    val shutterAngles = listOf(45f, 90f, 144f, 172.8f, 180f, 270f, 360f)
    val isVideo = state.isVideoMode
    val angleMode = isVideo && state.useShutterAngleInVideo
    val options = buildList {
        add(ShutterOption(0L, "AUTO"))
        addAll(ShutterLadder.filter { it.ns in caps.exposureMinNs..caps.exposureMaxNs })
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(
                text = if (angleMode) "Угол затвора · правило 180°" else "Выдержка · ${formatExposure(caps.exposureMaxNs)} … ${formatExposure(caps.exposureMinNs)}",
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
            val angleOptions = listOf(-1f) + shutterAngles
            ChoiceChipRow(
                items = angleOptions,
                selected = if (state.shutterNs <= 0L) -1f else shutterAngles.firstOrNull { abs(state.shutterAngleDeg - it) < 1f } ?: -2f,
                label = { if (it < 0f) "AUTO" else String.format(Locale.US, "%.1f°", it) },
                onSelect = { if (it < 0f) onSelectNs(0L) else onSelectAngle(it) },
                tag = { "shutter_angle_${it.toInt()}" },
                compact = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (state.shutterNs > 0L) {
                    "${String.format(Locale.US, "%.1f°", state.shutterAngleDeg)} при ${state.videoFps.fps} к/с = ${formatExposure(state.shutterNs)}"
                } else {
                    "Автоматика: сейчас ${state.liveShutterLabel}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = NothingSilver
            )
        } else {
            ChoiceChipRow(
                items = options,
                selected = options.firstOrNull { it.ns == state.shutterNs } ?: ShutterOption(-1L, ""),
                label = { it.label },
                onSelect = { onSelectNs(it.ns) },
                tag = { "shutter_ns_${it.ns}" },
                compact = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            val idx = options.indexOfFirst { it.ns == state.shutterNs }.coerceAtLeast(0)
            TactileRulerTickStrip(activeFraction = idx / (options.size - 1f).coerceAtLeast(1f))
            if (state.shutterNs > 0L && state.isoValue <= 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ISO зафиксировано на значении автоматики (${state.liveIsoLabel}): задайте ISO вручную, если кадр слишком тёмный или светлый.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NothingSilver
                )
            }
        }
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
    val caps = state.capabilities
    if (!caps.manualFocus) {
        UnsupportedNote(
            title = "Фокус",
            text = "У этой камеры объектив с фиксированным фокусом или без ручного управления. Нажмите на кадр для фокуса по точке, удержите для блокировки AE/AF."
        )
        return
    }
    val isManual = state.manualFocusDiopters != null
    val maxDiopters = caps.minFocusDiopters.coerceAtLeast(0.5f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(
                text = "Фокус",
                trailing = if (!isManual) "AF · ${formatDiopters(state.liveExposure.focusDistanceDiopters)}" else null,
                trailingColor = ScopeGreen,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceChip(
                    label = "AF",
                    selected = !isManual,
                    accent = ScopeGreen,
                    onClick = { if (isManual) onManualFocusChange(null) },
                    compact = true
                )
                ChoiceChip(
                    label = "MF",
                    selected = isManual,
                    accent = ScopeAmber,
                    onClick = { if (!isManual) onManualFocusChange(state.liveExposure.focusDistanceDiopters.coerceIn(0f, maxDiopters)) },
                    tag = "btn_toggle_af_mf",
                    compact = true
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LabelledSlider(
            label = "Дистанция · ∞ → макро",
            valueText = if (isManual) state.focusDisplayLabel else formatDiopters(state.liveExposure.focusDistanceDiopters),
            value = (state.manualFocusDiopters ?: state.liveExposure.focusDistanceDiopters).coerceIn(0f, maxDiopters),
            onValueChange = { onManualFocusChange(it) },
            valueRange = 0f..maxDiopters,
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
                        Text(text = pt.slot, style = MaterialTheme.typography.labelLarge, color = ScopeAmber)
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
            listOf(600L to "0.6 с", 1400L to "1.4 с КИНО", 2800L to "2.8 с").forEach { (ms, lbl) ->
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
    selected: AwbPreset,
    estimateK: Int,
    onSelect: (AwbPreset) -> Unit
) {
    Column {
        SectionLabel(
            text = "Баланс белого",
            trailing = if (estimateK > 0) "в кадре ≈ ${estimateK}K" else null,
            trailingColor = ScopeGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        ChoiceChipRow(
            items = AwbPreset.entries,
            selected = selected,
            label = { if (it == AwbPreset.AUTO) "AWB" else "${it.code} ${it.labelRu.uppercase()}" },
            onSelect = onSelect,
            tag = { "wb_preset_${it.name}" },
            compact = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Пресеты переключают режимы баланса белого самой камеры: они работают на любом телефоне, в отличие от произвольных кельвинов.",
            style = MaterialTheme.typography.bodySmall,
            color = NothingSilver
        )
    }
}

@Composable
private fun ExposureDrawer(
    state: ProCameraUiState,
    onEvChange: (Float) -> Unit
) {
    val caps = state.capabilities
    val range = caps.evRange
    if (range <= 0f || state.isManualExposure) {
        UnsupportedNote(
            title = "Экспокоррекция",
            text = if (state.isManualExposure) {
                "В ручном режиме экспокоррекция не действует: яркость задают ISO и выдержка."
            } else {
                "Эта камера не поддерживает экспокоррекцию."
            }
        )
        return
    }
    val steps = ((2f * range) / caps.evStep).roundToInt() - 1
    Column {
        LabelledSlider(
            label = "Экспокоррекция · шаг ${String.format(Locale.US, "%.2f", caps.evStep)} EV",
            valueText = String.format(Locale.US, "EV %+.1f", state.evCompensation),
            value = state.evCompensation,
            onValueChange = onEvChange,
            valueRange = -range..range,
            steps = steps.coerceIn(0, 60),
            accent = ScopeAmber,
            tag = "slider_ev_compensation"
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(-2f, -1f, 0f, 1f, 2f).filter { abs(it) <= range + 0.01f }.forEach { ev ->
                ChoiceChip(
                    label = if (ev == 0f) "0" else String.format(Locale.US, "%+.0f", ev),
                    selected = abs(state.evCompensation - ev) < 0.05f,
                    accent = ScopeAmber,
                    onClick = { onEvChange(ev) },
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Сейчас: ISO ${state.liveIsoLabel} · ${state.liveShutterLabel}" +
                if (state.liveExposure.aeConverged) " · экспозиция стабильна" else "",
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver
        )
    }
}

@Composable
private fun AudioDrawer(
    state: ProCameraUiState,
    onToggleAudio: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(text = "Звук видео", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(ChipShape)
                    .background(if (state.audioEnabled) ScopeGreen.copy(alpha = 0.18f) else NothingSurface)
                    .border(1.dp, if (state.audioEnabled) ScopeGreen else NothingBorder, ChipShape)
                    .clickable { onToggleAudio() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("btn_toggle_audio"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (state.audioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    tint = if (state.audioEnabled) ScopeGreen else NothingSilver,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (state.audioEnabled) "МИКРОФОН ВКЛ" else "БЕЗ ЗВУКА",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.audioEnabled) ScopeGreen else NothingSilver
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (!state.audioEnabled) {
            Text(
                text = "Видео запишется без звуковой дорожки.",
                style = MaterialTheme.typography.bodySmall,
                color = NothingSilver
            )
        } else if (!state.audioAvailable) {
            Text(
                text = "Нет доступа к микрофону: выдайте разрешение на запись звука в настройках приложения.",
                style = MaterialTheme.typography.bodySmall,
                color = ScopeAmber
            )
        } else {
            DeckAudioBar("L", state.leftAudioDb)
            Spacer(modifier = Modifier.height(6.dp))
            DeckAudioBar("R", state.rightAudioDb)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = String.format(Locale.US, "Пик %.0f dBFS · стерео 48 кГц · уровень задаёт система", state.peakAudioDb),
                style = MaterialTheme.typography.labelSmall,
                color = if (state.peakAudioDb > -3f) NothingRed else NothingSilver
            )
        }
    }
}

@Composable
private fun DeckAudioBar(channel: String, db: Float) {
    val norm = ((db + 60f) / 60f).coerceIn(0.02f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = channel, style = MaterialTheme.typography.labelSmall, color = NothingWhite, modifier = Modifier.width(16.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NothingObsidian)
        ) {
            val segments = 30
            val segW = size.width / segments
            val active = (norm * segments).toInt()
            for (i in 0 until segments) {
                val c = when {
                    i >= 27 -> NothingRed
                    i >= 22 -> ScopeAmber
                    else -> ScopeGreen
                }
                drawRect(
                    color = if (i < active) c else c.copy(alpha = 0.14f),
                    topLeft = Offset(i * segW + 1f, 0f),
                    size = Size((segW - 2f).coerceAtLeast(1f), size.height)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format(Locale.US, "%.0f", db),
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun UnsupportedNote(title: String, text: String) {
    Column {
        SectionLabel(text = title, trailing = "АВТО", trailingColor = ScopeGreen)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = NothingSilver)
    }
}

private fun formatDiopters(d: Float): String = com.example.viewmodel.formatDiopters(d)

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
