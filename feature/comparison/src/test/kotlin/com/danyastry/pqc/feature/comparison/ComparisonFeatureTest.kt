package com.danyastry.pqc.feature.comparison

import com.danyastry.pqc.core.ui.FeatureCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ComparisonFeatureTest {

    @Test
    fun `is listed under comparison and works on every supported version`() {
        assertThat(ComparisonFeature.info.id).isEqualTo("comparison")
        assertThat(ComparisonFeature.info.category).isEqualTo(FeatureCategory.COMPARISON)
        assertThat(ComparisonFeature.info.minSdk).isEqualTo(26)
    }
}
