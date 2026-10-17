package com.danyastry.pqc.feature.slhdsa

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SlhDsaOnDeviceTest {

    private val slhDsa = SlhDsa()
    private val message = "Pay Alice 100 dollars".encodeToByteArray()

    @Test
    fun fastAndSmallVariantsSignAndVerifyOnTheDevice() {
        SlhDsaVariant.entries.forEach { variant ->
            val parameters = SlhDsaParameters(SlhDsaStrength.BITS_128, variant)
            val pair = slhDsa.generateKeyPair(parameters)
            val signature = slhDsa.sign(pair.private, message)

            assertThat(signature).hasLength(parameters.signatureBytes)
            assertThat(slhDsa.verify(pair.public, message, signature)).isTrue()
        }
    }

    @Test
    fun aChangedMessageFailsOnTheDevice() {
        val pair = slhDsa.generateKeyPair(SlhDsaParameters())
        val signature = slhDsa.sign(pair.private, message)

        assertThat(slhDsa.verify(pair.public, "Pay Alice 900 dollars".encodeToByteArray(), signature)).isFalse()
    }
}
