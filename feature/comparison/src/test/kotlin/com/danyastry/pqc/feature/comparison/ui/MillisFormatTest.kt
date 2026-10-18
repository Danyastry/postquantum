package com.danyastry.pqc.feature.comparison.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MillisFormatTest {

    @Test
    fun `shows more digits for faster operations`() {
        assertThat(millis(0.123)).isEqualTo("0.12 ms")
        assertThat(millis(12.34)).isEqualTo("12.3 ms")
        assertThat(millis(1_234.5)).isEqualTo("1235 ms")
    }
}
