package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CameraPresetEntity
import com.example.data.CapturedShotEntity
import com.example.model.AppMode
import com.example.model.CameraStatus
import com.example.ui.components.CameraDeckActions
import com.example.ui.components.ChipShape
import com.example.ui.components.FadingScrollRow
import com.example.ui.components.FloatingScopesHudDock
import com.example.ui.components.LensTurretRow
import com.example.ui.components.ManualDialDrawer
import com.example.ui.components.ParameterStrip
import com.example.ui.components.PillShape
import com.example.ui.components.QuickSettingSheet
import com.example.ui.components.StatusDot
import com.example.ui.components.TopTelemetryHudBar
import com.example.ui.components.ViewfinderOpticalOverlays
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
import com.example.ui.theme.ScopeMagenta
import com.example.viewmodel.ProCameraUiState
import com.example.viewmodel.ProCameraViewModel
import com.example.viewmodel.isCameraMode
import kotlinx.coroutines.delay
import kotlin.math.ln

private val ViewfinderShape = RoundedCornerShape(20.dp)

@Composable
fun GlyphCamStudioScreen(
    viewModel: ProCameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val capturedShots by viewModel.capturedShots.collectAsStateWithLifecycle()

    fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    var hasCameraPermission by remember { mutableStateOf(granted(Manifest.permission.CAMERA)) }
    var hasAudioPermission by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true || granted(Manifest.permission.CAMERA)
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] == true || granted(Manifest.permission.RECORD_AUDIO)
    }
    val requestPermissions: () -> Unit = {
        permissionAsked = true
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionAsked) requestPermissions()
    }

    // One PreviewView for the whole app lifetime: CameraX keeps its SurfaceTexture across detaches.
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hasCameraPermission = granted(Manifest.permission.CAMERA)
                    hasAudioPermission = granted(Manifest.permission.RECORD_AUDIO)
                    viewModel.onForeground()
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.onBackground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // (Re)bind CameraX whenever permission or anything the bind depends on changes.
    val bindKey = state.bindKey
    LaunchedEffect(hasCameraPermission, bindKey) {
        if (!hasCameraPermission) {
            viewModel.onPermissionsChanged(camera = false)
            return@LaunchedEffect
        }
        // Wait briefly for the first layout so the view port (WYSIWYG crop) is available.
        var waited = 0
        while (previewView.width == 0 && waited < 40) {
            delay(25)
            waited++
        }
        viewModel.bindCamera(lifecycleOwner, previewView)
    }
    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) viewModel.onForeground()
    }

    val backToCamera: () -> Unit = { viewModel.setAppMode(state.lastCameraMode) }
    val retryCamera: () -> Unit = {
        if (hasCameraPermission) viewModel.bindCamera(lifecycleOwner, previewView) else requestPermissions()
    }

    val rootView = LocalView.current
    LaunchedEffect(state.keepScreenOn) {
        rootView.keepScreenOn = state.keepScreenOn
    }

    val actions = remember(viewModel) {
        CameraDeckActions(
            openQuickSetting = viewModel::openQuickSetting,
            openSettings = { viewModel.setAppMode(AppMode.SETTINGS) },
            setPhotoFormat = viewModel::setPhotoFormat,
            setPhotoDrive = viewModel::setPhotoDrive,
            setSelfTimer = viewModel::setSelfTimer,
            setFlash = viewModel::setFlash,
            setPhotoAspect = viewModel::setPhotoAspect,
            setSceneExtension = viewModel::selectSceneExtension,
            setJpegQuality = viewModel::setJpegQuality,
            setCapturePriority = viewModel::setCapturePriority,
            setVideoResolution = viewModel::setVideoResolution,
            setVideoFps = viewModel::setVideoFps,
            setDynamicRange = viewModel::setDynamicRange,
            setStabilization = viewModel::setStabilization,
            setVideoBitrate = viewModel::setVideoBitrate,
            setSlowMotion = viewModel::setSlowMotion,
            setAnamorphic = viewModel::setAnamorphic,
            setAspectGuide = viewModel::setAspectGuide,
            setPeaking = viewModel::setFocusPeaking,
            setZebra = viewModel::setZebraThreshold,
            setGrid = viewModel::setGridOverlay,
            setIntervalSpacing = viewModel::setIntervalSpacing,
            setIntervalCount = viewModel::setIntervalCount,
            toggleFalseColor = viewModel::toggleFalseColor,
            toggleHorizon = viewModel::toggleHorizonLevel,
            toggleAeAfLock = { viewModel.toggleAeAfLock(previewView) },
            toggleAeLock = viewModel::toggleAeLock,
            toggleAwbLock = viewModel::toggleAwbLock,
            toggleFocusMagnifier = viewModel::toggleFocusMagnifier,
            selectLens = viewModel::selectLens,
            toggleFrontCamera = viewModel::toggleFrontCamera,
            toggleDialTab = viewModel::toggleDialTab,
            setIso = viewModel::setIso,
            setShutterNs = viewModel::setShutterNs,
            setShutterAngle = viewModel::setShutterAngle,
            toggleShutterAngleMode = viewModel::toggleShutterAngleMode,
            setManualFocus = viewModel::setManualFocus,
            triggerFocusPull = viewModel::triggerRackFocusPull,
            saveFocusPoint = viewModel::saveFocusPullPoint,
            setFocusPullSpeed = viewModel::setFocusPullSpeed,
            setWhiteBalance = viewModel::setWhiteBalance,
            setEv = viewModel::setEvCompensation,
            toggleAudio = viewModel::toggleAudioEnabled
        )
    }

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
                    shots = capturedShots,
                    hasCameraPermission = hasCameraPermission,
                    permissionAsked = permissionAsked,
                    previewView = previewView,
                    viewModel = viewModel,
                    actions = actions,
                    onRequestPermissions = requestPermissions,
                    onRetryCamera = retryCamera
                )
            }
            AppMode.GLYPH_STUDIO -> {
                GlyphStudioScreen(
                    state = state,
                    onSelectGlyphMode = viewModel::selectGlyphLightingMode,
                    onToggleSegment = viewModel::toggleGlyphSegment,
                    onBrightnessChange = viewModel::setGlyphBrightness,
                    onToggleTorchHardware = viewModel::toggleGlyphTorchHardware,
                    onReconnect = viewModel::reconnectGlyph,
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
                    onToggleBake = viewModel::toggleBakeLut,
                    onResetGrade = viewModel::resetColorGrade,
                    onSavePreset = viewModel::saveCurrentAsPreset,
                    onApplyPreset = {
                        viewModel.applyPreset(it)
                    },
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
            AppMode.SETTINGS -> {
                SettingsScreen(
                    state = state,
                    onJpegQuality = viewModel::setJpegQuality,
                    onCapturePriority = viewModel::setCapturePriority,
                    onPhotoAspect = viewModel::setPhotoAspect,
                    onVideoBitrate = viewModel::setVideoBitrate,
                    onAudioSource = viewModel::setAudioSource,
                    onRecordingLimit = viewModel::setRecordingLimit,
                    onAntiBanding = viewModel::setAntiBanding,
                    onNoiseReduction = viewModel::setNoiseReduction,
                    onEdgeMode = viewModel::setEdgeMode,
                    onToggleShutterSound = viewModel::toggleShutterSound,
                    onToggleVolumeKeys = viewModel::toggleVolumeKeyShutter,
                    onToggleMirror = viewModel::toggleMirrorSelfie,
                    onToggleHaptics = viewModel::toggleHaptics,
                    onToggleKeepScreenOn = viewModel::toggleKeepScreenOn,
                    onToggleBakeLut = viewModel::toggleBakeLut,
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
    shots: List<CapturedShotEntity>,
    hasCameraPermission: Boolean,
    permissionAsked: Boolean,
    previewView: PreviewView,
    viewModel: ProCameraViewModel,
    actions: CameraDeckActions,
    onRequestPermissions: () -> Unit,
    onRetryCamera: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopTelemetryHudBar(
            state = state,
            actions = actions,
            modifier = Modifier.statusBarsPadding()
        )

        Viewfinder(
            state = state,
            presets = presets,
            hasCameraPermission = hasCameraPermission,
            permissionAsked = permissionAsked,
            previewView = previewView,
            viewModel = viewModel,
            onRequestPermissions = onRequestPermissions,
            onRetryCamera = onRetryCamera,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NothingBlack)
                .padding(horizontal = 12.dp)
                .padding(top = 10.dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            QuickSettingSheet(state = state, actions = actions)
            ManualDialDrawer(state = state, actions = actions)
            LensTurretRow(state = state, actions = actions)
            Spacer(modifier = Modifier.height(8.dp))
            ParameterStrip(state = state, onToggleDialTab = actions.toggleDialTab)
            Spacer(modifier = Modifier.height(12.dp))
            ShutterDeckRow(
                state = state,
                lastShot = shots.firstOrNull(),
                shotCount = shots.size,
                onSelectMode = viewModel::setAppMode,
                onShutterTrigger = viewModel::triggerShutter,
                onPauseResume = viewModel::togglePauseRecording,
                onSnapshot = { viewModel.snapshotWhileRecording(previewView) }
            )
        }
    }
}

@Composable
private fun Viewfinder(
    state: ProCameraUiState,
    presets: List<CameraPresetEntity>,
    hasCameraPermission: Boolean,
    permissionAsked: Boolean,
    previewView: PreviewView,
    viewModel: ProCameraViewModel,
    onRequestPermissions: () -> Unit,
    onRetryCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentState by rememberUpdatedState(state)
    val cameraReady = state.cameraStatus is CameraStatus.Ready

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Frame shaped like the output (4:3, 16:9 or 1:1 photo, 16:9 video) that fits the available area.
        Box(
            modifier = Modifier
                .aspectRatio(state.viewfinderAspect)
                .clip(ViewfinderShape)
                .background(NothingObsidian)
                .border(
                    width = if (state.isRecording) 2.dp else 1.dp,
                    color = when {
                        state.isRecording && state.isRecordingPaused -> ScopeAmber
                        state.isRecording -> NothingRed
                        else -> NothingBorder.copy(alpha = 0.6f)
                    },
                    shape = ViewfinderShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (currentState.cameraStatus !is CameraStatus.Ready) return@detectTapGestures
                            val nx = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                            val ny = (offset.y / size.height).coerceIn(0.05f, 0.95f)
                            viewModel.onViewfinderTap(nx, ny, previewView, offset.x, offset.y, longPress = false)
                        },
                        onLongPress = { offset ->
                            if (currentState.cameraStatus !is CameraStatus.Ready) return@detectTapGestures
                            val nx = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                            val ny = (offset.y / size.height).coerceIn(0.05f, 0.95f)
                            viewModel.onViewfinderTap(nx, ny, previewView, offset.x, offset.y, longPress = true)
                        },
                        onDoubleTap = {
                            val target = if (currentState.zoomRatio < 1.5f) 2f else 1f
                            viewModel.setZoom(target)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) viewModel.pinchZoom(zoom)
                    }
                }
                .testTag("camera_viewfinder_box")
        ) {
            if (hasCameraPermission) {
                CameraPreviewLayer(previewView = previewView, state = state)
            }

            ViewfinderOpticalOverlays(
                telemetry = state.scopeTelemetry,
                gridMode = state.gridOverlayMode,
                aspectGuide = state.aspectGuide,
                maskOpacity = state.aspectMaskOpacity,
                zebraThreshold = state.zebraThreshold,
                peakingColor = state.focusPeakingColor,
                showFalseColor = state.showFalseColor,
                showHorizon = state.showHorizonLevel && cameraReady,
                rollDegrees = state.rollDegrees,
                pitchDegrees = state.pitchDegrees,
                tapFocusPoint = state.tapFocusNormalized,
                aeAfLocked = state.aeAfLocked,
                modifier = Modifier.fillMaxSize()
            )

            ShutterFlashLayer(trigger = state.shutterFlashTrigger)

            FloatingScopesHudDock(
                telemetry = state.scopeTelemetry,
                showHistogram = state.showHistogramRgb,
                showWaveform = state.showWaveformScope,
                showVectorscope = state.showVectorscope,
                showAudio = state.isVideoMode && state.audioEnabled && state.audioAvailable,
                leftAudioDb = state.leftAudioDb,
                rightAudioDb = state.rightAudioDb,
                peakAudioDb = state.peakAudioDb,
                onToggleHistogram = viewModel::toggleHistogram,
                onToggleWaveform = viewModel::toggleWaveform,
                onToggleVectorscope = viewModel::toggleVectorscope,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            )

            when {
                !hasCameraPermission -> PermissionCard(
                    permissionAsked = permissionAsked,
                    onRequestPermissions = onRequestPermissions,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
                state.cameraStatus is CameraStatus.Error -> CameraErrorCard(
                    message = (state.cameraStatus as CameraStatus.Error).message,
                    onRetry = onRetryCamera,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
                state.cameraStatus is CameraStatus.Starting || state.cameraStatus is CameraStatus.NoPermission -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NothingRed, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Запуск камеры…",
                            style = MaterialTheme.typography.labelMedium,
                            color = NothingSilver
                        )
                    }
                }
            }

            if (state.timerCountdown > 0) {
                Text(
                    text = state.timerCountdown.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 96.sp,
                    lineHeight = 100.sp,
                    color = NothingWhite,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("timer_countdown")
                )
            }

            ViewfinderBadges(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )

            ZoomRulerOverlay(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 92.dp)
            )

            ViewfinderBottomStrip(
                state = state,
                presets = presets,
                onApplyPreset = viewModel::applyPreset,
                onToggleHistogram = viewModel::toggleHistogram,
                onToggleWaveform = viewModel::toggleWaveform,
                onToggleVectorscope = viewModel::toggleVectorscope,
                onToggleMagnifier = viewModel::toggleFocusMagnifier,
                onOpenLutLab = { viewModel.setAppMode(AppMode.LUT_LAB) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * The live camera stream. The LUT is applied as a hardware-layer colour filter on the view and
 * the anamorphic de-squeeze as a horizontal scale, so what you see is the real preview, graded.
 */
@Composable
private fun CameraPreviewLayer(previewView: PreviewView, state: ProCameraUiState) {
    // Focus magnifier is a pure preview punch-in: the capture pipeline is untouched.
    val magnifier = if (state.focusMagnifier) 2f else 1f
    val scaleX = state.anamorphicDeSqueeze.scaleX * magnifier
    val lutFilter: ColorMatrixColorFilter? = remember(
        state.activeLut, state.userContrast, state.userSaturation, state.shadowLift, state.highlightRollOff
    ) {
        state.lutColorMatrix?.let { ColorMatrixColorFilter(it) }
    }
    AndroidView(
        factory = { previewView },
        update = { pv ->
            if (pv.scaleX != scaleX) pv.scaleX = scaleX
            if (pv.scaleY != magnifier) pv.scaleY = magnifier
            if (pv.tag !== lutFilter) {
                pv.tag = lutFilter
                if (lutFilter != null) {
                    pv.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = lutFilter })
                } else {
                    pv.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("camera_preview_view")
    )
}

@Composable
private fun ShutterFlashLayer(trigger: Long) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0L) {
            alpha.snapTo(0.85f)
            alpha.animateTo(0f, tween(durationMillis = 220))
        }
    }
    if (alpha.value > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value)
                .background(NothingWhite)
        )
    }
}

