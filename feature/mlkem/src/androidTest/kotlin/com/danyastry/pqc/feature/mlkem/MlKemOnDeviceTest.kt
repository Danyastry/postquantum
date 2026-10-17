package com.danyastry.pqc.feature.mlkem

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyastry.pqc.core.pqc.AesGcm
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MlKemOnDeviceTest {

    private val mlKem = MlKem()

    @Test
    fun everyLevelAgreesOnTheDevice() {
        MlKemLevel.entries.forEach { level ->
            val bob = mlKem.generateKeyPair(level)
            val alice = mlKem.encapsulate(bob.public)

            assertThat(alice.ciphertext).hasLength(level.ciphertextBytes)
            assertThat(mlKem.decapsulate(bob.private, alice.ciphertext)).isEqualTo(alice.sharedSecret)
        }
    }

    @Test
    fun aFlippedBitIsRejectedSilentlyOnTheDevice() {
        val bob = mlKem.generateKeyPair(MlKemLevel.ML_KEM_768)
        val alice = mlKem.encapsulate(bob.public)
        val tampered = alice.ciphertext.copyOf().also { it[100] = (it[100].toInt() xor 1).toByte() }

        assertThat(mlKem.decapsulate(bob.private, tampered)).isNotEqualTo(alice.sharedSecret)
    }

    @Test
    fun theSecretEncryptsAMessageOnTheDevice() {
        val bob = mlKem.generateKeyPair(MlKemLevel.ML_KEM_1024)
        val alice = mlKem.encapsulate(bob.public)
        val sealed = AesGcm.seal(alice.sharedSecret, "Attack at dawn".encodeToByteArray())

        assertThat(AesGcm.open(mlKem.decapsulate(bob.private, alice.ciphertext), sealed).decodeToString()).isEqualTo("Attack at dawn")
    }
}
