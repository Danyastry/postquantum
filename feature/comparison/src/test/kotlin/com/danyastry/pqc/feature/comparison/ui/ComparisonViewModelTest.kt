package com.danyastry.pqc.feature.comparison.ui

import com.danyastry.pqc.feature.comparison.Benchmark
import com.danyastry.pqc.feature.comparison.KeyExchangeSchemes
import com.danyastry.pqc.feature.comparison.SignatureSchemes
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val keyExchanges = KeyExchangeSchemes.all.filter { it.name in setOf("X25519", "ML-KEM-768") }
    private val signatures = SignatureSchemes.all.filter { it.name in setOf("Ed25519", "ML-DSA-65") }
    private lateinit var viewModel: ComparisonViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ComparisonViewModel(keyExchanges, signatures, Benchmark(runs = 1, warmups = 0), dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with no results`() = test {
        assertThat(state.keyExchanges).isEmpty()
        assertThat(state.signatures).isEmpty()
        assertThat(state.isRunning).isFalse()
    }

    @Test
    fun `measures every scheme in order`() = test {
        viewModel.onRun()
        advanceUntilIdle()

        assertThat(state.keyExchanges.map { it.name }).containsExactly("X25519", "ML-KEM-768").inOrder()
        assertThat(state.signatures.map { it.name }).containsExactly("Ed25519", "ML-DSA-65").inOrder()
        assertThat(state.done).isEqualTo(4)
        assertThat(state.isRunning).isFalse()
    }

    @Test
    fun `shows which scheme it is measuring`() = test {
        viewModel.onRun()

        assertThat(state.measuring).isEqualTo("X25519")
        assertThat(state.total).isEqualTo(4)
        runCurrent()
        advanceUntilIdle()
        assertThat(state.measuring).isNull()
    }

    @Test
    fun `results carry real sizes and the quantum-safe flag`() = test {
        viewModel.onRun()
        advanceUntilIdle()

        val mlKem = state.keyExchanges.single { it.name == "ML-KEM-768" }
        val ed25519 = state.signatures.single { it.name == "Ed25519" }
        assertThat(mlKem.publicKeyBytes).isEqualTo(1_184)
        assertThat(mlKem.isQuantumSafe).isTrue()
        assertThat(ed25519.signatureBytes).isEqualTo(64)
        assertThat(ed25519.isQuantumSafe).isFalse()
    }

    @Test
    fun `running again replaces the old results`() = test {
        viewModel.onRun()
        advanceUntilIdle()

        viewModel.onRun()
        advanceUntilIdle()

        assertThat(state.keyExchanges).hasSize(2)
        assertThat(state.signatures).hasSize(2)
    }

    @Test
    fun `a second tap while running is ignored`() = test {
        viewModel.onRun()
        viewModel.onRun()
        advanceUntilIdle()

        assertThat(state.keyExchanges).hasSize(2)
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }
}
