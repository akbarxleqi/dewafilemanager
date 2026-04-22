package com.dewa.filemanager.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toReadableSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.2f %s", this / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun Long.toReadableDate(): String {
    val sdf = SimpleDateFormat("dd-MM-yy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}
