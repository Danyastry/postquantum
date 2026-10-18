package com.danyastry.pqc.feature.comparison

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.pqc.core.ui.FeatureCategory
import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.core.ui.FeatureInfo
import com.danyastry.pqc.feature.comparison.ui.ComparisonRoute

object ComparisonFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "comparison",
        name = "Post-quantum vs classical",
        category = FeatureCategory.COMPARISON,
        summary = "Sizes and speed measured on this device",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        ComparisonRoute(modifier = modifier)
    }
}
