package com.danyastry.pqc.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.pqc.core.ui.FeatureCategory
import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.core.ui.FeatureInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class FeatureCatalogTest {

    @Test
    fun `empty catalog has no sections`() {
        val catalog = FeatureCatalog(emptyList(), deviceSdk = 35)

        assertThat(catalog.isEmpty).isTrue()
        assertThat(catalog.sections).isEmpty()
    }

    @Test
    fun `groups features into sections ordered by category`() {
        val mlKem = feature("ml-kem", "ML-KEM", FeatureCategory.KEY_EXCHANGE)
        val mlDsa = feature("ml-dsa", "ML-DSA", FeatureCategory.SIGNATURES)
        val hybrid = feature("hybrid", "Hybrid", FeatureCategory.HYBRID)

        val catalog = FeatureCatalog(listOf(hybrid, mlDsa, mlKem), deviceSdk = 35)

        assertThat(catalog.sections.map { it.category }).containsExactly(
            FeatureCategory.KEY_EXCHANGE,
            FeatureCategory.SIGNATURES,
            FeatureCategory.HYBRID,
        ).inOrder()
    }

    @Test
    fun `sorts features inside a section by name`() {
        val slhDsa = feature("slh-dsa", "SLH-DSA", FeatureCategory.SIGNATURES)
        val mlDsa = feature("ml-dsa", "ML-DSA", FeatureCategory.SIGNATURES)

        val catalog = FeatureCatalog(listOf(slhDsa, mlDsa), deviceSdk = 35)

        assertThat(catalog.sections.single().entries).containsExactly(mlDsa, slhDsa).inOrder()
    }

    @Test
    fun `features newer than the device are listed but not supported`() {
        val future = feature("future", "Future scheme", FeatureCategory.KEY_EXCHANGE, minSdk = 31)

        val catalog = FeatureCatalog(listOf(future), deviceSdk = 30)

        assertThat(catalog.sections.single().entries).containsExactly(future)
        assertThat(catalog.isSupported(future)).isFalse()
        assertThat(catalog.find("future")).isNull()
    }

    @Test
    fun `finds supported features by id`() {
        val future = feature("future", "Future scheme", FeatureCategory.KEY_EXCHANGE, minSdk = 31)

        val catalog = FeatureCatalog(listOf(future), deviceSdk = 31)

        assertThat(catalog.isSupported(future)).isTrue()
        assertThat(catalog.find("future")).isSameInstanceAs(future)
        assertThat(catalog.find("unknown")).isNull()
    }

    @Test
    fun `rejects duplicate ids`() {
        val first = feature("ml-kem", "ML-KEM", FeatureCategory.KEY_EXCHANGE)
        val second = feature("ml-kem", "Kyber", FeatureCategory.KEY_EXCHANGE)

        val error = assertThrows(IllegalArgumentException::class.java) { FeatureCatalog(listOf(first, second), deviceSdk = 35) }
        assertThat(error).hasMessageThat().contains("ml-kem")
    }

    @Test
    fun `registry has unique ids`() {
        FeatureCatalog(FeatureRegistry.features, deviceSdk = 35)
    }

    private fun feature(id: String, name: String, category: FeatureCategory, minSdk: Int = 26): FeatureEntry =
        object : FeatureEntry {
            override val info = FeatureInfo(id = id, name = name, category = category, summary = "$name summary", minSdk = minSdk)

            @Composable
            override fun Content(modifier: Modifier) = Unit
        }
}
