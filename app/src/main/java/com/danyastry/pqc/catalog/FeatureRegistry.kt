package com.danyastry.pqc.catalog

import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.feature.hybrid.HybridFeature
import com.danyastry.pqc.feature.mldsa.MlDsaFeature
import com.danyastry.pqc.feature.mlkem.MlKemFeature
import com.danyastry.pqc.feature.slhdsa.SlhDsaFeature

object FeatureRegistry {
    val features: List<FeatureEntry> = listOf(
        MlKemFeature,
        MlDsaFeature,
        SlhDsaFeature,
        HybridFeature,
    )
}
