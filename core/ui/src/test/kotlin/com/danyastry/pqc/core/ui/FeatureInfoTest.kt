package com.danyastry.pqc.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class FeatureInfoTest {

    @Test
    fun `accepts kebab-case ids`() {
        assertThat(info(id = "ml-kem").id).isEqualTo("ml-kem")
        assertThat(info(id = "slh-dsa").id).isEqualTo("slh-dsa")
    }

    @Test
    fun `rejects ids that are not lowercase kebab-case`() {
        listOf("", "ML-KEM", "ml_kem", "ml kem", "-ml", "ml-", "ml--kem").forEach { id ->
            assertThrows(IllegalArgumentException::class.java) { info(id = id) }
        }
    }

    @Test
    fun `rejects blank names and minSdk below the baseline`() {
        assertThrows(IllegalArgumentException::class.java) { info(name = " ") }
        assertThrows(IllegalArgumentException::class.java) { info(minSdk = 25) }
    }

    @Test
    fun `defaults to the app baseline`() {
        assertThat(info().minSdk).isEqualTo(AndroidVersion.BASELINE_SDK)
    }

    @Test
    fun `is supported from its minSdk up`() {
        val ecdh = info(minSdk = 31)

        assertThat(ecdh.isSupportedOn(30)).isFalse()
        assertThat(ecdh.isSupportedOn(31)).isTrue()
        assertThat(ecdh.isSupportedOn(35)).isTrue()
    }

    private fun info(id: String = "ml-kem", name: String = "ML-KEM", minSdk: Int = AndroidVersion.BASELINE_SDK) =
        FeatureInfo(id = id, name = name, category = FeatureCategory.KEY_EXCHANGE, summary = "Summary", minSdk = minSdk)
}
