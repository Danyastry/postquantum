package com.danyastry.pqc.feature.mldsa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.pqc.core.ui.FeatureCategory
import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.core.ui.FeatureInfo
import com.danyastry.pqc.feature.mldsa.ui.MlDsaRoute

object MlDsaFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "ml-dsa",
        name = "ML-DSA",
        category = FeatureCategory.SIGNATURES,
        summary = "FIPS 204 signatures, formerly Dilithium",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        MlDsaRoute(modifier = modifier)
    }
}
