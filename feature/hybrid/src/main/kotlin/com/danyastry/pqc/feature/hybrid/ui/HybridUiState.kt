package com.danyastry.pqc.feature.hybrid.ui

enum class AttackerBreaks { X25519, ML_KEM, BOTH }

sealed interface AttackResult {
    data class Read(val text: String) : AttackResult
    data object Failed : AttackResult
}

data class HybridUiState(
    val clientShareBytes: Int = 0,
    val serverShareBytes: Int = 0,
    val serverSecret: String = "",
    val clientSecret: String = "",
    val sessionKey: String = "",
    val input: String = "Attack at dawn",
    val sealedBytes: Int = 0,
    val attackerBreaks: AttackerBreaks = AttackerBreaks.X25519,
    val attack: AttackResult? = null,
    val isBusy: Boolean = false,
) {
    val canRespond: Boolean
        get() = clientShareBytes > 0 && !isBusy

    val canFinish: Boolean
        get() = serverShareBytes > 0 && !isBusy

    val secretsMatch: Boolean?
        get() = if (clientSecret.isEmpty()) null else clientSecret == serverSecret

    val canSend: Boolean
        get() = sessionKey.isNotEmpty() && input.isNotEmpty() && !isBusy

    val canAttack: Boolean
        get() = sealedBytes > 0 && !isBusy
}
