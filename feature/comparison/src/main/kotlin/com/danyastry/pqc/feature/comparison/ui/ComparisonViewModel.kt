package com.danyastry.pqc.feature.comparison.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.pqc.feature.comparison.Benchmark
import com.danyastry.pqc.feature.comparison.KeyExchangeScheme
import com.danyastry.pqc.feature.comparison.KeyExchangeSchemes
import com.danyastry.pqc.feature.comparison.SignatureScheme
import com.danyastry.pqc.feature.comparison.SignatureSchemes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComparisonViewModel(
    private val keyExchanges: List<KeyExchangeScheme> = KeyExchangeSchemes.all,
    private val signatures: List<SignatureScheme> = SignatureSchemes.all,
    private val benchmark: Benchmark = Benchmark(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(ComparisonUiState())
    val state: StateFlow<ComparisonUiState> = _state.asStateFlow()

    fun onRun() {
        if (_state.value.isRunning) return
        val total = keyExchanges.size + signatures.size
        _state.update { ComparisonUiState(measuring = keyExchanges.firstOrNull()?.name ?: signatures.firstOrNull()?.name, total = total) }
        viewModelScope.launch {
            keyExchanges.forEach { scheme ->
                _state.update { it.copy(measuring = scheme.name) }
                val result = withContext(dispatcher) { benchmark.keyExchange(scheme) }
                _state.update { it.copy(keyExchanges = it.keyExchanges + result, done = it.done + 1) }
            }
            signatures.forEach { scheme ->
                _state.update { it.copy(measuring = scheme.name) }
                val result = withContext(dispatcher) { benchmark.signature(scheme) }
                _state.update { it.copy(signatures = it.signatures + result, done = it.done + 1) }
            }
            _state.update { it.copy(measuring = null) }
        }
    }
}
