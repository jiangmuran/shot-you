package com.shotyou.app.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.model.Photo
import com.shotyou.app.domain.repository.DeleteOutcome
import com.shotyou.app.domain.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Reads photos from MediaStore and decodes/encodes bytes for AI calls.
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoRepository {

    private val resolver get() = context.contentResolver

    override suspend fun queryImages(limit: Int): List<Photo> = withContext(Dispatchers.IO) {
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )

        // Newest first: prefer DATE_TAKEN, fall back to DATE_ADDED.
        val sortOrder =
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

        val photos = ArrayList<Photo>(minOf(limit, 512))
        resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext() && photos.size < limit) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val dateTaken = cursor.getLong(dateTakenCol).takeIf { it > 0 }
                    ?: (cursor.getLong(dateAddedCol) * 1000L) // DATE_ADDED is seconds
                photos += Photo(
                    id = id,
                    uri = contentUri.toString(),
                    displayName = cursor.getString(nameCol) ?: "IMG_$id",
                    dateTakenMs = dateTaken,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    bucketName = cursor.getString(bucketCol),
                )
            }
        }
        photos
    }

    override suspend fun loadAiImage(uri: String, maxEdge: Int): AiImage = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)

        // Pass 1: bounds only, to compute a sample size.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(parsed)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Unable to decode image bounds for $uri")
        }

        val safeMaxEdge = maxEdge.coerceAtLeast(1)
        val options = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, safeMaxEdge)
        }

        var bitmap = resolver.openInputStream(parsed)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("Unable to decode image $uri")

        // Apply EXIF orientation.
        val orientation = runCatching {
            resolver.openInputStream(parsed)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        bitmap = applyOrientation(bitmap, orientation)

        // Final precise downscale so the longest edge <= maxEdge.
        val longest = max(bitmap.width, bitmap.height)
        if (longest > safeMaxEdge) {
            val scale = safeMaxEdge.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
        }

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        bitmap.recycle()

        AiImage(id = uri, bytes = output.toByteArray(), mimeType = "image/jpeg")
    }

    override suspend fun saveGeneratedImage(
        bytes: ByteArray,
        mimeType: String,
        displayName: String,
    ): String = withContext(Dispatchers.IO) {
        val name = displayName.ifBlank { "ShotYou_${System.currentTimeMillis()}" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ShotYou")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val itemUri = resolver.insert(collection, values)
                ?: throw IOException("Failed to create MediaStore record for $name")
            try {
                resolver.openOutputStream(itemUri)?.use { it.write(bytes) }
                    ?: throw IOException("Failed to open output stream for $itemUri")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            } catch (t: Throwable) {
                runCatching { resolver.delete(itemUri, null, null) }
                throw t
            }
            itemUri.toString()
        } else {
            // Legacy direct-file fallback for API <= 28.
            @Suppress("DEPRECATION")
            val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES,
            )
            val dir = java.io.File(picturesDir, "ShotYou").apply { if (!exists()) mkdirs() }
            val file = java.io.File(dir, ensureExtension(name, mimeType))
            file.outputStream().use { it.write(bytes) }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            val saved = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: Uri.fromFile(file)
            saved.toString()
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (max(w, h) / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    private fun ensureExtension(name: String, mimeType: String): String {
        if (name.contains('.')) return name
        val ext = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "$name.$ext"
    }

    override suspend fun deletePhotos(uris: List<String>): DeleteOutcome = withContext(Dispatchers.IO) {
        val parsed = uris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        if (parsed.isEmpty()) return@withContext DeleteOutcome.Deleted(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires user consent to delete media the app doesn't own.
            val pendingIntent = MediaStore.createDeleteRequest(resolver, parsed)
            DeleteOutcome.NeedsConsent(pendingIntent.intentSender)
        } else {
            var count = 0
            parsed.forEach { uri ->
                count += runCatching { resolver.delete(uri, null, null) }.getOrDefault(0)
            }
            DeleteOutcome.Deleted(count)
        }
    }
}
