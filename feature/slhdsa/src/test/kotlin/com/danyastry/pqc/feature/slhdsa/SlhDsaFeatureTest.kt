package com.danyastry.pqc.feature.slhdsa

import com.danyastry.pqc.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlhDsaFeatureTest {

    @Test
    fun `is listed under signatures and works on every supported version`() {
        assertThat(SlhDsaFeature.info.id).isEqualTo("slh-dsa")
        assertThat(SlhDsaFeature.info.category).isEqualTo(FeatureCategory.SIGNATURES)
        assertThat(SlhDsaFeature.info.minSdk).isEqualTo(26)
    }
}
