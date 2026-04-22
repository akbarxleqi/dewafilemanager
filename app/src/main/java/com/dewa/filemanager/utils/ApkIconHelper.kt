package com.dewa.filemanager.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

object ApkIconHelper {
    fun getApkIcon(context: Context, apkPath: String): ImageBitmap? {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkPath, 0) ?: return null
            val appInfo = info.applicationInfo ?: return null
            
            // Critical: Need to set sourceDir and publicSourceDir for loadIcon to work
            appInfo.sourceDir = apkPath
            appInfo.publicSourceDir = apkPath
            
            val iconDrawable = appInfo.loadIcon(pm)
            drawableToBitmap(iconDrawable).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
