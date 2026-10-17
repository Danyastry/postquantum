package com.danyastry.pqc.feature.mlkem

import com.danyastry.pqc.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MlKemFeatureTest {

    @Test
    fun `is listed under key exchange and works on every supported version`() {
        assertThat(MlKemFeature.info.id).isEqualTo("ml-kem")
        assertThat(MlKemFeature.info.category).isEqualTo(FeatureCategory.KEY_EXCHANGE)
        assertThat(MlKemFeature.info.minSdk).isEqualTo(26)
    }
}
