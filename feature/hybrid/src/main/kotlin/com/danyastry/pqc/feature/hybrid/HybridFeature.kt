package com.danyastry.pqc.feature.hybrid

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.pqc.core.ui.FeatureCategory
import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.core.ui.FeatureInfo
import com.danyastry.pqc.feature.hybrid.ui.HybridRoute

object HybridFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "hybrid",
        name = "X25519 + ML-KEM-768",
        category = FeatureCategory.HYBRID,
        summary = "The hybrid key exchange browsers use in TLS today",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        HybridRoute(modifier = modifier)
    }
}
