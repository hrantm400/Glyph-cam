package com.example.shots

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.model.AppMode
import com.example.model.PhotoAspect
import com.example.viewmodel.QuickSetting
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
        org.robolectric.Shadows.shadowOf(rule.activity.application).grantPermissions(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        val vm = ProCameraViewModel(rule.activity.application)
        rule.setContent {
            MyApplicationTheme { GlyphCamStudioScreen(viewModel = vm, modifier = Modifier.fillMaxSize()) }
        }
        val out = File(System.getProperty("shotsDir") ?: "/tmp/shots").apply { mkdirs() }
        fun capture(name: String) {
            rule.mainClock.advanceTimeBy(1500)
            rule.waitForIdle()
            val view = rule.activity.window.decorView
            val bmp = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
            FileOutputStream(File(out, "$name.png")).use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        AppMode.entries.forEach { mode ->
            rule.runOnIdle { vm.setAppMode(mode) }
            capture("${mode.ordinal}_${mode.name}")
        }
        // Extra states: a quick-setting sheet over a square frame, and the interval sheet.
        rule.runOnIdle {
            vm.setAppMode(AppMode.PHOTO_PRO)
            vm.setPhotoAspect(PhotoAspect.SQUARE_1_1)
            vm.openQuickSetting(QuickSetting.DRIVE)
        }
        capture("6_QUICK_SHEET_DRIVE")
        rule.runOnIdle {
            vm.setPhotoAspect(PhotoAspect.WIDE_16_9)
            vm.openQuickSetting(QuickSetting.INTERVAL)
        }
        capture("7_QUICK_SHEET_INTERVAL")
        rule.runOnIdle {
            vm.setPhotoAspect(PhotoAspect.FULL_4_3)
            vm.setAppMode(AppMode.CINE_VIDEO)
            vm.openQuickSetting(QuickSetting.FPS)
        }
        capture("8_QUICK_SHEET_FPS")
        rule.runOnIdle { vm.openQuickSetting(QuickSetting.FPS) }
    }
}
