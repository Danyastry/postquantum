package com.danyastry.pqc.catalog

import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.feature.mlkem.MlKemFeature

object FeatureRegistry {
    val features: List<FeatureEntry> = listOf(
        MlKemFeature,
    )
}
