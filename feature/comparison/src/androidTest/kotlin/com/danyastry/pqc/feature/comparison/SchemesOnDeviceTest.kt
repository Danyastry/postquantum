package com.danyastry.pqc.feature.comparison

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchemesOnDeviceTest {

    private val benchmark = Benchmark()

    @Test
    fun everyKeyExchangeWorksOnTheDevice() {
        KeyExchangeSchemes.all.forEach { scheme ->
            val result = benchmark.keyExchange(scheme)
            Log.i(TAG, result.toString())

            assertThat(result.wireBytes).isGreaterThan(0)
        }
    }

    @Test
    fun everySignatureWorksOnTheDevice() {
        SignatureSchemes.all.forEach { scheme ->
            val result = benchmark.signature(scheme)
            Log.i(TAG, result.toString())

            assertThat(result.signatureBytes).isGreaterThan(0)
        }
    }

    private companion object {
        const val TAG = "SchemesOnDevice"
    }
}
