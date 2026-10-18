package com.danyastry.pqc.feature.hybrid

import com.danyastry.pqc.core.pqc.AesGcm
import com.google.common.truth.Truth.assertThat
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertThrows
import org.junit.Test

class HybridKeyExchangeTest {

    private val exchange = HybridKeyExchange()

    @Test
    fun `key shares have the tls x25519mlkem768 sizes`() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)

        assertThat(client.share).hasLength(1_216)
        assertThat(server.share).hasLength(1_120)
    }

    @Test
    fun `both sides get the same 64 byte secret`() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)

        val result = exchange.clientFinish(client, server.share)

        assertThat(result.sharedSecret).hasLength(64)
        assertThat(result.sharedSecret).isEqualTo(server.sharedSecret)
    }

    @Test
    fun `the secret is the ml-kem part followed by the x25519 part`() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)

        val result = exchange.clientFinish(client, server.share)

        assertThat(result.sharedSecret.copyOfRange(0, 32)).isEqualTo(server.mlKemSecret)
        assertThat(result.sharedSecret.copyOfRange(32, 64)).isEqualTo(server.x25519Secret)
    }

    @Test
    fun `the session key comes from both parts`() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)
        val sessionKey = HybridKeyExchange.sessionKey(server.sharedSecret)

        val withoutMlKem = HybridKeyExchange.sessionKey(ByteArray(32) + server.x25519Secret)
        val withoutX25519 = HybridKeyExchange.sessionKey(server.mlKemSecret + ByteArray(32))

        assertThat(sessionKey).hasLength(32)
        assertThat(withoutMlKem).isNotEqualTo(sessionKey)
        assertThat(withoutX25519).isNotEqualTo(sessionKey)
    }

    @Test
    fun `knowing only one part does not open the message`() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)
        val sealed = AesGcm.seal(HybridKeyExchange.sessionKey(server.sharedSecret), "Attack at dawn".encodeToByteArray())

        val guessOnlyX25519 = HybridKeyExchange.sessionKey(ByteArray(32) + server.x25519Secret)

        assertThrows(AEADBadTagException::class.java) { AesGcm.open(guessOnlyX25519, sealed) }
    }

    @Test
    fun `the client opens what the server sealed`() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)
        val sealed = AesGcm.seal(HybridKeyExchange.sessionKey(server.sharedSecret), "Attack at dawn".encodeToByteArray())

        val result = exchange.clientFinish(client, server.share)

        assertThat(AesGcm.open(HybridKeyExchange.sessionKey(result.sharedSecret), sealed).decodeToString()).isEqualTo("Attack at dawn")
    }

    @Test
    fun `a changed server share breaks the agreement`() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)
        val tampered = server.share.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }

        assertThat(exchange.clientFinish(client, tampered).sharedSecret).isNotEqualTo(server.sharedSecret)
    }

    @Test
    fun `every handshake makes a new secret`() {
        val first = exchange.serverRespond(exchange.clientHello().share)
        val second = exchange.serverRespond(exchange.clientHello().share)

        assertThat(first.sharedSecret).isNotEqualTo(second.sharedSecret)
    }

    @Test
    fun `rejects key shares of the wrong size`() {
        val client = exchange.clientHello()

        assertThrows(IllegalArgumentException::class.java) { exchange.serverRespond(ByteArray(32)) }
        assertThrows(IllegalArgumentException::class.java) { exchange.clientFinish(client, ByteArray(32)) }
    }
}
