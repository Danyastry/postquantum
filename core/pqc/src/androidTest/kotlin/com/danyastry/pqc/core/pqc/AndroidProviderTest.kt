package com.danyastry.pqc.core.pqc

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.security.Security
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidProviderTest {

    @Test
    fun theBuiltInProviderNamedBcHasNoPostQuantumAlgorithms() {
        val builtIn = Security.getProvider("BC")

        assertThat(builtIn?.getService("KeyPairGenerator", "ML-KEM")).isNull()
    }

    @Test
    fun ourBundledProviderHasThemOnTheDevice() {
        assertThat(BouncyCastle.supports("KeyPairGenerator", "ML-KEM")).isTrue()
        assertThat(BouncyCastle.supports("Signature", "ML-DSA")).isTrue()
        assertThat(BouncyCastle.supports("Signature", "SLH-DSA")).isTrue()
    }

    @Test
    fun ourProviderIsNotTheBuiltInOne() {
        assertThat(BouncyCastle.provider).isNotSameInstanceAs(Security.getProvider("BC"))
    }
}
