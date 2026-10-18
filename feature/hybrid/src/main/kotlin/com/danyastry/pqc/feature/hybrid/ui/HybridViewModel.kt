package com.danyastry.pqc.feature.hybrid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.pqc.core.pqc.AesGcm
import com.danyastry.pqc.core.pqc.Fingerprint
import com.danyastry.pqc.feature.hybrid.ClientState
import com.danyastry.pqc.feature.hybrid.HybridKeyExchange
import com.danyastry.pqc.feature.hybrid.ServerResponse
import java.security.GeneralSecurityException
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HybridViewModel(
    private val exchange: HybridKeyExchange = HybridKeyExchange(),
    private val random: SecureRandom = SecureRandom(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(HybridUiState())
    val state: StateFlow<HybridUiState> = _state.asStateFlow()

    private var client: ClientState? = null
    private var server: ServerResponse? = null
    private var sealed: ByteArray? = null

    fun onClientHello() = work {
        val hello = exchange.clientHello()
        client = hello
        server = null
        sealed = null
        _state.update {
            HybridUiState(input = it.input, attackerBreaks = it.attackerBreaks, clientShareBytes = hello.share.size)
        }
    }

    fun onServerRespond() = work {
        val hello = client ?: return@work
        val response = exchange.serverRespond(hello.share)
        server = response
        sealed = null
        _state.update {
            it.copy(
                serverShareBytes = response.share.size,
                serverSecret = Fingerprint.of(response.sharedSecret),
                clientSecret = "",
                sessionKey = "",
                sealedBytes = 0,
                attack = null,
            )
        }
    }

    fun onClientFinish() = work {
        val hello = client ?: return@work
        val response = server ?: return@work
        val result = exchange.clientFinish(hello, response.share)
        _state.update {
            it.copy(
                clientSecret = Fingerprint.of(result.sharedSecret),
                sessionKey = Fingerprint.of(HybridKeyExchange.sessionKey(result.sharedSecret)),
            )
        }
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
    }

    fun onServerSend() = work {
        val response = server ?: return@work
        val message = AesGcm.seal(HybridKeyExchange.sessionKey(response.sharedSecret), _state.value.input.encodeToByteArray())
        sealed = message
        _state.update { it.copy(sealedBytes = message.size, attack = null) }
    }

    fun onAttackerChange(breaks: AttackerBreaks) {
        _state.update { it.copy(attackerBreaks = breaks, attack = null) }
    }

    fun onAttack() = work {
        val response = server ?: return@work
        val message = sealed ?: return@work
        val breaks = _state.value.attackerBreaks
        val mlKemGuess = if (breaks != AttackerBreaks.X25519) response.mlKemSecret else randomSecret()
        val x25519Guess = if (breaks != AttackerBreaks.ML_KEM) response.x25519Secret else randomSecret()
        val result = try {
            AttackResult.Read(AesGcm.open(HybridKeyExchange.sessionKey(mlKemGuess + x25519Guess), message).decodeToString())
        } catch (_: GeneralSecurityException) {
            AttackResult.Failed
        }
        _state.update { it.copy(attack = result) }
    }

    private fun randomSecret(): ByteArray = ByteArray(HybridKeyExchange.SECRET_BYTES).also(random::nextBytes)

    private fun work(action: () -> Unit) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            withContext(dispatcher) { action() }
            _state.update { it.copy(isBusy = false) }
        }
    }
}
