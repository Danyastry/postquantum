package com.danyastry.pqc.feature.mldsa

import com.danyastry.pqc.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MlDsaFeatureTest {

    @Test
    fun `is listed under signatures and works on every supported version`() {
        assertThat(MlDsaFeature.info.id).isEqualTo("ml-dsa")
        assertThat(MlDsaFeature.info.category).isEqualTo(FeatureCategory.SIGNATURES)
        assertThat(MlDsaFeature.info.minSdk).isEqualTo(26)
    }
}
