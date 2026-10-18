package com.danyastry.pqc.feature.comparison

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class SchemesTest {

    private val message = "Pay Alice 100 dollars".encodeToByteArray()

    @Test
    fun `every key exchange delivers the same secret to the receiver`() {
        KeyExchangeSchemes.all.forEach { scheme ->
            val receiver = scheme.generateKeyPair()
            val delivery = scheme.send(receiver.public)

            assertWithMessage(scheme.name).that(scheme.receive(receiver, delivery.wire)).isEqualTo(delivery.secret)
        }
    }

    @Test
    fun `key exchange sizes match the standards`() {
        val expected = mapOf(
            "X25519" to (32 to 32),
            "ECDH P-256" to (65 to 65),
            "RSA-3072" to (384 to 384),
            "ML-KEM-512" to (800 to 768),
            "ML-KEM-768" to (1_184 to 1_088),
            "ML-KEM-1024" to (1_568 to 1_568),
        )
        KeyExchangeSchemes.all.forEach { scheme ->
            val receiver = scheme.generateKeyPair()
            val (publicKey, wire) = expected.getValue(scheme.name)

            assertWithMessage(scheme.name).that(scheme.publicKeyBytes(receiver.public)).isEqualTo(publicKey)
            assertWithMessage(scheme.name).that(scheme.send(receiver.public).wire).hasLength(wire)
        }
    }

    @Test
    fun `every signature verifies and rejects a changed message`() {
        SignatureSchemes.all.forEach { scheme ->
            val pair = scheme.generateKeyPair()
            val signature = scheme.sign(pair.private, message)

            assertWithMessage(scheme.name).that(scheme.verify(pair.public, message, signature)).isTrue()
            assertWithMessage(scheme.name).that(scheme.verify(pair.public, "Pay Alice 900 dollars".encodeToByteArray(), signature)).isFalse()
        }
    }

    @Test
    fun `signature sizes match the standards`() {
        val expected = mapOf(
            "Ed25519" to (32 to 64..64),
            "ECDSA P-256" to (65 to 70..72),
            "RSA-3072" to (384 to 384..384),
            "ML-DSA-44" to (1_312 to 2_420..2_420),
            "ML-DSA-65" to (1_952 to 3_309..3_309),
            "ML-DSA-87" to (2_592 to 4_627..4_627),
            "SLH-DSA-SHA2-128f" to (32 to 17_088..17_088),
            "SLH-DSA-SHA2-128s" to (32 to 7_856..7_856),
        )
        SignatureSchemes.all.forEach { scheme ->
            val pair = scheme.generateKeyPair()
            val (publicKey, signatureRange) = expected.getValue(scheme.name)

            assertWithMessage(scheme.name).that(scheme.publicKeyBytes(pair.public)).isEqualTo(publicKey)
            assertWithMessage(scheme.name).that(scheme.sign(pair.private, message).size).isIn(signatureRange)
        }
    }

    @Test
    fun `only the nist post-quantum schemes are marked quantum safe`() {
        assertThat(KeyExchangeSchemes.all.filter { it.isQuantumSafe }.map { it.name }).containsExactly("ML-KEM-512", "ML-KEM-768", "ML-KEM-1024")
        assertThat(SignatureSchemes.all.filter { it.isQuantumSafe }.map { it.name })
            .containsExactly("ML-DSA-44", "ML-DSA-65", "ML-DSA-87", "SLH-DSA-SHA2-128f", "SLH-DSA-SHA2-128s")
    }
}
