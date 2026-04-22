package com.dewa.filemanager.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

object SyntaxHighlighter {
    private val JAVA_KT_KEYWORDS = listOf("abstract", "as", "break", "class", "continue", "do", "else", "enum", "extends", "false", "final", "finally", "for", "fun", "if", "implements", "import", "in", "interface", "is", "null", "object", "package", "private", "protected", "public", "return", "super", "this", "throw", "true", "try", "typealias", "val", "var", "when", "while", "void", "static", "override", "lateinit", "suspend", "constructor")
    private val JS_TS_KEYWORDS = listOf("await", "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with", "yield", "let", "async")
    private val PHP_KEYWORDS = listOf("abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "finally", "fn", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "match", "namespace", "new", "or", "print", "private", "protected", "public", "require", "require_once", "return", "static", "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor", "yield")
    private val HTML_CSS_KEYWORDS = listOf("html", "head", "body", "div", "span", "a", "img", "form", "input", "button", "p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "table", "tr", "td", "th", "color", "background", "margin", "padding", "border", "display", "position", "flex", "grid", "width", "height", "font", "text", "align")
    
    // VS Code Dark+ theme colors for better visibility on dark backgrounds
    private val KEYWORD_COLOR = Color(0xFF569CD6) // Blue
    private val STRING_COLOR = Color(0xFFCE9178)  // Orange-ish
    private val COMMENT_COLOR = Color(0xFF6A9955) // Green
    private val TAG_COLOR = Color(0xFF4EC9B0)     // Teal

    fun highlight(text: String, extension: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)

            val keywords = when (extension.lowercase()) {
                "js", "ts", "mjs" -> JS_TS_KEYWORDS
                "php" -> PHP_KEYWORDS
                "html", "htm", "css", "xml" -> HTML_CSS_KEYWORDS
                else -> JAVA_KT_KEYWORDS
            }

            // Highlight Keywords
            val keywordPattern = Pattern.compile("\\b(${keywords.joinToString("|")})\\b")
            val keywordMatcher = keywordPattern.matcher(text)
            while (keywordMatcher.find()) {
                addStyle(
                    SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold),
                    keywordMatcher.start(),
                    keywordMatcher.end()
                )
            }

            // Highlight HTML/XML Tags if applicable
            if (extension.lowercase() in listOf("html", "htm", "xml", "php")) {
                val tagPattern = Pattern.compile("</?[a-zA-Z0-9]+.*?>")
                val tagMatcher = tagPattern.matcher(text)
                while (tagMatcher.find()) {
                    addStyle(SpanStyle(color = TAG_COLOR), tagMatcher.start(), tagMatcher.end())
                }
            }

            // Highlight Strings
            val stringPattern = Pattern.compile("[\"'](.*?)[\"']")
            val stringMatcher = stringPattern.matcher(text)
            while (stringMatcher.find()) {
                addStyle(SpanStyle(color = STRING_COLOR), stringMatcher.start(), stringMatcher.end())
            }

            // Highlight Comments
            val commentPattern = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/|<!--[\\s\\S]*?-->")
            val commentMatcher = commentPattern.matcher(text)
            while (commentMatcher.find()) {
                addStyle(SpanStyle(color = COMMENT_COLOR), commentMatcher.start(), commentMatcher.end())
            }
        }
    }
}
