package com.example

import com.example.utils.TimeFormatter
import com.example.utils.VietnameseTextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun vietnameseSearchNormalization_handlesAccentsAndCase() {
        assertTrue(VietnameseTextUtils.matchesSearch("Bóng Trăng Cổ Độ", "bong trang co do"))
        assertTrue(VietnameseTextUtils.matchesSearch("Đêm Sương Trên Bến Đò", "DEM SUONG"))
        assertTrue(VietnameseTextUtils.matchesSearch("Bạch Nhật Anh Audio", "bach nhat"))
        assertFalse(VietnameseTextUtils.matchesSearch("Hành Tinh Tĩnh Lặng", "kiem hiep"))
    }

    @Test
    fun timeFormatter_formatsMmSsAndHhMmSsProperly() {
        assertEquals("00:00", TimeFormatter.formatDuration(0L))
        assertEquals("00:54", TimeFormatter.formatDuration(54_000L))
        assertEquals("12:05", TimeFormatter.formatDuration(725_000L))
        assertEquals("01:05:09", TimeFormatter.formatDuration(3_909_000L))
    }
}
