package com.dewa.filemanager.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dewa.filemanager.data.model.FileEntity
import java.util.Locale

object FileIconHelper {
    
    data class IconSpecs(
        val icon: ImageVector,
        val color: Color,
        val text: String? = null
    )

    fun getIconSpecs(file: FileEntity): IconSpecs {
        if (file.isDirectory) {
            return IconSpecs(Icons.Default.Folder, Color(0xFFFFA000))
        }

        val extension = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        
        return when (extension) {
            // Document types
            "pdf" -> IconSpecs(Icons.Default.PictureAsPdf, Color(0xFFF44336))
            "doc", "docx", "odt", "rtf" -> IconSpecs(Icons.Default.Description, Color(0xFF1976D2))
            "xls", "xlsx", "csv", "ods" -> IconSpecs(Icons.Default.TableChart, Color(0xFF2E7D32))
            "ppt", "pptx", "odp" -> IconSpecs(Icons.Default.CoPresent, Color(0xFFFB8C00))
            "txt", "md", "log", "conf", "ini", "properties" -> IconSpecs(Icons.Default.Article, Color(0xFFB0BEC5))
            
            // Media
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> IconSpecs(Icons.Default.Image, Color(0xFF9C27B0))
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm" -> IconSpecs(Icons.Default.VideoFile, Color(0xFF2196F3))
            "mp3", "wav", "flac", "ogg", "m4a", "aac" -> IconSpecs(Icons.Default.AudioFile, Color(0xFFFF5722))
            
            // Archives & Packages
            "zip", "rar", "7z", "tar", "gz", "bz2" -> IconSpecs(Icons.Default.FolderZip, Color(0xFFFFC107))
            "apk", "apks", "xapk" -> IconSpecs(Icons.Default.Android, Color(0xFF4CAF50))
            
            // Programming Languages
            "java" -> IconSpecs(Icons.Default.LocalCafe, Color(0xFFFF9600)) // Java Orange
            "kt", "kts" -> IconSpecs(Icons.Default.Code, Color(0xFF7F52FF), text = "K") // Kotlin Purple
            "js", "mjs" -> IconSpecs(Icons.Default.Javascript, Color(0xFFF7DF1E)) // JS Yellow
            "ts" -> IconSpecs(Icons.Default.Javascript, Color(0xFF3178C6)) // TS Blue
            "html", "htm" -> IconSpecs(Icons.Default.Public, Color(0xFFE34F26)) // HTML Orange
            "css", "scss", "sass", "less" -> IconSpecs(Icons.Default.Description, Color(0xFF1572B6)) // CSS Blue
            "php" -> IconSpecs(Icons.Default.Php, Color(0xFF777BB4)) // PHP Purple
            "py", "pyc", "pyw" -> IconSpecs(Icons.Default.Code, Color(0xFF3776AB)) // Python Blue
            "c" -> IconSpecs(Icons.Default.Code, Color(0xFFA8B9CC)) // C Blue
            "cpp", "cc", "cxx", "h", "hpp" -> IconSpecs(Icons.Default.Code, Color(0xFF00599C)) // C++ Blue
            "cs" -> IconSpecs(Icons.Default.Code, Color(0xFF239120)) // C# Green
            "go" -> IconSpecs(Icons.Default.Code, Color(0xFF00ADD8)) // Go Cyan
            "rs" -> IconSpecs(Icons.Default.Code, Color(0xFFDEA584)) // Rust
            "rb", "erb" -> IconSpecs(Icons.Default.Code, Color(0xFFCC342D)) // Ruby Red
            "swift" -> IconSpecs(Icons.Default.Code, Color(0xFFF05138)) // Swift Orange
            "dart" -> IconSpecs(Icons.Default.Code, Color(0xFF0175C2), text = "D") // Dart Cyan
            "smali" -> IconSpecs(Icons.Default.Code, Color(0xFF7E57C2)) // Smali Deep Purple
            "dex" -> IconSpecs(Icons.Default.SettingsSystemDaydream, Color(0xFF607D8B)) // Dex Blue Grey
            
            // Data & Config
            "json" -> IconSpecs(Icons.Default.DataObject, Color(0xFF8BC34A)) // JSON Green
            "xml" -> IconSpecs(Icons.Default.DataObject, Color(0xFFFF9800)) // XML Orange
            "yml", "yaml" -> IconSpecs(Icons.Default.DataObject, Color(0xFFCB171E)) // YAML Red
            "sql" -> IconSpecs(Icons.Default.Storage, Color(0xFF00BCD4)) // SQL Cyan
            
            // Scripts
            "sh", "bash", "zsh", "bat", "cmd" -> IconSpecs(Icons.Default.Terminal, Color(0xFF4CAF50)) // Script Green
            
            else -> IconSpecs(Icons.Default.Description, Color(0xFF90A4AE)) // Default Grey
        }
    }
}
