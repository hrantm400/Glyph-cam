package com.example.ui.components

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Small in-memory cache so the archive grid does not decode the same file twice. */
private object ThumbnailCache {
    val lru = LruCache<String, Bitmap>(48)
}

/** Loads a real thumbnail of a MediaStore item (photo or video) off the main thread. */
@Composable
fun rememberMediaThumbnail(uri: String?, isVideo: Boolean, sizePx: Int = 384): State<Bitmap?> {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = uri?.let { ThumbnailCache.lru.get("$it@$sizePx") }, uri, sizePx) {
        if (uri == null) {
            value = null
            return@produceState
        }
        val key = "$uri@$sizePx"
        val cached = ThumbnailCache.lru.get(key)
        if (cached != null) {
            value = cached
            return@produceState
        }
        val bitmap = withContext(Dispatchers.IO) { loadMediaThumbnail(context, Uri.parse(uri), isVideo, sizePx) }
        if (bitmap != null) ThumbnailCache.lru.put(key, bitmap)
        value = bitmap
    }
}

fun loadMediaThumbnail(context: Context, uri: Uri, isVideo: Boolean, sizePx: Int): Bitmap? {
    val resolver = context.contentResolver
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
        } else if (isVideo) {
            legacyVideoFrame(resolver, uri)
        } else {
            legacyImageThumbnail(resolver, uri, sizePx)
        }
    } catch (e: Exception) {
        try {
            if (isVideo) legacyVideoFrame(resolver, uri) else legacyImageThumbnail(resolver, uri, sizePx)
        } catch (inner: Exception) {
            null
        }
    }
}

private fun legacyVideoFrame(resolver: ContentResolver, uri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            retriever.setDataSource(pfd.fileDescriptor)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } finally {
        runCatching { retriever.release() }
    }
}

private fun legacyImageThumbnail(resolver: ContentResolver, uri: Uri, sizePx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}
