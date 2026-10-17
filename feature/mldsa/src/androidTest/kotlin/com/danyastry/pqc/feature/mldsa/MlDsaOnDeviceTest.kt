package com.danyastry.pqc.feature.mldsa

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MlDsaOnDeviceTest {

    private val mlDsa = MlDsa()
    private val message = "Pay Alice 100 dollars".encodeToByteArray()

    @Test
    fun everyLevelSignsAndVerifiesOnTheDevice() {
        MlDsaLevel.entries.forEach { level ->
            val pair = mlDsa.generateKeyPair(level)
            val signature = mlDsa.sign(pair.private, message)

            assertThat(signature).hasLength(level.signatureBytes)
            assertThat(mlDsa.verify(pair.public, message, signature)).isTrue()
        }
    }

    @Test
    fun aChangedMessageFailsOnTheDevice() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)
        val signature = mlDsa.sign(pair.private, message)

        assertThat(mlDsa.verify(pair.public, "Pay Alice 900 dollars".encodeToByteArray(), signature)).isFalse()
    }

    @Test
    fun hedgedAndDeterministicBehaveAsExpectedOnTheDevice() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_44)

        assertThat(mlDsa.sign(pair.private, message, mode = SigningMode.HEDGED)).isNotEqualTo(mlDsa.sign(pair.private, message, mode = SigningMode.HEDGED))
        assertThat(mlDsa.sign(pair.private, message, mode = SigningMode.DETERMINISTIC)).isEqualTo(mlDsa.sign(pair.private, message, mode = SigningMode.DETERMINISTIC))
    }
}
