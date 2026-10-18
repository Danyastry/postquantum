package com.danyastry.pqc.feature.comparison.ui

import com.danyastry.pqc.feature.comparison.KeyExchangeResult
import com.danyastry.pqc.feature.comparison.SignatureResult

data class ComparisonUiState(
    val keyExchanges: List<KeyExchangeResult> = emptyList(),
    val signatures: List<SignatureResult> = emptyList(),
    val measuring: String? = null,
    val done: Int = 0,
    val total: Int = 0,
) {
    val isRunning: Boolean
        get() = measuring != null
}
