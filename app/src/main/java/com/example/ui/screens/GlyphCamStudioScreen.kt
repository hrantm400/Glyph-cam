package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.camera.ProCameraController
import com.example.data.CameraPresetEntity
import com.example.model.AnamorphicDeSqueeze
import com.example.model.AppMode
import com.example.model.FrameAspectGuide
import com.example.model.LutProfile
import com.example.model.PhotoDriveMode
import com.example.model.PhotoFormat
import com.example.model.VideoCodecProfile
import com.example.model.VideoFps
import com.example.model.VideoResolution
import com.example.ui.components.ChipShape
import com.example.ui.components.FadingScrollRow
import com.example.ui.components.FloatingScopesHudDock
import com.example.ui.components.LensTurretRow
import com.example.ui.components.ManualDialDrawer
import com.example.ui.components.ParameterStrip
import com.example.ui.components.PillShape
import com.example.ui.components.TopTelemetryHudBar
import com.example.ui.components.ViewfinderOpticalOverlays
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
import com.example.viewmodel.ProCameraUiState
import com.example.viewmodel.ProCameraViewModel

private val AppMode.isCameraMode: Boolean
    get() = this == AppMode.PHOTO_PRO || this == AppMode.CINE_VIDEO

private val ViewfinderShape = RoundedCornerShape(20.dp)

@Composable
fun GlyphCamStudioScreen(
    viewModel: ProCameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val capturedShots by viewModel.capturedShots.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true || hasCameraPermission
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] == true || hasAudioPermission
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraController = remember {
        ProCameraController(
            context = context,
            scope = coroutineScope,
            onScopeTelemetryUpdated = viewModel::updateScopeTelemetry,
            onHorizonUpdated = viewModel::updateHorizon,
            onAudioLevelsUpdated = viewModel::updateAudioLevels,
            onRecordingStateChanged = viewModel::updateRecordingState,
            onCaptureSaved = viewModel::recordCapturedShot,
            onStatusMessage = viewModel::updateStatusBanner
        )
    }

    DisposableEffect(lifecycleOwner, hasAudioPermission, state.audioGainDb, state.windCutEnabled) {
        cameraController.startSensorsAndAudio(
            hasAudioPermission = hasAudioPermission,
            audioGainDb = state.audioGainDb,
            windCutEnabled = state.windCutEnabled
        )
        onDispose {
            cameraController.stopSensorsAndAudio()
        }
    }

    // Bind CameraX whenever camera permission or selected lens changes
    LaunchedEffect(hasCameraPermission, state.selectedLens, state.zebraThreshold) {
        if (hasCameraPermission) {
            cameraController.bindCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                lens = state.selectedLens,
                zebraThreshold = state.zebraThreshold.irePercent
            )
        }
    }

    // Apply Camera2Interop manual exposure, focus, WB, zoom, and Glyph Torch hardware sync
    LaunchedEffect(
        state.isoValue,
        state.shutterDenominator,
        state.wbKelvin,
        state.evCompensation,
        state.manualFocusDiopters,
        state.fineZoomRatio,
        state.glyphTorchHardwareSync
    ) {
        cameraController.applyHardwareParameters(
            isoValue = state.isoValue,
            shutterDenominator = state.shutterDenominator,
            wbKelvin = state.wbKelvin,
            evCompensation = state.evCompensation,
            focusDistanceDiopters = state.manualFocusDiopters,
            zoomRatio = state.fineZoomRatio,
            torchEnabled = state.glyphTorchHardwareSync
        )
    }

    // Studios (Glyph / LUT / Vault) open on top of the camera and return to the mode they came from
    var lastCameraMode by rememberSaveable { mutableStateOf(AppMode.CINE_VIDEO) }
    LaunchedEffect(state.activeMode) {
        if (state.activeMode.isCameraMode) lastCameraMode = state.activeMode
    }
    val backToCamera: () -> Unit = { viewModel.setAppMode(lastCameraMode) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        val studioModifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()

        when (state.activeMode) {
            AppMode.PHOTO_PRO, AppMode.CINE_VIDEO -> {
                CameraWorkspace(
                    state = state,
                    presets = presets,
                    shotCount = capturedShots.size,
                    hasCameraPermission = hasCameraPermission,
                    previewView = previewView,
                    viewModel = viewModel,
                    onRequestPermissions = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    onTapViewfinder = { normX, normY, pxX, pxY, longPress ->
                        viewModel.setTapFocusPoint(normX, normY, toggleLock = longPress)
                        cameraController.triggerTapToFocus(previewView, pxX, pxY, longPress)
                    },
                    onShutterTrigger = {
                        if (state.activeMode == AppMode.CINE_VIDEO) {
                            cameraController.toggleVideoRecording()
                        } else {
                            viewModel.triggerShutterFlash()
                            cameraController.capturePhoto(
                                burstCount = state.photoDriveMode.burstCount,
                                onFinished = {}
                            )
                        }
                    }
                )
            }
            AppMode.GLYPH_STUDIO -> {
                GlyphStudioScreen(
                    state = state,
                    onSelectGlyphMode = viewModel::selectGlyphLightingMode,
                    onToggleSegment = viewModel::toggleGlyphSegment,
                    onBrightnessChange = viewModel::setGlyphBrightness,
                    onToggleTorchHardware = viewModel::toggleGlyphTorchHardware,
                    onBackToCamera = backToCamera,
                    modifier = studioModifier
                )
            }
            AppMode.LUT_LAB -> {
                LutAndPresetsScreen(
                    state = state,
                    presets = presets,
                    onSelectLut = viewModel::selectLutProfile,
                    onUpdateGrade = viewModel::updateColorGrading,
                    onToggleSplitCompare = viewModel::toggleSplitScreenLut,
                    onResetGrade = viewModel::resetColorGrade,
                    onSavePreset = viewModel::saveCurrentAsPreset,
                    onApplyPreset = viewModel::applyPreset,
                    onDeletePreset = viewModel::deletePreset,
                    onBackToCamera = backToCamera,
                    modifier = studioModifier
                )
            }
            AppMode.MEDIA_VAULT -> {
                MediaVaultScreen(
                    shots = capturedShots,
                    inspectedShot = state.inspectShot,
                    onSelectShot = viewModel::inspectCapturedShot,
                    onDeleteShot = viewModel::deleteCapturedShot,
                    onBackToCamera = backToCamera,
                    modifier = studioModifier
                )
            }
        }
    }
}

