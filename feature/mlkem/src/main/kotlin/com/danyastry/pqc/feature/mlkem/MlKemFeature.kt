package com.danyastry.pqc.feature.mlkem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.pqc.core.ui.FeatureCategory
import com.danyastry.pqc.core.ui.FeatureEntry
import com.danyastry.pqc.core.ui.FeatureInfo
import com.danyastry.pqc.feature.mlkem.ui.MlKemRoute

object MlKemFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "ml-kem",
        name = "ML-KEM",
        category = FeatureCategory.KEY_EXCHANGE,
        summary = "FIPS 203 key exchange, formerly Kyber",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        MlKemRoute(modifier = modifier)
    }
}