@Composable
private fun PermissionCard(
    permissionAsked: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NothingBlack.copy(alpha = 0.92f))
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
            Text(text = stringResource(R.string.grant_permissions_btn), style = MaterialTheme.typography.labelLarge)
        }
        if (permissionAsked) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    runCatching { context.startActivity(intent) }
                },
                shape = PillShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "ОТКРЫТЬ НАСТРОЙКИ", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CameraErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NothingBlack.copy(alpha = 0.92f))
            .border(1.dp, NothingRed.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("camera_error_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = NothingRed,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Камера не запустилась",
            style = MaterialTheme.typography.titleMedium,
            color = NothingWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = NothingSilver,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = NothingWhite),
            shape = PillShape,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_retry_camera")
        ) {
            Text(text = "ПОВТОРИТЬ", style = MaterialTheme.typography.labelLarge)
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
    onToggleMagnifier: () -> Unit,
    onOpenLutLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Messages fade after a few seconds; the camera's ready line stays as a quiet fallback.
    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.statusBanner) {
        bannerVisible = state.statusBanner.isNotEmpty()
        if (bannerVisible) {
            delay(3500)
            bannerVisible = false
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, NothingBlack.copy(alpha = 0.9f))))
            .padding(horizontal = 12.dp)
            .padding(top = 28.dp, bottom = 10.dp)
    ) {
        FadingScrollRow(modifier = Modifier.fillMaxWidth(), spacing = 6.dp, fade = false) {
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
                Text(text = state.activeLut.code, style = MaterialTheme.typography.labelLarge, color = lutColor)
            }

            MiniTogglePill("HIST", state.showHistogramRgb, ScopeGreen, onToggleHistogram)
            MiniTogglePill("WFM", state.showWaveformScope, ScopeGreen, onToggleWaveform)
            MiniTogglePill("VEC", state.showVectorscope, ScopeCyan, onToggleVectorscope)
            MiniTogglePill("ЛУПА", state.focusMagnifier, ScopeAmber, onToggleMagnifier)

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
                    Text(text = preset.slotCode, style = MaterialTheme.typography.labelLarge, color = ScopeAmber)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        val readyLine = (state.cameraStatus as? CameraStatus.Ready)?.detail ?: ""
        val showMessage = bannerVisible && state.statusBanner.isNotEmpty()
        Text(
            text = if (showMessage) state.statusBanner else readyLine,
            style = MaterialTheme.typography.labelSmall,
            color = if (showMessage) NothingWhite else NothingSilver.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("status_banner")
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
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = if (active) color else NothingSilver)
    }
}