@Composable
private fun CameraWorkspace(
    state: ProCameraUiState,
    presets: List<CameraPresetEntity>,
    shotCount: Int,
    hasCameraPermission: Boolean,
    previewView: PreviewView,
    viewModel: ProCameraViewModel,
    onRequestPermissions: () -> Unit,
    onTapViewfinder: (Float, Float, Float, Float, Boolean) -> Unit,
    onShutterTrigger: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Top telemetry + quick settings
        TopTelemetryHudBar(
            state = state,
            onCycleFormatOrRes = {
                if (state.activeMode == AppMode.PHOTO_PRO) {
                    val list = PhotoFormat.entries
                    viewModel.setPhotoFormat(list[(list.indexOf(state.photoFormat) + 1) % list.size])
                } else {
                    val list = VideoResolution.entries
                    viewModel.setVideoResolution(list[(list.indexOf(state.videoResolution) + 1) % list.size])
                }
            },
            onCycleFpsOrDrive = {
                if (state.activeMode == AppMode.PHOTO_PRO) {
                    val list = PhotoDriveMode.entries
                    viewModel.setPhotoDriveMode(list[(list.indexOf(state.photoDriveMode) + 1) % list.size])
                } else {
                    val list = VideoFps.entries
                    viewModel.setVideoFps(list[(list.indexOf(state.videoFps) + 1) % list.size])
                }
            },
            onCycleCodecOrNd = {
                if (state.activeMode == AppMode.PHOTO_PRO) {
                    viewModel.cycleNdFilter()
                } else {
                    val list = VideoCodecProfile.entries
                    viewModel.setVideoCodec(list[(list.indexOf(state.videoCodec) + 1) % list.size])
                }
            },
            onCycleAnamorphic = {
                val list = AnamorphicDeSqueeze.entries
                viewModel.setAnamorphicDeSqueeze(list[(list.indexOf(state.anamorphicDeSqueeze) + 1) % list.size])
            },
            onCycleAspectGuide = {
                val list = FrameAspectGuide.entries
                viewModel.setAspectGuide(list[(list.indexOf(state.aspectGuide) + 1) % list.size])
            },
            onCycleZebra = viewModel::cycleZebraThreshold,
            onCyclePeaking = viewModel::cycleFocusPeaking,
            onToggleFalseColor = viewModel::toggleFalseColor,
            onCycleGrid = viewModel::cycleGridOverlay,
            onToggleHorizon = viewModel::toggleHorizonLevel,
            onToggleAeAfLock = viewModel::toggleAeAfLock,
            modifier = Modifier.statusBarsPadding()
        )

        // 2. Viewfinder
        Viewfinder(
            state = state,
            presets = presets,
            hasCameraPermission = hasCameraPermission,
            previewView = previewView,
            onRequestPermissions = onRequestPermissions,
            onTapViewfinder = onTapViewfinder,
            onApplyPreset = viewModel::applyPreset,
            onToggleHistogram = viewModel::toggleHistogram,
            onToggleWaveform = viewModel::toggleWaveform,
            onToggleVectorscope = viewModel::toggleVectorscope,
            onOpenLutLab = { viewModel.setAppMode(AppMode.LUT_LAB) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        // 3. Control deck: dial drawer, lenses, parameters, mode switch and shutter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NothingBlack)
                .padding(horizontal = 12.dp)
                .padding(top = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            ManualDialDrawer(
                state = state,
                onIsoChange = viewModel::setIso,
                onShutterDenomChange = viewModel::setShutterDenominator,
                onShutterAngleChange = viewModel::setShutterAngle,
                onToggleShutterAngleMode = viewModel::toggleShutterAngleMode,
                onManualFocusChange = viewModel::setManualFocus,
                onTriggerFocusPull = viewModel::triggerRackFocusPull,
                onSaveFocusPullPoint = viewModel::saveFocusPullPoint,
                onFocusPullSpeedChange = viewModel::setFocusPullSpeed,
                onWbChange = viewModel::setWhiteBalance,
                onEvChange = viewModel::setEvCompensation,
                onMeteringChange = viewModel::setMeteringMode,
                onStabilizationChange = viewModel::setStabilizationMode,
                onAudioGainChange = viewModel::setAudioGain,
                onToggleWindCut = viewModel::toggleWindCut,
                onAudioDirectionChange = viewModel::setAudioDirection
            )

            LensTurretRow(
                state = state,
                onSelectLens = viewModel::selectLens
            )

            Spacer(modifier = Modifier.height(8.dp))

            ParameterStrip(
                state = state,
                onToggleDialTab = viewModel::toggleDialTab
            )

            Spacer(modifier = Modifier.height(12.dp))

            ShutterDeckRow(
                state = state,
                shotCount = shotCount,
                onSelectMode = viewModel::setAppMode,
                onShutterTrigger = onShutterTrigger
            )
        }
    }
}

@Composable
private fun Viewfinder(
    state: ProCameraUiState,
    presets: List<CameraPresetEntity>,
    hasCameraPermission: Boolean,
    previewView: PreviewView,
    onRequestPermissions: () -> Unit,
    onTapViewfinder: (Float, Float, Float, Float, Boolean) -> Unit,
    onApplyPreset: (CameraPresetEntity) -> Unit,
    onToggleHistogram: () -> Unit,
    onToggleWaveform: () -> Unit,
    onToggleVectorscope: () -> Unit,
    onOpenLutLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ViewfinderShape)
            .background(NothingObsidian)
            .border(
                width = if (state.isRecording) 2.dp else 1.dp,
                color = if (state.isRecording) NothingRed else NothingBorder.copy(alpha = 0.6f),
                shape = ViewfinderShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val nx = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                        val ny = (offset.y / size.height).coerceIn(0.05f, 0.95f)
                        onTapViewfinder(nx, ny, offset.x, offset.y, false)
                    },
                    onLongPress = { offset ->
                        val nx = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                        val ny = (offset.y / size.height).coerceIn(0.05f, 0.95f)
                        onTapViewfinder(nx, ny, offset.x, offset.y, true)
                    }
                )
            }
            .testTag("camera_viewfinder_box")
    ) {
        // Base layer: calibration scene, visible until the camera stream arrives
        OpticalReferenceSceneCanvas(
            state = state,
            modifier = Modifier.fillMaxSize()
        )

        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.96f }
            )
        }

        // Live LUT tint
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lutAccent = Color(state.activeLut.accentHex)
            val tintAlpha = when (state.activeLut) {
                LutProfile.NOTHING_STD -> 0.0f
                LutProfile.N_LOG_FLAT -> 0.16f
                LutProfile.NOTHING_MONO_RED -> 0.25f
                else -> 0.12f
            }
            if (tintAlpha > 0f) {
                val tintWidth = if (state.splitScreenLutCompare) size.width * 0.5f else size.width
                drawRect(
                    color = lutAccent.copy(alpha = tintAlpha),
                    topLeft = Offset(0f, 0f),
                    size = Size(tintWidth, size.height)
                )
            }
        }

        ViewfinderOpticalOverlays(
            telemetry = state.scopeTelemetry,
            gridMode = state.gridOverlayMode,
            aspectGuide = state.aspectGuide,
            maskOpacity = state.aspectMaskOpacity,
            zebraThreshold = state.zebraThreshold,
            peakingColor = state.focusPeakingColor,
            showFalseColor = state.showFalseColor,
            showHorizon = state.showHorizonLevel,
            rollDegrees = state.rollDegrees,
            pitchDegrees = state.pitchDegrees,
            tapFocusPoint = state.tapFocusNormalized,
            aeAfLocked = state.aeAfLocked,
            splitLutActive = state.splitScreenLutCompare,
            lutName = state.activeLut.code,
            filmGrainAmount = state.filmGrainAmount,
            modifier = Modifier.fillMaxSize()
        )

        FloatingScopesHudDock(
            telemetry = state.scopeTelemetry,
            showHistogram = state.showHistogramRgb,
            showWaveform = state.showWaveformScope,
            showVectorscope = state.showVectorscope,
            showAudio = state.activeMode == AppMode.CINE_VIDEO,
            leftAudioDb = state.leftAudioDb,
            rightAudioDb = state.rightAudioDb,
            peakAudioDb = state.peakAudioDb,
            onToggleHistogram = onToggleHistogram,
            onToggleWaveform = onToggleWaveform,
            onToggleVectorscope = onToggleVectorscope,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )

        if (!hasCameraPermission) {
            PermissionCard(
                onRequestPermissions = onRequestPermissions,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp)
            )
        }

        ViewfinderBottomStrip(
            state = state,
            presets = presets,
            onApplyPreset = onApplyPreset,
            onToggleHistogram = onToggleHistogram,
            onToggleWaveform = onToggleWaveform,
            onToggleVectorscope = onToggleVectorscope,
            onOpenLutLab = onOpenLutLab,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PermissionCard(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NothingBlack.copy(alpha = 0.9f))
            .border(1.dp, NothingBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = NothingRed,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.permission_camera_title),
            style = MaterialTheme.typography.titleMedium,
            color = NothingWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.permission_camera_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = NothingSilver,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = NothingWhite),
            shape = PillShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_grant_camera_permissions")
        ) {
            Text(
                text = stringResource(R.string.grant_permissions_btn),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ViewfinderBottomStrip(
    state: ProCameraUiState,
    presets: List<CameraPresetEntity>,
    onApplyPreset: (CameraPresetEntity) -> Unit,
    onToggleHistogram: () -> Unit,
    onToggleWaveform: () -> Unit,
    onToggleVectorscope: () -> Unit,
    onOpenLutLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, NothingBlack.copy(alpha = 0.9f))
                )
            )
            .padding(horizontal = 12.dp)
            .padding(top = 28.dp, bottom = 10.dp)
    ) {
        FadingScrollRow(modifier = Modifier.fillMaxWidth(), spacing = 6.dp) {
            // Active LUT → opens the LUT lab
            val lutColor = Color(state.activeLut.accentHex)
            Row(
                modifier = Modifier
                    .clip(ChipShape)
                    .background(lutColor.copy(alpha = 0.16f))
                    .border(1.dp, lutColor.copy(alpha = 0.8f), ChipShape)
                    .clickable { onOpenLutLab() }
                    .padding(horizontal = 8.dp, vertical = 7.dp)
                    .testTag("mini_pill_lut"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "LUT и цвет",
                    tint = lutColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = state.activeLut.code,
                    style = MaterialTheme.typography.labelLarge,
                    color = lutColor
                )
            }

            MiniTogglePill("HIST", state.showHistogramRgb, ScopeGreen, onToggleHistogram)
            MiniTogglePill("WFM", state.showWaveformScope, ScopeGreen, onToggleWaveform)
            MiniTogglePill("VEC", state.showVectorscope, ScopeCyan, onToggleVectorscope)

            // Quick recall presets C1–C4 (least critical, so they are the part that scrolls off)
            presets.take(4).forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(NothingBlack.copy(alpha = 0.7f))
                        .border(1.dp, NothingBorder, ChipShape)
                        .clickable { onApplyPreset(preset) }
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                        .testTag("quick_preset_${preset.slotCode}")
                ) {
                    Text(
                        text = preset.slotCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = ScopeAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.statusBanner,
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniTogglePill(
    label: String,
    active: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(ChipShape)
            .background(if (active) color.copy(alpha = 0.2f) else NothingBlack.copy(alpha = 0.7f))
            .border(1.dp, if (active) color else NothingBorder, ChipShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag("mini_pill_$label")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) color else NothingSilver
        )
    }
}

@Composable
private fun OpticalReferenceSceneCanvas(
    state: ProCameraUiState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Simulated exposure & WB atmosphere
        val evFactor = Math.pow(2.0, (state.evCompensation * 0.35).toDouble()).toFloat().coerceIn(0.4f, 1.8f)
        val warmBias = ((state.wbKelvin - 5500) / 15000f).coerceIn(-0.2f, 0.2f)

        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(
                        (0.08f * evFactor + warmBias.coerceAtLeast(0f)).coerceIn(0f, 1f),
                        (0.10f * evFactor).coerceIn(0f, 1f),
                        (0.16f * evFactor - warmBias.coerceAtLeast(0f)).coerceIn(0f, 1f)
                    ),
                    Color(0xFF09090D),
                    Color(0xFF14141C)
                )
            )
        )

        val horizonY = h * 0.52f
        drawLine(
            color = ScopeCyan.copy(alpha = 0.18f),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // Bokeh reference orbs responding to focus distance
        val blurScale = if (state.manualFocusDiopters != null) {
            (1f + kotlin.math.abs(state.manualFocusDiopters - 2.2f) * 0.22f).coerceIn(1f, 2.4f)
        } else 1.0f

        drawCircle(
            color = NothingRed.copy(alpha = 0.26f),
            radius = 30.dp.toPx() * blurScale,
            center = Offset(w * 0.28f, h * 0.38f)
        )
        drawCircle(
            color = ScopeCyan.copy(alpha = 0.22f),
            radius = 36.dp.toPx() * blurScale,
            center = Offset(w * 0.72f, h * 0.42f)
        )
        drawCircle(
            color = ScopeAmber.copy(alpha = 0.24f),
            radius = 24.dp.toPx() * blurScale,
            center = Offset(w * 0.52f, h * 0.34f)
        )
    }
}

