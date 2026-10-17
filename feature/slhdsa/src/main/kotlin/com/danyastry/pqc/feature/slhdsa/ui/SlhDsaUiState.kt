package com.danyastry.pqc.feature.slhdsa.ui

import com.danyastry.pqc.feature.slhdsa.SlhDsaParameters

enum class Work { GENERATING, SIGNING, VERIFYING }

data class KeysInfo(val publicKeyBytes: Int, val privateKeyBytes: Int, val fingerprint: String, val millis: Long)

data class SignatureInfo(val bytes: Int, val fingerprint: String, val millis: Long, val isTampered: Boolean = false)

data class VerifyResult(val isValid: Boolean, val millis: Long)

data class SlhDsaUiState(
    val parameters: SlhDsaParameters = SlhDsaParameters(),
    val message: String = "Pay Alice 100 dollars",
    val keys: KeysInfo? = null,
    val signature: SignatureInfo? = null,
    val verify: VerifyResult? = null,
    val work: Work? = null,
) {
    val isBusy: Boolean
        get() = work != null

    val canSign: Boolean
        get() = keys != null && !isBusy

    val canVerify: Boolean
        get() = signature != null && !isBusy

    val canTamper: Boolean
        get() = signature != null && !signature.isTampered && !isBusy
}