@Composable
private fun ShutterDeckRow(
    state: ProCameraUiState,
    lastShot: CapturedShotEntity?,
    shotCount: Int,
    onSelectMode: (AppMode) -> Unit,
    onShutterTrigger: () -> Unit,
    onPauseResume: () -> Unit,
    onSnapshot: () -> Unit
) {
    // Icons follow the device orientation so the deck reads correctly in landscape.
    val targetRotation = if (state.deviceRotationDeg == 270) -90f else state.deviceRotationDeg.toFloat()
    val rotation by animateFloatAsState(targetValue = targetRotation, label = "deckRotation")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (state.isRecording) {
                DeckQuickTile(
                    icon = if (state.isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    title = "ЗАПИСЬ",
                    value = if (state.isRecordingPaused) "ДАЛЕЕ" else "ПАУЗА",
                    accent = ScopeAmber,
                    onClick = onPauseResume,
                    tag = "btn_pause_resume",
                    rotation = rotation
                )
            } else {
                ArchiveTile(
                    lastShot = lastShot,
                    count = shotCount,
                    onClick = { onSelectMode(AppMode.MEDIA_VAULT) },
                    rotation = rotation
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ModeSwitch(activeMode = state.activeMode, onSelectMode = onSelectMode)
            Spacer(modifier = Modifier.height(10.dp))
            ShutterButton(state = state, onClick = onShutterTrigger)
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (state.isRecording) {
                DeckQuickTile(
                    icon = Icons.Default.CameraAlt,
                    title = "СТОП-КАДР",
                    value = "ФОТО",
                    accent = NothingWhite,
                    onClick = onSnapshot,
                    tag = "btn_snapshot",
                    rotation = rotation
                )
            } else {
                val glyphOn = state.glyphConnected && state.activeGlyphSegments.isNotEmpty()
                DeckQuickTile(
                    icon = Icons.Default.FlashOn,
                    title = "GLYPH",
                    value = when {
                        !state.glyphSupported -> "—"
                        !state.glyphConnected -> "OFF"
                        else -> "${state.activeGlyphSegments.size}/11"
                    },
                    accent = if (glyphOn) ScopeAmber else NothingSilver,
                    onClick = { onSelectMode(AppMode.GLYPH_STUDIO) },
                    tag = "btn_open_glyph_studio",
                    rotation = rotation
                )
            }
        }
    }
}

@Composable
private fun ArchiveTile(
    lastShot: CapturedShotEntity?,
    count: Int,
    onClick: () -> Unit,
    rotation: Float = 0f
) {
    val thumb by rememberMediaThumbnail(uri = lastShot?.uri, isVideo = lastShot?.isVideo == true, sizePx = 160)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NothingSurface)
            .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("btn_open_media_vault"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .rotate(rotation)
                .clip(RoundedCornerShape(8.dp))
                .background(NothingObsidian),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = thumb
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Последний кадр",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Архив",
                    tint = ScopeCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.labelLarge, color = NothingWhite)
        Text(text = "АРХИВ", style = MaterialTheme.typography.labelSmall, color = NothingSilver)
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
    icon: ImageVector,
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
    val ready = state.cameraStatus is CameraStatus.Ready
    val busy = state.isCapturing || state.timerCountdown > 0
    val intervalActive = state.intervalRunning
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(3.dp, if (ready) NothingWhite else NothingSilver.copy(alpha = 0.5f), CircleShape)
            .clickable { onClick() }
            .padding(7.dp)
            .testTag("shutter_record_button"),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isVideoMode -> Box(
                modifier = Modifier
                    .size(if (state.isRecording) 28.dp else 58.dp)
                    .clip(if (state.isRecording) RoundedCornerShape(6.dp) else CircleShape)
                    .background(if (ready) NothingRed else NothingRed.copy(alpha = 0.4f))
            )
            intervalActive -> Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(ScopeAmber.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(NothingBlack)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = state.intervalProgressLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingBlack,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            busy -> Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(ScopeAmber.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                if (state.timerCountdown > 0) {
                    Text(
                        text = state.timerCountdown.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = NothingBlack
                    )
                } else {
                    CircularProgressIndicator(color = NothingBlack, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            }
            else -> Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(if (ready) NothingWhite else NothingSilver.copy(alpha = 0.4f)),
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
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    tag: String,
    rotation: Float = 0f
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
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = NothingWhite)
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = NothingSilver)
    }
}

