package com.danyastry.pqc.feature.slhdsa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.pqc.core.pqc.Fingerprint
import com.danyastry.pqc.feature.slhdsa.HashFamily
import com.danyastry.pqc.feature.slhdsa.SlhDsa
import com.danyastry.pqc.feature.slhdsa.SlhDsaParameters
import com.danyastry.pqc.feature.slhdsa.SlhDsaStrength
import com.danyastry.pqc.feature.slhdsa.SlhDsaVariant
import java.security.KeyPair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlhDsaViewModel(
    private val slhDsa: SlhDsa = SlhDsa(),
    private val clock: () -> Long = System::nanoTime,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(SlhDsaUiState())
    val state: StateFlow<SlhDsaUiState> = _state.asStateFlow()

    private var keys: KeyPair? = null
    private var signature: ByteArray? = null

    fun onStrengthChange(strength: SlhDsaStrength) = changeParameters { it.copy(strength = strength) }

    fun onVariantChange(variant: SlhDsaVariant) = changeParameters { it.copy(variant = variant) }

    fun onHashChange(hash: HashFamily) = changeParameters { it.copy(hash = hash) }

    fun onMessageChange(message: String) {
        _state.update { it.copy(message = message, verify = null) }
    }

    fun onGenerateKeys() = work(Work.GENERATING) {
        val parameters = _state.value.parameters
        val (pair, millis) = timed { slhDsa.generateKeyPair(parameters) }
        val publicKey = SlhDsa.publicKeyBytes(pair.public)
        keys = pair
        signature = null
        _state.update {
            it.copy(
                keys = KeysInfo(publicKey.size, SlhDsa.privateKeyBytes(pair.private).size, Fingerprint.of(publicKey), millis),
                signature = null,
                verify = null,
            )
        }
    }

    fun onSign() = work(Work.SIGNING) {
        val pair = keys ?: return@work
        val message = _state.value.message.encodeToByteArray()
        val (signed, millis) = timed { slhDsa.sign(pair.private, message) }
        signature = signed
        _state.update { it.copy(signature = SignatureInfo(signed.size, Fingerprint.of(signed), millis), verify = null) }
    }

    fun onFlipBit() {
        val bytes = signature ?: return
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 1).toByte()
        _state.update { it.copy(signature = it.signature?.copy(isTampered = true, fingerprint = Fingerprint.of(bytes)), verify = null) }
    }

    fun onVerify() = work(Work.VERIFYING) {
        val pair = keys ?: return@work
        val signed = signature ?: return@work
        val message = _state.value.message.encodeToByteArray()
        val (valid, millis) = timed { slhDsa.verify(pair.public, message, signed) }
        _state.update { it.copy(verify = VerifyResult(valid, millis)) }
    }

    private fun changeParameters(change: (SlhDsaParameters) -> SlhDsaParameters) {
        if (_state.value.isBusy) return
        keys = null
        signature = null
        _state.update { SlhDsaUiState(parameters = change(it.parameters), message = it.message) }
    }

    private fun <T> timed(action: () -> T): Pair<T, Long> {
        val start = clock()
        val result = action()
        return result to (clock() - start) / NANOS_PER_MILLI
    }

    private fun work(kind: Work, action: () -> Unit) {
        if (_state.value.isBusy) return
        _state.update { it.copy(work = kind) }
        viewModelScope.launch {
            withContext(dispatcher) { action() }
            _state.update { it.copy(work = null) }
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
