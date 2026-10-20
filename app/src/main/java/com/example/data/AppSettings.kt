package com.example.data

import android.content.Context
import com.example.model.AntiBanding
import com.example.model.CapturePriority
import com.example.model.JpegQuality
import com.example.model.PhotoAspect
import com.example.model.ProcessingMode
import com.example.model.VideoBitrate

/** Settings that survive restarts. Kept tiny on purpose: SharedPreferences, one key per field. */
data class UserSettings(
    val jpegQuality: JpegQuality = JpegQuality.Q95,
    val capturePriority: CapturePriority = CapturePriority.QUALITY,
    val photoAspect: PhotoAspect = PhotoAspect.FULL_4_3,
    val videoBitrate: VideoBitrate = VideoBitrate.AUTO,
    val antiBanding: AntiBanding = AntiBanding.AUTO,
    val noiseReduction: ProcessingMode = ProcessingMode.AUTO,
    val edgeMode: ProcessingMode = ProcessingMode.AUTO,
    val shutterSound: Boolean = true,
    val volumeKeyShutter: Boolean = true,
    val mirrorSelfie: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val bakeLutIntoPhotos: Boolean = true,
    val keepScreenOn: Boolean = true
) {
    companion object {
        private const val PREFS = "glyphcam_settings"

        fun load(context: Context): UserSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            fun <T : Enum<T>> enumOf(key: String, default: T, values: Array<T>): T =
                values.firstOrNull { it.name == p.getString(key, null) } ?: default
            return UserSettings(
                jpegQuality = enumOf("jpegQuality", JpegQuality.Q95, JpegQuality.values()),
                capturePriority = enumOf("capturePriority", CapturePriority.QUALITY, CapturePriority.values()),
                photoAspect = enumOf("photoAspect", PhotoAspect.FULL_4_3, PhotoAspect.values()),
                videoBitrate = enumOf("videoBitrate", VideoBitrate.AUTO, VideoBitrate.values()),
                antiBanding = enumOf("antiBanding", AntiBanding.AUTO, AntiBanding.values()),
                noiseReduction = enumOf("noiseReduction", ProcessingMode.AUTO, ProcessingMode.values()),
                edgeMode = enumOf("edgeMode", ProcessingMode.AUTO, ProcessingMode.values()),
                shutterSound = p.getBoolean("shutterSound", true),
                volumeKeyShutter = p.getBoolean("volumeKeyShutter", true),
                mirrorSelfie = p.getBoolean("mirrorSelfie", true),
                hapticsEnabled = p.getBoolean("hapticsEnabled", true),
                bakeLutIntoPhotos = p.getBoolean("bakeLut", true),
                keepScreenOn = p.getBoolean("keepScreenOn", true)
            )
        }
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("jpegQuality", jpegQuality.name)
            .putString("capturePriority", capturePriority.name)
            .putString("photoAspect", photoAspect.name)
            .putString("videoBitrate", videoBitrate.name)
            .putString("antiBanding", antiBanding.name)
            .putString("noiseReduction", noiseReduction.name)
            .putString("edgeMode", edgeMode.name)
            .putBoolean("shutterSound", shutterSound)
            .putBoolean("volumeKeyShutter", volumeKeyShutter)
            .putBoolean("mirrorSelfie", mirrorSelfie)
            .putBoolean("hapticsEnabled", hapticsEnabled)
            .putBoolean("bakeLut", bakeLutIntoPhotos)
            .putBoolean("keepScreenOn", keepScreenOn)
            .apply()
    }
}