/** Small state badges in the viewfinder corner: interval progress, pause, scene mode, magnifier, locks. */
@Composable
private fun ViewfinderBadges(state: ProCameraUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.intervalRunning) {
            ViewfinderBadge(
                text = "ИНТЕРВАЛ ${state.intervalProgressLabel} · ${state.intervalSpacing.label}",
                color = ScopeAmber,
                tag = "badge_interval"
            )
        }
        if (state.isRecording && state.isRecordingPaused) {
            ViewfinderBadge(text = "ПАУЗА · ${state.formattedTimecode}", color = ScopeAmber, tag = "badge_paused")
        }
        if (state.isSlowMotionMode) {
            ViewfinderBadge(
                text = "SLOW-MO ${state.videoFps.fps} К/С" + if (state.slowMotion) " · ×${state.videoFps.slowMotionFactor}" else " · РЕАЛЬНОЕ ВРЕМЯ",
                color = ScopeMagenta,
                tag = "badge_slowmo"
            )
        }
        if (state.capabilities.cameraKind != com.example.model.LensKind.MAIN && state.capabilities.cameraKind != com.example.model.LensKind.FRONT) {
            ViewfinderBadge(text = state.capabilities.cameraKind.labelRu.uppercase(), color = NothingWhite, tag = "badge_lens_kind")
        }
        if (state.isExtensionActive) {
            ViewfinderBadge(text = state.sceneExtension.labelRu.uppercase(), color = ScopeMagenta, tag = "badge_scene")
        }
        if (state.focusMagnifier) {
            ViewfinderBadge(text = "ЛУПА ×2 · ТОЛЬКО ПРЕВЬЮ", color = ScopeAmber, tag = "badge_magnifier")
        }
        if (state.aeLocked || state.awbLocked) {
            ViewfinderBadge(
                text = listOfNotNull(if (state.aeLocked) "AE-L" else null, if (state.awbLocked) "AWB-L" else null).joinToString(" · "),
                color = ScopeCyan,
                tag = "badge_locks"
            )
        }
    }
}

