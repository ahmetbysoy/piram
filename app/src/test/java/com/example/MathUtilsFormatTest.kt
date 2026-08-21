package com.example

import com.example.core.util.MathUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class MathUtilsFormatTest {

    @Test
    fun `tickSize hane ile fiyat`() {
        assertEquals("65000.00", MathUtils.formatPrice(65000.0, 2))
        assertEquals("0.00001234", MathUtils.formatPrice(0.00001234, 8))
        assertEquals("150.0", MathUtils.formatPrice(150.0, 1))
        assertEquals("3", MathUtils.formatPrice(3.0, 0))
    }

    @Test
    fun `kisa kesir sifirla tamamlanir`() {
        assertEquals("65000.50", MathUtils.formatPrice(65000.5, 2))
        assertEquals("0.10000000", MathUtils.formatPrice(0.1, 8))
    }

    @Test
    fun `uzun kesir kirpilir`() {
        assertEquals("65000.12", MathUtils.formatPrice(65000.123456, 2))
    }

    @Test
    fun `negatif decimals otomatik biçime duser`() {
        assertEquals(MathUtils.formatPrice(65000.5), MathUtils.formatPrice(65000.5, -1))
    }

    @Test
    fun `decimalsFromTickSize`() {
        assertEquals(2, MathUtils.decimalsFromTickSize("0.01"))
        assertEquals(8, MathUtils.decimalsFromTickSize("0.00000001"))
        assertEquals(0, MathUtils.decimalsFromTickSize("1"))
    }
}
