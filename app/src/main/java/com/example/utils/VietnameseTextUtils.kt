package com.example.utils

import java.text.Normalizer
import java.util.Locale

// Tiện ích chuẩn hóa chuỗi tiếng Việt phục vụ tìm kiếm tức thời có dấu và không dấu
object VietnameseTextUtils {
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

    // Loại bỏ dấu tiếng Việt, chuyển chữ thường để so khớp linh hoạt
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        val nfd = Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(nfd, "")
            .replace('đ', 'd')
    }

    // Kiểm tra target có chứa từ khóa query (không phân biệt hoa/thường, có dấu/không dấu)
    fun matchesSearch(target: String, query: String): Boolean {
        if (query.isBlank()) return true
        val normalizedTarget = normalize(target)
        val normalizedQuery = normalize(query)
        return normalizedTarget.contains(normalizedQuery)
    }
}
