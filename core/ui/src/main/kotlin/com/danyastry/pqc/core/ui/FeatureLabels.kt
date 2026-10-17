package com.danyastry.pqc.core.ui

import androidx.annotation.StringRes

@get:StringRes
val FeatureCategory.titleRes: Int
    get() = when (this) {
        FeatureCategory.KEY_EXCHANGE -> R.string.category_key_exchange
        FeatureCategory.SIGNATURES -> R.string.category_signatures
        FeatureCategory.HYBRID -> R.string.category_hybrid
        FeatureCategory.COMPARISON -> R.string.category_comparison
    }

@get:StringRes
val FeatureCategory.descriptionRes: Int
    get() = when (this) {
        FeatureCategory.KEY_EXCHANGE -> R.string.category_key_exchange_description
        FeatureCategory.SIGNATURES -> R.string.category_signatures_description
        FeatureCategory.HYBRID -> R.string.category_hybrid_description
        FeatureCategory.COMPARISON -> R.string.category_comparison_description
    }
