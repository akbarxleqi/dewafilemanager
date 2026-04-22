package com.dewa.filemanager.utils

import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

object ImageThumbnailHelper {
    fun getThumbnail(path: String, maxSize: Int = 150): ImageBitmap? {
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)

            var scale = 1
            while (options.outWidth / scale / 2 >= maxSize && options.outHeight / scale / 2 >= maxSize) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // Memory efficient
            }

            val bitmap = BitmapFactory.decodeFile(path, decodeOptions)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getVideoThumbnail(path: String, maxSize: Int = 150): ImageBitmap? {
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(file, Size(maxSize, maxSize), null)
            } else {
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND)
            }
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
