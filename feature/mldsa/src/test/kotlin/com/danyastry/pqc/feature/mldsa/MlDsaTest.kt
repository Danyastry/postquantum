package com.danyastry.pqc.feature.mldsa

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class MlDsaTest {

    private val mlDsa = MlDsa()
    private val message = "Pay Alice 100 dollars".encodeToByteArray()
    private val payments = "payments".encodeToByteArray()

    @Test
    fun `key and signature sizes match the fips 204 table`() {
        MlDsaLevel.entries.forEach { level ->
            val pair = mlDsa.generateKeyPair(level)

            assertThat(MlDsa.publicKeyBytes(pair.public)).hasLength(level.publicKeyBytes)
            assertThat(MlDsa.privateKeyBytes(pair.private)).hasLength(level.privateKeyBytes)
            assertThat(mlDsa.sign(pair.private, message)).hasLength(level.signatureBytes)
        }
    }

    @Test
    fun `the private key is stored as a 32 byte seed`() {
        assertThat(MlDsa.seedBytes(mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65).private)).hasLength(MlDsa.SEED_BYTES)
    }

    @Test
    fun `a signature verifies for the same message and key`() {
        MlDsaLevel.entries.forEach { level ->
            val pair = mlDsa.generateKeyPair(level)

            assertThat(mlDsa.verify(pair.public, message, mlDsa.sign(pair.private, message))).isTrue()
        }
    }

    @Test
    fun `a changed message fails`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)
        val signature = mlDsa.sign(pair.private, message)

        assertThat(mlDsa.verify(pair.public, "Pay Alice 900 dollars".encodeToByteArray(), signature)).isFalse()
    }

    @Test
    fun `a changed signature fails`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)
        val signature = mlDsa.sign(pair.private, message).also { it[10] = (it[10].toInt() xor 1).toByte() }

        assertThat(mlDsa.verify(pair.public, message, signature)).isFalse()
    }

    @Test
    fun `another public key fails`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)
        val other = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)

        assertThat(mlDsa.verify(other.public, message, mlDsa.sign(pair.private, message))).isFalse()
    }

    @Test
    fun `a signature of the wrong length fails instead of crashing`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)

        assertThat(mlDsa.verify(pair.public, message, ByteArray(10))).isFalse()
    }

    @Test
    fun `hedged signing gives a new signature every time`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)

        assertThat(mlDsa.sign(pair.private, message, mode = SigningMode.HEDGED))
            .isNotEqualTo(mlDsa.sign(pair.private, message, mode = SigningMode.HEDGED))
    }

    @Test
    fun `deterministic signing gives the same signature every time`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)

        assertThat(mlDsa.sign(pair.private, message, mode = SigningMode.DETERMINISTIC))
            .isEqualTo(mlDsa.sign(pair.private, message, mode = SigningMode.DETERMINISTIC))
    }

    @Test
    fun `both modes verify the same way`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_44)

        SigningMode.entries.forEach { mode ->
            assertThat(mlDsa.verify(pair.public, message, mlDsa.sign(pair.private, message, mode = mode))).isTrue()
        }
    }

    @Test
    fun `a signature only verifies with the same context`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)
        val signature = mlDsa.sign(pair.private, message, payments)

        assertThat(mlDsa.verify(pair.public, message, signature, payments)).isTrue()
        assertThat(mlDsa.verify(pair.public, message, signature, "login".encodeToByteArray())).isFalse()
        assertThat(mlDsa.verify(pair.public, message, signature)).isFalse()
    }

    @Test
    fun `an empty context is the same as no context`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_65)

        assertThat(mlDsa.verify(pair.public, message, mlDsa.sign(pair.private, message, ByteArray(0)))).isTrue()
    }

    @Test
    fun `context is limited to 255 bytes`() {
        val pair = mlDsa.generateKeyPair(MlDsaLevel.ML_DSA_44)

        assertThat(mlDsa.sign(pair.private, message, ByteArray(255))).isNotEmpty()
        assertThrows(IllegalArgumentException::class.java) { mlDsa.sign(pair.private, message, ByteArray(256)) }
    }

    @Test
    fun `levels map to nist categories`() {
        assertThat(MlDsaLevel.entries.map { it.nistCategory }).containsExactly(2, 3, 5).inOrder()
    }
}
