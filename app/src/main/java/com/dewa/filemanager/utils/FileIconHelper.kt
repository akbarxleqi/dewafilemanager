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
        val color: Color
    )

    fun getIconSpecs(file: FileEntity): IconSpecs {
        if (file.isDirectory) {
            return IconSpecs(Icons.Default.Folder, Color(0xFFFFA000))
        }

        val extension = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        
        return when (extension) {
            "pdf" -> IconSpecs(Icons.Default.PictureAsPdf, Color(0xFFF44336))
            
            "apk" -> IconSpecs(Icons.Default.Android, Color(0xFF4CAF50))
            
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> 
                IconSpecs(Icons.Default.Image, Color(0xFF9C27B0))
            
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm" -> 
                IconSpecs(Icons.Default.VideoFile, Color(0xFF2196F3))
            
            "mp3", "wav", "flac", "ogg", "m4a", "aac" -> 
                IconSpecs(Icons.Default.AudioFile, Color(0xFFFF5722))
            
            "zip", "rar", "7z", "tar", "gz", "bz2" -> 
                IconSpecs(Icons.Default.FolderZip, Color(0xFFFFC107))
            
            "doc", "docx", "odt", "rtf" -> 
                IconSpecs(Icons.Default.Description, Color(0xFF1976D2))
            
            "xls", "xlsx", "csv", "ods" -> 
                IconSpecs(Icons.Default.TableChart, Color(0xFF2E7D32))
            
            "ppt", "pptx", "odp" -> 
                IconSpecs(Icons.Default.CoPresent, Color(0xFFFB8C00))
            
            "txt", "md", "log", "conf" -> 
                IconSpecs(Icons.Default.Article, Color(0xFF78909C))
            
            "xml", "java", "kt", "js", "ts", "html", "css", "php", "py", 
            "c", "cpp", "h", "hpp", "go", "rs", "json", "sh", "bat" -> 
                IconSpecs(Icons.Default.Code, Color(0xFF009688))
            
            else -> IconSpecs(Icons.Default.Description, Color(0xFF90A4AE))
        }
    }
}
