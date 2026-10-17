package com.danyastry.pqc.core.pqc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FingerprintTest {

    @Test
    fun `shows the first 8 bytes of sha-256`() {
        assertThat(Fingerprint.of("abc".encodeToByteArray())).isEqualTo("ba:78:16:bf:8f:01:cf:ea")
    }

    @Test
    fun `different keys get different fingerprints`() {
        assertThat(Fingerprint.of(byteArrayOf(1))).isNotEqualTo(Fingerprint.of(byteArrayOf(2)))
    }
}
