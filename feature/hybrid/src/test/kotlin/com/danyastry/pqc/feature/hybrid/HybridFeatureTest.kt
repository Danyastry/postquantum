package com.danyastry.pqc.feature.hybrid

import com.danyastry.pqc.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HybridFeatureTest {

    @Test
    fun `is listed under hybrid and works on every supported version`() {
        assertThat(HybridFeature.info.id).isEqualTo("hybrid")
        assertThat(HybridFeature.info.category).isEqualTo(FeatureCategory.HYBRID)
        assertThat(HybridFeature.info.minSdk).isEqualTo(26)
    }
}
