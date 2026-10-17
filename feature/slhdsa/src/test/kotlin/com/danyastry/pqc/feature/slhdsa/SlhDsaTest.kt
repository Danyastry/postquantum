package com.danyastry.pqc.feature.slhdsa

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlhDsaTest {

    private val slhDsa = SlhDsa()
    private val message = "Pay Alice 100 dollars".encodeToByteArray()
    private val allParameters = SlhDsaStrength.entries.flatMap { strength ->
        SlhDsaVariant.entries.flatMap { variant -> HashFamily.entries.map { hash -> SlhDsaParameters(strength, variant, hash) } }
    }

    @Test
    fun `covers all twelve fips 205 parameter sets`() {
        assertThat(allParameters.map { it.spec }.toSet()).hasSize(12)
    }

    @Test
    fun `names follow the standard`() {
        assertThat(SlhDsaParameters(SlhDsaStrength.BITS_128, SlhDsaVariant.SMALL, HashFamily.SHA2).name).isEqualTo("SLH-DSA-SHA2-128s")
        assertThat(SlhDsaParameters(SlhDsaStrength.BITS_256, SlhDsaVariant.FAST, HashFamily.SHAKE).name).isEqualTo("SLH-DSA-SHAKE-256f")
        allParameters.forEach { assertThat(it.spec.name).ignoringCase().isEqualTo(it.name) }
    }

    @Test
    fun `fast parameter sets have the fips 205 sizes and verify`() {
        allParameters.filter { it.variant == SlhDsaVariant.FAST }.forEach { assertSizesAndRoundTrip(it) }
    }

    @Test
    fun `small sha2 parameter sets have the fips 205 sizes and verify`() {
        allParameters.filter { it.variant == SlhDsaVariant.SMALL && it.hash == HashFamily.SHA2 }.forEach { assertSizesAndRoundTrip(it) }
    }

    @Test
    fun `small signatures are smaller than fast ones at every strength`() {
        SlhDsaStrength.entries.forEach { strength ->
            assertThat(SlhDsaParameters(strength, SlhDsaVariant.SMALL).signatureBytes)
                .isLessThan(SlhDsaParameters(strength, SlhDsaVariant.FAST).signatureBytes)
        }
    }

    @Test
    fun `a changed message fails`() {
        val pair = slhDsa.generateKeyPair(SlhDsaParameters())
        val signature = slhDsa.sign(pair.private, message)

        assertThat(slhDsa.verify(pair.public, "Pay Alice 900 dollars".encodeToByteArray(), signature)).isFalse()
    }

    @Test
    fun `a changed signature fails`() {
        val pair = slhDsa.generateKeyPair(SlhDsaParameters())
        val signature = slhDsa.sign(pair.private, message).also { it[100] = (it[100].toInt() xor 1).toByte() }

        assertThat(slhDsa.verify(pair.public, message, signature)).isFalse()
    }

    @Test
    fun `another public key fails`() {
        val pair = slhDsa.generateKeyPair(SlhDsaParameters())
        val other = slhDsa.generateKeyPair(SlhDsaParameters())

        assertThat(slhDsa.verify(other.public, message, slhDsa.sign(pair.private, message))).isFalse()
    }

    @Test
    fun `a signature of the wrong length fails instead of crashing`() {
        val pair = slhDsa.generateKeyPair(SlhDsaParameters())

        assertThat(slhDsa.verify(pair.public, message, ByteArray(10))).isFalse()
    }

    @Test
    fun `every signature is randomized`() {
        val pair = slhDsa.generateKeyPair(SlhDsaParameters())

        assertThat(slhDsa.sign(pair.private, message)).isNotEqualTo(slhDsa.sign(pair.private, message))
    }

    private fun assertSizesAndRoundTrip(parameters: SlhDsaParameters) {
        val pair = slhDsa.generateKeyPair(parameters)
        val signature = slhDsa.sign(pair.private, message)

        assertThat(SlhDsa.publicKeyBytes(pair.public)).hasLength(parameters.strength.publicKeyBytes)
        assertThat(SlhDsa.privateKeyBytes(pair.private)).hasLength(parameters.strength.privateKeyBytes)
        assertThat(signature).hasLength(parameters.signatureBytes)
        assertThat(slhDsa.verify(pair.public, message, signature)).isTrue()
    }
}
