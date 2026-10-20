package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserSettings
import com.example.model.AppMode
import com.example.model.GlyphLightingMode
import com.example.model.IntervalCount
import com.example.model.JpegQuality
import com.example.model.LutProfile
import com.example.model.PhotoAspect
import com.example.model.PhotoDriveMode
import com.example.model.SceneExtension
import com.example.model.VideoBitrate
import com.example.viewmodel.ProCameraViewModel
import com.example.viewmodel.QuickSetting
import com.example.viewmodel.formatExposure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GlyphCam Pro", appName)
    }

    @Test
    fun `exposure labels are human readable`() {
        assertEquals("1/60", formatExposure(16_666_667L))
        assertEquals("1/1000", formatExposure(1_000_000L))
        assertEquals("2.0\"", formatExposure(2_000_000_000L))
        assertEquals("—", formatExposure(0L))
    }

    @Test
    fun `lut matrices are identity only for the natural profile`() {
        assertTrue(LutProfile.NOTHING_STD.isIdentity)
        assertFalse(LutProfile.KODAK_2383_TEAL.isIdentity)
        val mono = LutProfile.NOTHING_MONO_RED.buildColorMatrix().values
        // Fully desaturated: every output channel mixes R, G and B.
        assertTrue(mono[1] > 0f && mono[2] > 0f && mono[5] > 0f)
    }

    @Test
    fun `pro camera viewModel updates shutter glyph lut and mode state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ProCameraViewModel(app)

        viewModel.setAppMode(AppMode.CINE_VIDEO)
        viewModel.setShutterAngle(180f)
        assertEquals("180°", viewModel.uiState.value.shutterDisplayLabel)
        assertEquals(16_666_666L, viewModel.uiState.value.shutterNs)

        viewModel.setAppMode(AppMode.PHOTO_PRO)
        assertEquals(AppMode.PHOTO_PRO, viewModel.uiState.value.activeMode)
        assertEquals("1/60", viewModel.uiState.value.shutterDisplayLabel)

        viewModel.setShutterAuto()
        assertEquals("AUTO", viewModel.uiState.value.shutterDisplayLabel)

        viewModel.selectGlyphLightingMode(GlyphLightingMode.STUDIO_SOFT_FILL)
        assertEquals(11, viewModel.uiState.value.activeGlyphSegments.size)
        assertFalse(viewModel.uiState.value.glyphSupported)

        viewModel.selectLutProfile(LutProfile.NOTHING_MONO_RED)
        assertEquals(LutProfile.NOTHING_MONO_RED, viewModel.uiState.value.activeLut)
        assertNotNull(viewModel.uiState.value.lutColorMatrix)
        viewModel.selectLutProfile(LutProfile.NOTHING_STD)
        assertNull(viewModel.uiState.value.lutColorMatrix)

        viewModel.setAppMode(AppMode.MEDIA_VAULT)
        assertEquals(AppMode.PHOTO_PRO, viewModel.uiState.value.cameraMode)
    }

    @Test
    fun `pro pack settings persist across view models and shape the bind key`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ProCameraViewModel(app)

        vm.setJpegQuality(JpegQuality.Q100)
        vm.setPhotoAspect(PhotoAspect.SQUARE_1_1)
        vm.setVideoBitrate(VideoBitrate.M70)
        vm.toggleShutterSound()
        assertEquals(1f, vm.uiState.value.viewfinderAspect)
        assertEquals(JpegQuality.Q100, vm.uiState.value.bindKey.jpegQuality)
        assertEquals(PhotoAspect.SQUARE_1_1, vm.uiState.value.bindKey.photoAspect)
        assertFalse(vm.controller.shutterSoundEnabled)

        val stored = UserSettings.load(app)
        assertEquals(JpegQuality.Q100, stored.jpegQuality)
        assertEquals(PhotoAspect.SQUARE_1_1, stored.photoAspect)
        assertEquals(VideoBitrate.M70, stored.videoBitrate)
        assertFalse(stored.shutterSound)

        val second = ProCameraViewModel(app)
        assertEquals(JpegQuality.Q100, second.uiState.value.jpegQuality)
        assertFalse(second.uiState.value.shutterSound)

        // Interval settings switch the drive mode; the sheet toggles open and closed.
        vm.setIntervalCount(IntervalCount.C10)
        assertEquals(PhotoDriveMode.INTERVAL, vm.uiState.value.photoDriveMode)
        assertEquals("0 / 10", vm.uiState.value.intervalProgressLabel)
        vm.openQuickSetting(QuickSetting.DRIVE)
        assertEquals(QuickSetting.DRIVE, vm.uiState.value.quickSetting)
        vm.openQuickSetting(QuickSetting.DRIVE)
        assertNull(vm.uiState.value.quickSetting)

        // Scene modes stay off until the camera reports them.
        vm.selectSceneExtension(SceneExtension.NIGHT)
        assertEquals(SceneExtension.NONE, vm.uiState.value.sceneExtension)
        assertFalse(vm.uiState.value.isExtensionActive)

        // Video keeps a 16:9 box whatever the photo aspect is.
        vm.setAppMode(AppMode.CINE_VIDEO)
        assertEquals(9f / 16f, vm.uiState.value.viewfinderAspect)

        // Volume keys fire the shutter only while enabled.
        assertTrue(vm.onVolumeKey())
        vm.toggleVolumeKeyShutter()
        assertFalse(vm.onVolumeKey())
    }
}
