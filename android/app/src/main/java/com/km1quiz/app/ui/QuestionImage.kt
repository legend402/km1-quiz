package com.km1quiz.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.km1quiz.app.ui.theme.Px
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 题目图片：assets(images/xxx) 解码 + LRU 缓存 + 按目标宽度降采样 */
private val imageCache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {}

private const val TARGET_WIDTH_PX = 1280

private fun decodeAsset(context: android.content.Context, relPath: String): Bitmap? {
    val full = "images/$relPath"
    imageCache.get(full)?.let { return it }
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(full).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_WIDTH_PX) { sample *= 2 }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.assets.open(full).use { BitmapFactory.decodeStream(it, null, opts) }
        if (bmp != null) { imageCache.put(full, bmp) }
        bmp
    } catch (_: Exception) {
        null
    }
}

@Composable
fun QuestionImage(relPath: String?, px: Px, modifier: Modifier = Modifier) {
    if (relPath == null) { return }
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = null, relPath) {
        value = withContext(Dispatchers.IO) { decodeAsset(context, relPath) }
    }
    val current = bmp
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (current == null) {
            // 加载中/失败占位（静态灰块，与像素风一致）
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(px.surfaceAlt)
                    .border(2.dp, px.border)
                    .padding(vertical = 18.dp)
            ) {
                Text(
                    "题目图片",
                    style = MaterialTheme.typography.labelMedium,
                    color = px.textDim,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = "题目图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, px.border)
            )
        }
    }
}
