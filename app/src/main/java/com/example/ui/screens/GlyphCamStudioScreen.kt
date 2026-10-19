package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import com.example.ui.components.FloatingScopesHudDock
import com.example.ui.components.LensAndManualControlDeck
import com.example.ui.components.TopTelemetryHudBar
import com.example.ui.components.ViewfinderOpticalOverlays
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
import com.example.viewmodel.ProCameraUiState
import com.example.viewmodel.ProCameraViewModel

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Route between Live Viewfinder (Photo Pro / Cine Video) and Sub-Studios (Glyph / LUT / Vault)
        Box(modifier = Modifier.weight(1f)) {
            when (state.activeMode) {
                AppMode.PHOTO_PRO, AppMode.CINE_VIDEO -> {
                    ProViewfinderWorkspace(
                        state = state,
                        presets = presets,
                        hasCameraPermission = hasCameraPermission,
                        previewView = previewView,
                        onRequestPermissions = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                        onTapViewfinder = { normX, normY, pxX, pxY, longPress ->
                            viewModel.setTapFocusPoint(normX, normY, toggleLock = longPress)
                            cameraController.triggerTapToFocus(previewView, pxX, pxY, longPress)
                        },
                        onApplyPreset = viewModel::applyPreset,
                        onToggleHistogram = viewModel::toggleHistogram,
                        onToggleWaveform = viewModel::toggleWaveform,
                        onToggleVectorscope = viewModel::toggleVectorscope,
                        onCycleLut = {
                            val entries = LutProfile.entries
                            val next = entries[(entries.indexOf(state.activeLut) + 1) % entries.size]
                            viewModel.selectLutProfile(next)
                        },
                        viewModel = viewModel
                    )
                }
                AppMode.GLYPH_STUDIO -> {
                    GlyphStudioScreen(
                        state = state,
                        onSelectGlyphMode = viewModel::selectGlyphLightingMode,
                        onToggleSegment = viewModel::toggleGlyphSegment,
                        onBrightnessChange = viewModel::setGlyphBrightness,
                        onToggleTorchHardware = viewModel::toggleGlyphTorchHardware,
                        onBackToCamera = { viewModel.setAppMode(AppMode.CINE_VIDEO) }
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
                        onApplyPreset = {
                            viewModel.applyPreset(it)
                        },
                        onDeletePreset = viewModel::deletePreset,
                        onBackToCamera = { viewModel.setAppMode(AppMode.CINE_VIDEO) }
                    )
                }
                AppMode.MEDIA_VAULT -> {
                    MediaVaultScreen(
                        shots = capturedShots,
                        inspectedShot = state.inspectShot,
                        onSelectShot = viewModel::inspectCapturedShot,
                        onDeleteShot = viewModel::deleteCapturedShot,
                        onBackToCamera = { viewModel.setAppMode(AppMode.CINE_VIDEO) }
                    )
                }
            }
        }

        // Persistent Bottom Shutter Deck & 5-Mode Switcher
        BottomShutterAndModeBar(
            state = state,
            shotCount = capturedShots.size,
            onSelectMode = viewModel::setAppMode,
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
}

