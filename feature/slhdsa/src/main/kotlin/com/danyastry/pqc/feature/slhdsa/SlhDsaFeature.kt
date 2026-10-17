package com.danyastry.pqc.feature.slhdsa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.pqc.core.ui.FeatureCategory
import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.core.ui.FeatureInfo
import com.danyastry.pqc.feature.slhdsa.ui.SlhDsaRoute

object SlhDsaFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "slh-dsa",
        name = "SLH-DSA",
        category = FeatureCategory.SIGNATURES,
        summary = "FIPS 205 hash-based signatures, formerly SPHINCS+",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        SlhDsaRoute(modifier = modifier)
    }
}
