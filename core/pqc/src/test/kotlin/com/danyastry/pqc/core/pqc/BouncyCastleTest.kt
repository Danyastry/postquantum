package com.danyastry.pqc.core.pqc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BouncyCastleTest {

    @Test
    fun `is the version with the final nist standards`() {
        assertThat(BouncyCastle.version).isEqualTo("1.86")
    }

    @Test
    fun `offers ml-kem for key exchange`() {
        assertThat(BouncyCastle.supports("KeyPairGenerator", "ML-KEM")).isTrue()
        assertThat(BouncyCastle.supports("KeyGenerator", "ML-KEM")).isTrue()
    }

    @Test
    fun `offers ml-dsa and slh-dsa for signatures`() {
        assertThat(BouncyCastle.supports("Signature", "ML-DSA")).isTrue()
        assertThat(BouncyCastle.supports("Signature", "SLH-DSA")).isTrue()
    }

    @Test
    fun `still offers the classical algorithms to compare with`() {
        assertThat(BouncyCastle.supports("KeyAgreement", "X25519")).isTrue()
        assertThat(BouncyCastle.supports("Signature", "Ed25519")).isTrue()
    }

    @Test
    fun `knows nothing it should not`() {
        assertThat(BouncyCastle.supports("Signature", "NOT-A-REAL-SCHEME")).isFalse()
    }
}
