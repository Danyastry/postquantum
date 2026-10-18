package com.danyastry.pqc.feature.comparison

import com.google.common.truth.Truth.assertThat
import java.security.KeyPair
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec
import org.junit.Assert.assertThrows
import org.junit.Test

class BenchmarkTest {

    @Test
    fun `reports the median of the timed runs in milliseconds`() {
        val keyGen = listOf(5L, 1L, 3L)
        val send = listOf(2L, 2L, 9L)
        val receive = listOf(4L, 6L, 5L)
        val benchmark = Benchmark(clock = scriptedClock(millis = keyGen + send + receive), runs = 3, warmups = 0)

        val result = benchmark.keyExchange(X25519Exchange())

        assertThat(result.keyGenMillis).isEqualTo(3.0)
        assertThat(result.sendMillis).isEqualTo(2.0)
        assertThat(result.receiveMillis).isEqualTo(5.0)
    }

    @Test
    fun `measures sizes from the real keys`() {
        val result = Benchmark(runs = 1, warmups = 0).keyExchange(MlKemExchange(MLKEMParameterSpec.ml_kem_768))

        assertThat(result.publicKeyBytes).isEqualTo(1_184)
        assertThat(result.wireBytes).isEqualTo(1_088)
        assertThat(result.isQuantumSafe).isTrue()
    }

    @Test
    fun `measures signatures`() {
        val result = Benchmark(runs = 1, warmups = 0).signature(SignatureSchemes.all.first { it.name == "Ed25519" })

        assertThat(result.publicKeyBytes).isEqualTo(32)
        assertThat(result.signatureBytes).isEqualTo(64)
        assertThat(result.isQuantumSafe).isFalse()
    }

    @Test
    fun `a scheme that gives the wrong secret fails loudly`() {
        val broken = object : KeyExchangeScheme by X25519Exchange() {
            override fun receive(own: KeyPair, wire: ByteArray): ByteArray = ByteArray(32)
        }

        assertThrows(IllegalStateException::class.java) { Benchmark(runs = 1, warmups = 0).keyExchange(broken) }
    }

    private fun scriptedClock(millis: List<Long>): () -> Long {
        val ticks = millis.flatMap { listOf(0L, it * 1_000_000) }.iterator()
        return { ticks.next() }
    }
}
