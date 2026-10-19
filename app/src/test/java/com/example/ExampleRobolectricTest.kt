package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppMode
import com.example.model.GlyphLightingMode
import com.example.model.LutProfile
import com.example.model.NothingLens
import com.example.viewmodel.ProCameraViewModel
import org.junit.Assert.assertEquals
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
    fun `pro camera viewModel updates lens shutter angle glyph and lut state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ProCameraViewModel(app)

        viewModel.selectLens(NothingLens.ULTRA_WIDE)
        assertEquals(NothingLens.ULTRA_WIDE, viewModel.uiState.value.selectedLens)
        assertEquals(0.6f, viewModel.uiState.value.fineZoomRatio, 0.01f)

        viewModel.setShutterAngle(180f)
        assertEquals("180.0°", viewModel.uiState.value.shutterDisplayLabel)

        viewModel.selectGlyphLightingMode(GlyphLightingMode.STUDIO_SOFT_FILL)
        assertEquals(11, viewModel.uiState.value.activeGlyphSegments.size)

        viewModel.selectLutProfile(LutProfile.NOTHING_MONO_RED)
        assertEquals(LutProfile.NOTHING_MONO_RED, viewModel.uiState.value.activeLut)

        viewModel.setAppMode(AppMode.PHOTO_PRO)
        assertEquals(AppMode.PHOTO_PRO, viewModel.uiState.value.activeMode)
        assertTrue(viewModel.uiState.value.shutterDisplayLabel.startsWith("1/"))
    }
}
