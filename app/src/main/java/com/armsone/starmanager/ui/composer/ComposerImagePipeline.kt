package com.armsone.starmanager.ui.composer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

data class ComposerImagePolicy(
    val maximumLongEdgePixels: Int = 4_096,
    val maximumBytes: Int = 8_000_000,
    val initialJpegQuality: Int = 92,
    val minimumJpegQuality: Int = 70
)

/** 작성 화면용 사진은 한 장씩 처리해 방향을 고정하고 메모리·저장 크기를 제한한다. */
object ComposerImagePipeline {
    fun prepareForComposer(
        source: ByteArray,
        policy: ComposerImagePolicy = ComposerImagePolicy()
    ): ByteArray {
        require(source.isNotEmpty()) { "EMPTY_IMAGE" }
        var bitmap = decodeSampled(source, policy.maximumLongEdgePixels)
            ?: throw IllegalArgumentException("UNSUPPORTED_IMAGE")
        val oriented = applyExifOrientation(bitmap, source)
        if (oriented !== bitmap) bitmap.recycle()
        bitmap = oriented
        val scaled = scaleToLongEdge(bitmap, policy.maximumLongEdgePixels)
        if (scaled !== bitmap) bitmap.recycle()
        bitmap = scaled

        try {
            var quality = policy.initialJpegQuality
            var encoded = encodeJpeg(bitmap, quality)
            while (encoded.size > policy.maximumBytes && quality > policy.minimumJpegQuality) {
                quality = maxOf(policy.minimumJpegQuality, quality - 6)
                encoded = encodeJpeg(bitmap, quality)
            }
            while (encoded.size > policy.maximumBytes && maxOf(bitmap.width, bitmap.height) > 1_280) {
                val smaller = Bitmap.createScaledBitmap(
                    bitmap,
                    maxOf(1, (bitmap.width * 0.85f).toInt()),
                    maxOf(1, (bitmap.height * 0.85f).toInt()),
                    true
                )
                if (smaller !== bitmap) bitmap.recycle()
                bitmap = smaller
                encoded = encodeJpeg(bitmap, quality)
            }
            if (encoded.size > policy.maximumBytes) throw IllegalArgumentException("IMAGE_SIZE_TARGET_UNREACHABLE")
            return encoded
        } finally {
            bitmap.recycle()
        }
    }

    fun decodeForDisplay(
        source: ByteArray,
        maximumLongEdgePixels: Int,
        lowMemory: Boolean = false
    ): Bitmap? {
        if (source.isEmpty()) return null
        var bitmap = decodeSampled(source, maximumLongEdgePixels, lowMemory) ?: return null
        val oriented = applyExifOrientation(bitmap, source)
        if (oriented !== bitmap) bitmap.recycle()
        bitmap = oriented
        val scaled = scaleToLongEdge(bitmap, maximumLongEdgePixels)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    internal fun sampleSizeFor(width: Int, height: Int, maximumLongEdgePixels: Int): Int {
        if (width <= 0 || height <= 0 || maximumLongEdgePixels <= 0) return 1
        var sampleSize = 1
        val longEdge = maxOf(width, height)
        while (longEdge / (sampleSize * 2) >= maximumLongEdgePixels) sampleSize *= 2
        return sampleSize
    }

    private fun decodeSampled(source: ByteArray, maximumLongEdgePixels: Int, lowMemory: Boolean = false): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maximumLongEdgePixels)
            if (lowMemory) inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(source, 0, source.size, options)
    }

    private fun applyExifOrientation(bitmap: Bitmap, source: ByteArray): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(source)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToLongEdge(bitmap: Bitmap, maximumLongEdgePixels: Int): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= maximumLongEdgePixels) return bitmap
        val ratio = maximumLongEdgePixels.toFloat() / longEdge.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            maxOf(1, (bitmap.width * ratio).toInt()),
            maxOf(1, (bitmap.height * ratio).toInt()),
            true
        )
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "IMAGE_ENCODE_FAILED" }
        return output.toByteArray()
    }
}
