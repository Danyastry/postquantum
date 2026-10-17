package com.danyastry.pqc.feature.mlkem.ui

import com.danyastry.pqc.feature.mlkem.MlKemLevel

data class BobKeys(val encapsulationKeyBytes: Int, val decapsulationKeyBytes: Int, val seedBytes: Int, val fingerprint: String)

data class AliceResult(val ciphertextBytes: Int, val secretFingerprint: String, val isTampered: Boolean = false)

enum class MessageResult { READ, UNREADABLE }

data class MlKemUiState(
    val level: MlKemLevel = MlKemLevel.ML_KEM_768,
    val bob: BobKeys? = null,
    val alice: AliceResult? = null,
    val bobSecretFingerprint: String = "",
    val secretsMatch: Boolean? = null,
    val input: String = "",
    val sealedBytes: Int = 0,
    val message: MessageResult? = null,
    val readText: String = "",
    val isBusy: Boolean = false,
) {
    val canEncapsulate: Boolean
        get() = bob != null && !isBusy

    val canTamper: Boolean
        get() = alice != null && !alice.isTampered && !isBusy

    val canDecapsulate: Boolean
        get() = alice != null && !isBusy

    val canSend: Boolean
        get() = alice != null && input.isNotEmpty() && !isBusy

    val canRead: Boolean
        get() = sealedBytes > 0 && bobSecretFingerprint.isNotEmpty() && !isBusy
}