@Composable
private fun ShutterDeckRow(
    state: ProCameraUiState,
    shotCount: Int,
    onSelectMode: (AppMode) -> Unit,
    onShutterTrigger: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: media vault
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            DeckQuickTile(
                icon = Icons.Default.PhotoLibrary,
                title = "АРХИВ",
                value = shotCount.toString(),
                accent = ScopeCyan,
                onClick = { onSelectMode(AppMode.MEDIA_VAULT) },
                tag = "btn_open_media_vault"
            )
        }

        // Center: mode switch + shutter
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ModeSwitch(activeMode = state.activeMode, onSelectMode = onSelectMode)
            Spacer(modifier = Modifier.height(10.dp))
            ShutterButton(state = state, onClick = onShutterTrigger)
        }

        // Right: Glyph light studio
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val glyphOn = state.activeGlyphSegments.isNotEmpty()
            DeckQuickTile(
                icon = Icons.Default.FlashOn,
                title = "GLYPH",
                value = "${state.activeGlyphSegments.size}/11",
                accent = if (glyphOn) ScopeAmber else NothingSilver,
                onClick = { onSelectMode(AppMode.GLYPH_STUDIO) },
                tag = "btn_open_glyph_studio"
            )
        }
    }
}

@Composable
private fun ModeSwitch(
    activeMode: AppMode,
    onSelectMode: (AppMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(NothingSurface)
            .border(1.dp, NothingBorder, PillShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ModeSegment(
            label = "ФОТО",
            icon = Icons.Outlined.PhotoCamera,
            selected = activeMode == AppMode.PHOTO_PRO,
            onClick = { onSelectMode(AppMode.PHOTO_PRO) },
            tag = "mode_tab_PHOTO_PRO"
        )
        ModeSegment(
            label = "ВИДЕО",
            icon = Icons.Default.Videocam,
            selected = activeMode == AppMode.CINE_VIDEO,
            onClick = { onSelectMode(AppMode.CINE_VIDEO) },
            tag = "mode_tab_CINE_VIDEO"
        )
    }
}

@Composable
private fun ModeSegment(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) NothingWhite else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) NothingBlack else NothingSilver,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) NothingBlack else NothingSilver
        )
    }
}

@Composable
private fun ShutterButton(
    state: ProCameraUiState,
    onClick: () -> Unit
) {
    val isVideoMode = state.activeMode == AppMode.CINE_VIDEO
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(3.dp, NothingWhite, CircleShape)
            .clickable { onClick() }
            .padding(7.dp)
            .testTag("shutter_record_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoMode) {
            Box(
                modifier = Modifier
                    .size(if (state.isRecording) 28.dp else 58.dp)
                    .clip(if (state.isRecording) RoundedCornerShape(6.dp) else CircleShape)
                    .background(NothingRed)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(NothingWhite),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NothingRed)
                )
            }
        }
    }
}

@Composable
private fun DeckQuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NothingSurface)
            .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = NothingWhite
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = NothingSilver
        )
    }
}
