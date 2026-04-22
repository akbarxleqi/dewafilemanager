package com.dewa.filemanager.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

object SyntaxHighlighter {
    private val KEYWORDS = listOf(
        "abstract", "as", "break", "class", "continue", "do", "else", "enum", "extends", "false",
        "final", "finally", "for", "fun", "if", "implements", "import", "in", "interface", "is",
        "null", "object", "package", "private", "protected", "public", "return", "super", "this",
        "throw", "true", "try", "typealias", "val", "var", "when", "while", "void", "static",
        "override", "lateinit", "suspend", "constructor"
    )

    private val KEYWORD_COLOR = Color(0xFFD73A49) // Red-ish
    private val STRING_COLOR = Color(0xFF032F62)  // Blue-ish
    private val COMMENT_COLOR = Color(0xFF6A737D) // Gray
    private val TYPE_COLOR = Color(0xFF6F42C1)    // Purple

    fun highlight(text: String, extension: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)

            // Highlight Keywords
            val keywordPattern = Pattern.compile("\\b(${KEYWORDS.joinToString("|")})\\b")
            val keywordMatcher = keywordPattern.matcher(text)
            while (keywordMatcher.find()) {
                addStyle(
                    SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold),
                    keywordMatcher.start(),
                    keywordMatcher.end()
                )
            }

            // Highlight Strings
            val stringPattern = Pattern.compile("\"(.*?)\"")
            val stringMatcher = stringPattern.matcher(text)
            while (stringMatcher.find()) {
                addStyle(SpanStyle(color = STRING_COLOR), stringMatcher.start(), stringMatcher.end())
            }

            // Highlight Comments
            val commentPattern = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")
            val commentMatcher = commentPattern.matcher(text)
            while (commentMatcher.find()) {
                addStyle(SpanStyle(color = COMMENT_COLOR), commentMatcher.start(), commentMatcher.end())
            }
        }
    }
}
