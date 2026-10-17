package com.danyastry.pqc.feature.mldsa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.pqc.core.pqc.Fingerprint
import com.danyastry.pqc.feature.mldsa.MlDsa
import com.danyastry.pqc.feature.mldsa.MlDsaLevel
import com.danyastry.pqc.feature.mldsa.SigningMode
import java.security.KeyPair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MlDsaViewModel(
    private val mlDsa: MlDsa = MlDsa(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(MlDsaUiState())
    val state: StateFlow<MlDsaUiState> = _state.asStateFlow()

    private var keys: KeyPair? = null
    private var signature: ByteArray? = null
    private var verifyJob: Job? = null

    fun onLevelChange(level: MlDsaLevel) {
        keys = null
        signature = null
        _state.update { MlDsaUiState(level = level, mode = it.mode, message = it.message, context = it.context) }
    }

    fun onModeChange(mode: SigningMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun onMessageChange(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun onContextChange(context: String) {
        _state.update { it.copy(context = context) }
    }

    fun onGenerateKeys() = work {
        val pair = mlDsa.generateKeyPair(_state.value.level)
        val publicKey = MlDsa.publicKeyBytes(pair.public)
        keys = pair
        signature = null
        _state.update {
            MlDsaUiState(
                level = it.level,
                mode = it.mode,
                message = it.message,
                context = it.context,
                keys = KeysInfo(publicKey.size, MlDsa.privateKeyBytes(pair.private).size, MlDsa.seedBytes(pair.private)?.size ?: 0, Fingerprint.of(publicKey)),
            )
        }
    }

    fun onSign() = work {
        val pair = keys ?: return@work
        val snapshot = _state.value
        if (snapshot.isContextTooLong) return@work
        val previous = signature
        val signed = mlDsa.sign(pair.private, snapshot.message.encodeToByteArray(), snapshot.context.encodeToByteArray(), snapshot.mode)
        signature = signed
        val sameAsPrevious = previous?.let { signed.contentEquals(it) }
        val checked = snapshot.copy(checkMessage = snapshot.message, checkContext = snapshot.context)
        _state.update {
            it.copy(
                signature = SignatureInfo(signed.size, Fingerprint.of(signed), snapshot.mode, sameAsPrevious),
                checkMessage = snapshot.message,
                checkContext = snapshot.context,
                isValid = verify(checked),
            )
        }
    }

    fun onCheckMessageChange(message: String) {
        _state.update { it.copy(checkMessage = message) }
        verifyInBackground()
    }

    fun onCheckContextChange(context: String) {
        _state.update { it.copy(checkContext = context) }
        verifyInBackground()
    }

    fun onFlipSignatureBit() {
        val bytes = signature ?: return
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 1).toByte()
        _state.update { it.copy(signature = it.signature?.copy(isTampered = true, fingerprint = Fingerprint.of(bytes))) }
        verifyInBackground()
    }

    private fun verifyInBackground() {
        verifyJob?.cancel()
        val snapshot = _state.value
        verifyJob = viewModelScope.launch {
            val valid = withContext(dispatcher) { verify(snapshot) }
            _state.update { it.copy(isValid = valid) }
        }
    }

    private fun verify(snapshot: MlDsaUiState): Boolean? {
        val pair = keys ?: return null
        val signed = signature ?: return null
        if (snapshot.isContextTooLong) return null
        return mlDsa.verify(pair.public, snapshot.checkMessage.encodeToByteArray(), signed, snapshot.checkContext.encodeToByteArray())
    }

    private fun work(action: () -> Unit) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            withContext(dispatcher) { action() }
            _state.update { it.copy(isBusy = false) }
        }
    }
}
