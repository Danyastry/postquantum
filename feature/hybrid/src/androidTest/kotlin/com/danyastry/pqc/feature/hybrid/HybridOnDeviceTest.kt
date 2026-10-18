package com.danyastry.pqc.feature.hybrid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danyastry.pqc.core.pqc.AesGcm
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HybridOnDeviceTest {

    private val exchange = HybridKeyExchange()

    @Test
    fun theHandshakeAgreesOnTheDevice() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)

        val result = exchange.clientFinish(client, server.share)

        assertThat(client.share).hasLength(HybridKeyExchange.CLIENT_SHARE_BYTES)
        assertThat(server.share).hasLength(HybridKeyExchange.SERVER_SHARE_BYTES)
        assertThat(result.sharedSecret).isEqualTo(server.sharedSecret)
    }

    @Test
    fun theSessionKeyCarriesAMessageOnTheDevice() {
        val client = exchange.clientHello()
        val server = exchange.serverRespond(client.share)
        val sealed = AesGcm.seal(HybridKeyExchange.sessionKey(server.sharedSecret), "Attack at dawn".encodeToByteArray())

        val result = exchange.clientFinish(client, server.share)

        assertThat(AesGcm.open(HybridKeyExchange.sessionKey(result.sharedSecret), sealed).decodeToString()).isEqualTo("Attack at dawn")
    }
}