@Composable
private fun ViewfinderBadge(text: String, color: Color, tag: String) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(NothingBlack.copy(alpha = 0.72f))
            .border(1.dp, color.copy(alpha = 0.8f), ChipShape)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusDot(color = color, size = 6.dp)
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/** Logarithmic zoom ruler with a tick per real lens; shows for a moment after any zoom change. */
@Composable
private fun ZoomRulerOverlay(state: ProCameraUiState, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.zoomRulerUntilMs) {
        val remaining = state.zoomRulerUntilMs - SystemClock.elapsedRealtime()
        if (remaining > 0L) {
            visible = true
            delay(remaining)
        }
        visible = false
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val caps = state.capabilities
        val minZ = caps.minZoom.coerceAtLeast(0.1f)
        val maxZ = caps.maxZoom.coerceAtLeast(minZ * 1.1f)
        fun frac(z: Float): Float = (ln((z / minZ).toDouble()) / ln((maxZ / minZ).toDouble())).toFloat().coerceIn(0f, 1f)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(ChipShape)
                .background(NothingBlack.copy(alpha = 0.62f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("zoom_ruler"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.zoomLabel,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                color = ScopeAmber
            )
            Spacer(modifier = Modifier.height(4.dp))
            val lensTicks = caps.lensOptions.map { frac(it.zoomRatio) }
            val current = frac(state.zoomRatio)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val w = size.width
                val y = size.height / 2f
                drawLine(NothingSilver, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                for (i in 0..24) {
                    val x = w * i / 24f
                    val h = if (i % 4 == 0) 5.dp.toPx() else 3.dp.toPx()
                    drawLine(NothingSilver.copy(alpha = 0.7f), Offset(x, y - h), Offset(x, y + h), strokeWidth = 1f)
                }
                lensTicks.forEach { f ->
                    val x = w * f
                    drawLine(NothingWhite, Offset(x, y - 8.dp.toPx()), Offset(x, y + 8.dp.toPx()), strokeWidth = 2.dp.toPx())
                }
                val cx = w * current
                drawLine(ScopeAmber, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 3.dp.toPx())
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = String.format(java.util.Locale.US, "%.1f×", minZ), style = MaterialTheme.typography.labelSmall, color = NothingSilver)
                Text(text = String.format(java.util.Locale.US, "%.0f×", maxZ), style = MaterialTheme.typography.labelSmall, color = NothingSilver)
            }
        }
    }
}
