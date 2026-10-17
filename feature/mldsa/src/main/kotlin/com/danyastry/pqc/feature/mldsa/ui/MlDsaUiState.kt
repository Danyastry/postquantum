package com.danyastry.pqc.feature.mldsa.ui

import com.danyastry.pqc.feature.mldsa.MlDsaLevel
import com.danyastry.pqc.feature.mldsa.SigningMode

data class KeysInfo(val publicKeyBytes: Int, val privateKeyBytes: Int, val seedBytes: Int, val fingerprint: String)

data class SignatureInfo(
    val bytes: Int,
    val fingerprint: String,
    val mode: SigningMode,
    val sameAsPrevious: Boolean?,
    val isTampered: Boolean = false,
)

data class MlDsaUiState(
    val level: MlDsaLevel = MlDsaLevel.ML_DSA_65,
    val keys: KeysInfo? = null,
    val mode: SigningMode = SigningMode.HEDGED,
    val message: String = "Pay Alice 100 dollars",
    val context: String = "payments",
    val signature: SignatureInfo? = null,
    val checkMessage: String = "",
    val checkContext: String = "",
    val isValid: Boolean? = null,
    val isBusy: Boolean = false,
) {
    val canSign: Boolean
        get() = keys != null && !isBusy

    val canTamper: Boolean
        get() = signature != null && !signature.isTampered && !isBusy

    val isContextTooLong: Boolean
        get() = context.encodeToByteArray().size > MAX_CONTEXT_BYTES || checkContext.encodeToByteArray().size > MAX_CONTEXT_BYTES

    private companion object {
        const val MAX_CONTEXT_BYTES = 255
    }
}
