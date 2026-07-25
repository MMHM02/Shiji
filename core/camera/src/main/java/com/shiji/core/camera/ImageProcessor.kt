package com.shiji.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Prepares food photos for AI vision APIs:
 * downscale to ≤ 2048px on the long edge, correct EXIF rotation,
 * and re-encode as JPEG under 2 MB.
 */
class ImageProcessor {

    data class ProcessedImage(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
        val compressedSize: Int
    )

    suspend fun process(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048,
        initialQuality: Int = 85,
        maxSizeBytes: Int = 2 * 1024 * 1024
    ): Result<ProcessedImage> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // 1. Read bounds only.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("无法读取图片")
            }

            // 2. Decode with sampling, then scale precisely if still too large.
            val sample = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: throw IOException("图片解码失败")

            // 3. Apply EXIF rotation so the AI sees the photo upright.
            bitmap = applyExifRotation(context, uri, bitmap)

            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            if (scale < 1f) {
                val scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
                if (scaled != bitmap) bitmap.recycle()
                bitmap = scaled
            }

            // 4. Compress until under the size cap.
            val out = ByteArrayOutputStream()
            var quality = initialQuality
            do {
                out.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                quality -= 10
            } while (out.size() > maxSizeBytes && quality > 20)

            val result = ProcessedImage(
                bytes = out.toByteArray(),
                width = bitmap.width,
                height = bitmap.height,
                compressedSize = out.size()
            )
            bitmap.recycle()
            result
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxDim) sample *= 2
        return sample
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val rotation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        if (rotation == 0f) return bitmap
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(rotation) }, true
        )
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }
}
