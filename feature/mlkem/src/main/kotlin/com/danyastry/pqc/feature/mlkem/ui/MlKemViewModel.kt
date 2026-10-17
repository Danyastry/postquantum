package com.danyastry.pqc.feature.mlkem.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.pqc.core.pqc.AesGcm
import com.danyastry.pqc.core.pqc.Fingerprint
import com.danyastry.pqc.feature.mlkem.Encapsulation
import com.danyastry.pqc.feature.mlkem.MlKem
import com.danyastry.pqc.feature.mlkem.MlKemLevel
import java.security.GeneralSecurityException
import java.security.KeyPair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MlKemViewModel(
    private val mlKem: MlKem = MlKem(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(MlKemUiState())
    val state: StateFlow<MlKemUiState> = _state.asStateFlow()

    private var bobKeys: KeyPair? = null
    private var aliceEncapsulation: Encapsulation? = null
    private var ciphertext: ByteArray? = null
    private var bobSecret: ByteArray? = null
    private var sealed: ByteArray? = null

    fun onLevelChange(level: MlKemLevel) {
        clear()
        _state.update { MlKemUiState(level = level, input = it.input) }
    }

    fun onGenerateKeys() = work {
        val pair = mlKem.generateKeyPair(_state.value.level)
        val encapsulationKey = MlKem.encapsulationKeyBytes(pair.public)
        clear()
        bobKeys = pair
        _state.update {
            MlKemUiState(
                level = it.level,
                input = it.input,
                bob = BobKeys(
                    encapsulationKeyBytes = encapsulationKey.size,
                    decapsulationKeyBytes = MlKem.decapsulationKeyBytes(pair.private).size,
                    seedBytes = MlKem.seedBytes(pair.private)?.size ?: 0,
                    fingerprint = Fingerprint.of(encapsulationKey),
                ),
            )
        }
    }

    fun onEncapsulate() = work {
        val pair = bobKeys ?: return@work
        val encapsulation = mlKem.encapsulate(pair.public)
        aliceEncapsulation = encapsulation
        ciphertext = encapsulation.ciphertext
        bobSecret = null
        sealed = null
        _state.update {
            it.copy(
                alice = AliceResult(encapsulation.ciphertext.size, Fingerprint.of(encapsulation.sharedSecret)),
                bobSecretFingerprint = "",
                secretsMatch = null,
                sealedBytes = 0,
                message = null,
                readText = "",
            )
        }
    }

    fun onFlipBit() {
        val bytes = ciphertext ?: return
        bytes[0] = (bytes[0].toInt() xor 1).toByte()
        bobSecret = null
        _state.update {
            it.copy(alice = it.alice?.copy(isTampered = true), bobSecretFingerprint = "", secretsMatch = null, message = null, readText = "")
        }
    }

    fun onDecapsulate() = work {
        val pair = bobKeys ?: return@work
        val bytes = ciphertext ?: return@work
        val alice = aliceEncapsulation ?: return@work
        val secret = mlKem.decapsulate(pair.private, bytes)
        bobSecret = secret
        _state.update {
            it.copy(bobSecretFingerprint = Fingerprint.of(secret), secretsMatch = secret.contentEquals(alice.sharedSecret), message = null, readText = "")
        }
    }

    fun onInputChange(input: String) {
        _state.update { it.copy(input = input) }
    }

    fun onSend() = work {
        val alice = aliceEncapsulation ?: return@work
        val message = AesGcm.seal(alice.sharedSecret, _state.value.input.encodeToByteArray())
        sealed = message
        _state.update { it.copy(sealedBytes = message.size, message = null, readText = "") }
    }

    fun onRead() = work {
        val secret = bobSecret ?: return@work
        val message = sealed ?: return@work
        val text = try {
            AesGcm.open(secret, message).decodeToString()
        } catch (_: GeneralSecurityException) {
            null
        }
        _state.update {
            if (text == null) it.copy(message = MessageResult.UNREADABLE, readText = "") else it.copy(message = MessageResult.READ, readText = text)
        }
    }

    private fun clear() {
        bobKeys = null
        aliceEncapsulation = null
        ciphertext = null
        bobSecret = null
        sealed = null
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
