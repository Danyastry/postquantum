package com.danyastry.pqc.core.pqc

import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertThrows
import org.junit.Test

class AesGcmTest {

    private val key = ByteArray(32) { it.toByte() }

    @Test
    fun `opens what it sealed`() {
        val sealed = AesGcm.seal(key, "Attack at dawn".encodeToByteArray())

        assertThat(AesGcm.open(key, sealed).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `sealed data is iv, ciphertext and tag`() {
        assertThat(AesGcm.seal(key, ByteArray(10))).hasLength(12 + 10 + 16)
    }

    @Test
    fun `a fresh iv makes every result different`() {
        val message = "Hello".encodeToByteArray()

        assertThat(AesGcm.seal(key, message)).isNotEqualTo(AesGcm.seal(key, message))
    }

    @Test
    fun `another key cannot open it`() {
        val sealed = AesGcm.seal(key, "Attack at dawn".encodeToByteArray())

        assertThrows(AEADBadTagException::class.java) { AesGcm.open(ByteArray(32), sealed) }
    }

    @Test
    fun `a changed byte is detected`() {
        val sealed = AesGcm.seal(key, "Attack at dawn".encodeToByteArray())
        sealed[sealed.lastIndex] = (sealed.last().toInt() xor 1).toByte()

        assertThrows(AEADBadTagException::class.java) { AesGcm.open(key, sealed) }
    }

    @Test
    fun `needs a 256 bit key and a full message`() {
        assertThrows(IllegalArgumentException::class.java) { AesGcm.seal(ByteArray(16), ByteArray(1)) }
        assertThrows(IllegalArgumentException::class.java) { AesGcm.open(key, ByteArray(20)) }
    }
}
