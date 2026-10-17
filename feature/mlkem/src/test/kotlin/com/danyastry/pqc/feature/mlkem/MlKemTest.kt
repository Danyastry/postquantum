package com.danyastry.pqc.feature.mlkem

import com.danyastry.pqc.core.pqc.AesGcm
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MlKemTest {

    private val mlKem = MlKem()

    @Test
    fun `key and ciphertext sizes match the fips 203 table`() {
        MlKemLevel.entries.forEach { level ->
            val pair = mlKem.generateKeyPair(level)
            val encapsulation = mlKem.encapsulate(pair.public)

            assertThat(MlKem.encapsulationKeyBytes(pair.public)).hasLength(level.encapsulationKeyBytes)
            assertThat(MlKem.decapsulationKeyBytes(pair.private)).hasLength(level.decapsulationKeyBytes)
            assertThat(encapsulation.ciphertext).hasLength(level.ciphertextBytes)
            assertThat(encapsulation.sharedSecret).hasLength(MlKem.SHARED_SECRET_BYTES)
        }
    }

    @Test
    fun `the private key is stored as a 64 byte seed`() {
        val pair = mlKem.generateKeyPair(MlKemLevel.ML_KEM_768)

        assertThat(MlKem.seedBytes(pair.private)).hasLength(MlKem.SEED_BYTES)
    }

    @Test
    fun `both sides end up with the same secret`() {
        MlKemLevel.entries.forEach { level ->
            val bob = mlKem.generateKeyPair(level)
            val alice = mlKem.encapsulate(bob.public)

            assertThat(mlKem.decapsulate(bob.private, alice.ciphertext)).isEqualTo(alice.sharedSecret)
        }
    }

    @Test
    fun `every encapsulation makes a new secret`() {
        val bob = mlKem.generateKeyPair(MlKemLevel.ML_KEM_768)

        assertThat(mlKem.encapsulate(bob.public).sharedSecret).isNotEqualTo(mlKem.encapsulate(bob.public).sharedSecret)
    }

    @Test
    fun `a changed ciphertext gives a different secret instead of an error`() {
        val bob = mlKem.generateKeyPair(MlKemLevel.ML_KEM_768)
        val alice = mlKem.encapsulate(bob.public)
        val tampered = alice.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        val bobSecret = mlKem.decapsulate(bob.private, tampered)

        assertThat(bobSecret).hasLength(MlKem.SHARED_SECRET_BYTES)
        assertThat(bobSecret).isNotEqualTo(alice.sharedSecret)
    }

    @Test
    fun `the wrong private key gives a different secret`() {
        val bob = mlKem.generateKeyPair(MlKemLevel.ML_KEM_768)
        val eve = mlKem.generateKeyPair(MlKemLevel.ML_KEM_768)
        val alice = mlKem.encapsulate(bob.public)

        assertThat(mlKem.decapsulate(eve.private, alice.ciphertext)).isNotEqualTo(alice.sharedSecret)
    }

    @Test
    fun `the shared secret works as an aes-256 key`() {
        val bob = mlKem.generateKeyPair(MlKemLevel.ML_KEM_768)
        val alice = mlKem.encapsulate(bob.public)

        val sealed = AesGcm.seal(alice.sharedSecret, "Attack at dawn".encodeToByteArray())

        assertThat(AesGcm.open(mlKem.decapsulate(bob.private, alice.ciphertext), sealed).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `levels map to nist categories and aes strength`() {
        assertThat(MlKemLevel.entries.map { it.nistCategory }).containsExactly(1, 3, 5).inOrder()
        assertThat(MlKemLevel.entries.map { it.comparableAesBits }).containsExactly(128, 192, 256).inOrder()
    }
}