@Composable
private fun ProViewfinderWorkspace(
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
    onCycleLut: () -> Unit,
    viewModel: ProCameraViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Top Cinema Telemetry & Assist Bar
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
            onToggleAeAfLock = viewModel::toggleAeAfLock
        )

        // 2. Central Optical Viewfinder + Live Scopes + C1-C4 Quick Recall Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingObsidian)
                .border(
                    width = if (state.isRecording) 2.dp else 1.dp,
                    color = if (state.isRecording) NothingRed else NothingBorder,
                    shape = RoundedCornerShape(12.dp)
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
            val anamorphicScaleX by animateFloatAsState(
                targetValue = if (state.anamorphicDeSqueeze != AnamorphicDeSqueeze.NONE) 1.0f else 1.0f,
                label = "anamorphicScale"
            )

            // Base Layer: Optical Calibration Scene (always visible underneath or when camera permission is pending)
            OpticalReferenceSceneCanvas(
                state = state,
                modifier = Modifier.fillMaxSize()
            )

            // Hardware CameraX PreviewView when CAMERA permission is granted
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = anamorphicScaleX
                            alpha = 0.92f
                        }
                )
            }

            // Live 3D LUT Color Grading Tint Layer over Viewfinder
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

            // Optical Overlays: False Color, Zebras, Focus Peaking, Aspect Gate, Grids, Gyro Horizon, Focus Reticle
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

            // Top Floating Scopes & 24-Bit Audio Meters inside Viewfinder
            FloatingScopesHudDock(
                telemetry = state.scopeTelemetry,
                showHistogram = state.showHistogramRgb,
                showWaveform = state.showWaveformScope,
                showVectorscope = state.showVectorscope,
                leftAudioDb = state.leftAudioDb,
                rightAudioDb = state.rightAudioDb,
                peakAudioDb = state.peakAudioDb,
                onCycleScopeVisibility = {
                    onToggleVectorscope()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Permission Banner if CAMERA is not granted yet (non-blocking so user can still use all pro tools)
            if (!hasCameraPermission) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingBlack.copy(alpha = 0.88f))
                        .border(1.dp, NothingRed, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_camera_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = NothingWhite
                        )
                        Text(
                            text = "Симулятор IMX890 активен. Разрешите доступ к камере для живого потока.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NothingSilver,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        modifier = Modifier.testTag("btn_grant_camera_permissions")
                    ) {
                        Text("ВКЛ КАМЕРУ", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Bottom Floating Bar inside Viewfinder: C1-C4 Quick Presets + Scope & LUT Pills + Status Ticker
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, NothingBlack.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick C1-C4 Rig Preset Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        presets.take(4).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(NothingBlack.copy(alpha = 0.78f))
                                    .border(1.dp, NothingBorder, RoundedCornerShape(5.dp))
                                    .clickable { onApplyPreset(preset) }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                    .testTag("quick_preset_${preset.slotCode}")
                            ) {
                                Text(
                                    text = "${preset.slotCode} ${preset.lutId.take(4).uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ScopeAmber,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Scope & LUT Quick Toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MiniTogglePill("HIST", state.showHistogramRgb, ScopeGreen, onToggleHistogram)
                        MiniTogglePill("WFM", state.showWaveformScope, ScopeGreen, onToggleWaveform)
                        MiniTogglePill("VEC", state.showVectorscope, ScopeCyan, onToggleVectorscope)
                        MiniTogglePill(
                            label = state.activeLut.code,
                            active = state.activeLut != LutProfile.NOTHING_STD,
                            color = Color(state.activeLut.accentHex),
                            onClick = onCycleLut
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = state.statusBanner,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingSilver,
                    fontSize = 9.sp
                )
            }
        }

        // 3. Nothing Lens Turret + 6-Channel Manual Dial Deck
        LensAndManualControlDeck(
            state = state,
            onSelectLens = viewModel::selectLens,
            onFineZoomChange = viewModel::setFineZoom,
            onToggleDialTab = viewModel::toggleDialTab,
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
            .clip(RoundedCornerShape(5.dp))
            .background(if (active) color.copy(alpha = 0.22f) else NothingBlack.copy(alpha = 0.75f))
            .border(1.dp, if (active) color else NothingBorder, RoundedCornerShape(5.dp))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .testTag("mini_pill_$label")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) color else NothingSilver,
            fontSize = 9.sp
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

        // Perspective cinema studio grid & optical test targets
        val horizonY = h * 0.52f
        drawLine(
            color = ScopeCyan.copy(alpha = 0.22f),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // Bokeh / Neon reference orbs responding to focus distance
        val blurScale = if (state.manualFocusDiopters != null) {
            (1f + kotlin.math.abs(state.manualFocusDiopters - 2.2f) * 0.22f).coerceIn(1f, 2.4f)
        } else 1.0f

        drawCircle(
            color = NothingRed.copy(alpha = 0.28f),
            radius = 28.dp.toPx() * blurScale,
            center = Offset(w * 0.28f, h * 0.38f)
        )
        drawCircle(
            color = ScopeCyan.copy(alpha = 0.25f),
            radius = 34.dp.toPx() * blurScale,
            center = Offset(w * 0.72f, h * 0.42f)
        )
        drawCircle(
            color = ScopeAmber.copy(alpha = 0.26f),
            radius = 22.dp.toPx() * blurScale,
            center = Offset(w * 0.52f, h * 0.34f)
        )
    }
}

@Composable
private fun BottomShutterAndModeBar(
    state: ProCameraUiState,
    shotCount: Int,
    onSelectMode: (AppMode) -> Unit,
    onShutterTrigger: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NothingBlack)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 5-Tab Mode Switcher Ribbon (PHOTO PRO | CINE VIDEO | GLYPH | LUT | VAULT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppMode.entries.forEach { mode ->
                val selected = state.activeMode == mode
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) NothingWhite else NothingObsidian)
                        .border(
                            1.dp,
                            if (selected) NothingWhite else NothingBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("mode_tab_${mode.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.titleRu,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) NothingBlack else NothingSilver,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Shutter Deck Row: Left Glyph Quick Button | Center Dual-Ring Shutter/REC | Right Vault Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Glyph Light Studio Shortcut
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingObsidian)
                    .border(
                        1.dp,
                        if (state.activeGlyphSegments.isNotEmpty()) NothingWhite else NothingBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectMode(AppMode.GLYPH_STUDIO) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("btn_open_glyph_studio"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Glyph Studio",
                    tint = if (state.activeGlyphSegments.isNotEmpty()) ScopeAmber else NothingSilver,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "GLYPH LED",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver,
                        fontSize = 8.sp
                    )
                    Text(
                        text = "${state.activeGlyphSegments.size}/11 ЗОН",
                        style = MaterialTheme.typography.labelLarge,
                        color = NothingWhite,
                        fontSize = 11.sp
                    )
                }
            }

            // Center: Nothing OS Signature Mechanical Shutter / Record Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(3.dp, NothingWhite, CircleShape)
                    .clickable { onShutterTrigger() }
                    .padding(6.dp)
                    .testTag("shutter_record_button"),
                contentAlignment = Alignment.Center
            ) {
                val isVideoMode = state.activeMode == AppMode.CINE_VIDEO
                if (isVideoMode) {
                    Box(
                        modifier = Modifier
                            .size(if (state.isRecording) 30.dp else 52.dp)
                            .clip(if (state.isRecording) RoundedCornerShape(6.dp) else CircleShape)
                            .background(NothingRed)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
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

            // Right: Media Vault & EXIF Loupe Shortcut
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingObsidian)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectMode(AppMode.MEDIA_VAULT) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("btn_open_media_vault"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Архив EXIF",
                    tint = ScopeCyan,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "EXIF RAW",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingSilver,
                        fontSize = 8.sp
                    )
                    Text(
                        text = "$shotCount КАДР.",
                        style = MaterialTheme.typography.labelLarge,
                        color = NothingWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
