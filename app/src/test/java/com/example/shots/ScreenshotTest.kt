package com.example.shots

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.model.AppMode
import com.example.ui.screens.GlyphCamStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ProCameraViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureAllModes() {
        val vm = ProCameraViewModel(rule.activity.application)
        rule.setContent {
            MyApplicationTheme { GlyphCamStudioScreen(viewModel = vm, modifier = Modifier.fillMaxSize()) }
        }
        val out = File(System.getProperty("shotsDir") ?: "/tmp/shots").apply { mkdirs() }
        AppMode.entries.forEach { mode ->
            rule.runOnIdle { vm.setAppMode(mode) }
            rule.mainClock.advanceTimeBy(1500)
            rule.waitForIdle()
            val view = rule.activity.window.decorView
            val bmp = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
            FileOutputStream(File(out, "${mode.ordinal}_${mode.name}.png")).use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
