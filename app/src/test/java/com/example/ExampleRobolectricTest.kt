package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppMode
import com.example.model.GlyphLightingMode
import com.example.model.LutProfile
import com.example.viewmodel.ProCameraViewModel
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
}
