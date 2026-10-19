package com.example.glyph

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager

/**
 * Drives the Glyph Interface through Nothing's official SDK. The app models the back panel as
 * 11 segments (Phone (2) layout); [channelsFor] maps them onto the channel indices of whichever
 * Nothing phone this is running on. On any other device [supported] is false and nothing is sent.
 */
class GlyphBridge(
    context: Context,
    private val onStateChanged: (GlyphState) -> Unit
) {
    data class GlyphState(
        val supported: Boolean = false,
        val connected: Boolean = false,
        val deviceName: String = "",
        val message: String = ""
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var manager: GlyphManager? = null
    private var sessionOpen = false
    private var lastSegments: Set<Int> = emptySet()
    private var lastLight = 0

    private val device: String? = if (Build.VERSION.SDK_INT >= 33) {
        runCatching {
            when {
                Common.is20111() -> Glyph.DEVICE_20111
                Common.is22111() -> Glyph.DEVICE_22111
                Common.is23111() -> Glyph.DEVICE_23111
                Common.is23113() -> Glyph.DEVICE_23113
                Common.is24111() -> Glyph.DEVICE_24111
                Common.is25111() -> Glyph.DEVICE_25111
                Common.is25131() -> Glyph.DEVICE_25131
                else -> null
            }
        }.getOrNull()
    } else null

    val supported: Boolean get() = device != null

    val deviceName: String
        get() = when (device) {
            Glyph.DEVICE_20111 -> "Nothing Phone (1)"
            Glyph.DEVICE_22111 -> "Nothing Phone (2)"
            Glyph.DEVICE_23111 -> "Nothing Phone (2a)"
            Glyph.DEVICE_23113 -> "Nothing Phone (2a) Plus"
            Glyph.DEVICE_24111 -> "Nothing Phone (3a)"
            Glyph.DEVICE_25111 -> "Nothing Phone (4a)"
            Glyph.DEVICE_25131 -> "Nothing Phone (4b)"
            else -> Build.MODEL ?: ""
        }

    fun connect() {
        val target = device
        if (target == null) {
            onStateChanged(
                GlyphState(
                    supported = false,
                    connected = false,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                    message = "Glyph Interface есть только на телефонах Nothing"
                )
            )
            return
        }
        onStateChanged(GlyphState(supported = true, connected = false, deviceName = deviceName, message = "Подключение к Glyph…"))
        try {
            val gm = GlyphManager.getInstance(appContext)
            manager = gm
            gm.init(object : GlyphManager.Callback {
                override fun onServiceConnected(componentName: ComponentName?) {
                    val registered = runCatching { gm.register(target) }.getOrDefault(false)
                    if (!registered) {
                        post(GlyphState(true, false, deviceName, "Glyph не разрешён для приложения: включите режим отладки Glyph (adb) или установите ключ"))
                        return
                    }
                    try {
                        gm.openSession()
                        sessionOpen = true
                        post(GlyphState(true, true, deviceName, "Glyph подключён"))
                        if (lastSegments.isNotEmpty()) apply(lastSegments, lastLight * 100 / MAX_LIGHT)
                    } catch (e: GlyphException) {
                        post(GlyphState(true, false, deviceName, "Glyph: ${e.message}"))
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName?) {
                    sessionOpen = false
                    post(GlyphState(true, false, deviceName, "Сервис Glyph отключился"))
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "Glyph init failed", e)
            onStateChanged(GlyphState(true, false, deviceName, "Glyph недоступен: ${e.message}"))
        }
    }

    /** Lights exactly [segments] at [brightnessPercent]; an empty set turns everything off. */
    fun apply(segments: Set<Int>, brightnessPercent: Int) {
        val gm = manager ?: return
        lastSegments = segments
        lastLight = (brightnessPercent.coerceIn(0, 100) * MAX_LIGHT) / 100
        if (!sessionOpen) return
        try {
            if (segments.isEmpty() || lastLight == 0) {
                gm.turnOff()
                return
            }
            val builder = gm.glyphFrameBuilder
            val channels = segments.flatMap { channelsFor(it) }.distinct()
            if (channels.isEmpty()) {
                gm.turnOff()
                return
            }
            channels.forEach { builder.buildChannel(it, lastLight) }
            gm.toggle(builder.build())
        } catch (e: Throwable) {
            Log.w(TAG, "Glyph apply failed: ${e.message}")
        }
    }

    /** Shows a 0..100 level on the progress strip (D1 on Phone (1)/(2), C on the others). */
    fun showProgress(percent: Int) {
        val gm = manager ?: return
        if (!sessionOpen) return
        try {
            val builder = gm.glyphFrameBuilder
            val frame = when (device) {
                Glyph.DEVICE_20111, Glyph.DEVICE_22111 -> builder.buildChannelD().build()
                else -> builder.buildChannelC().build()
            }
            gm.displayProgress(frame, percent.coerceIn(0, 100))
        } catch (e: Throwable) {
            Log.w(TAG, "Glyph progress failed: ${e.message}")
        }
    }

    fun off() {
        lastSegments = emptySet()
        if (!sessionOpen) return
        runCatching { manager?.turnOff() }
    }

    fun release() {
        runCatching { if (sessionOpen) manager?.turnOff() }
        runCatching { if (sessionOpen) manager?.closeSession() }
        sessionOpen = false
        runCatching { manager?.unInit() }
        manager = null
    }

    private fun post(state: GlyphState) {
        mainHandler.post { onStateChanged(state) }
    }

    /**
     * App segment → SDK channel indices (see the SDK README tables).
     * 0/1 camera arcs, 2 slash, 3–8 the C-ring, 9 bottom strip, 10 record dot.
     */
    private fun channelsFor(segment: Int): List<Int> = when (device) {
        Glyph.DEVICE_22111 -> when (segment) {
            0 -> listOf(0)
            1 -> listOf(1)
            2 -> listOf(2)
            3 -> (3..10).toList()
            4 -> (11..18).toList()
            5 -> listOf(19)
            6 -> listOf(20)
            7 -> listOf(21)
            8 -> listOf(22, 23)
            9 -> (25..32).toList()
            10 -> listOf(24)
            else -> emptyList()
        }
        Glyph.DEVICE_20111 -> when (segment) {
            0, 1 -> listOf(0)
            2 -> listOf(1)
            3, 4 -> listOf(2)
            5 -> listOf(3)
            6 -> listOf(4)
            7, 8 -> listOf(5)
            9 -> (7..14).toList()
            10 -> listOf(6)
            else -> emptyList()
        }
        Glyph.DEVICE_23111, Glyph.DEVICE_23113 -> when (segment) {
            0, 1 -> listOf(25)
            2 -> listOf(24)
            in 3..8 -> { val g = segment - 3; (g * 4 until g * 4 + 4).toList() }
            else -> emptyList()
        }
        Glyph.DEVICE_24111 -> when (segment) {
            0, 1, 2 -> (20..30).toList()
            in 3..8 -> { val g = segment - 3; (g * 3 until minOf(g * 3 + 3, 20)).toList() + if (segment == 8) listOf(18, 19) else emptyList() }
            9, 10 -> (31..35).toList()
            else -> emptyList()
        }
        Glyph.DEVICE_25111 -> when (segment) {
            in 0..10 -> listOf(segment % 6)
            else -> emptyList()
        }
        Glyph.DEVICE_25131 -> when (segment) {
            in 0..10 -> listOf(segment % 4)
            else -> emptyList()
        }
        else -> emptyList()
    }

    companion object {
        private const val TAG = "GlyphBridge"
        private const val MAX_LIGHT = 4000
    }
}
