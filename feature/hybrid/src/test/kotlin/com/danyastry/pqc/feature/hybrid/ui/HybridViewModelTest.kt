package com.danyastry.pqc.feature.hybrid.ui

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HybridViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HybridViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = HybridViewModel(dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts before the handshake`() = test {
        assertThat(state.clientShareBytes).isEqualTo(0)
        assertThat(state.canRespond).isFalse()
        assertThat(state.attackerBreaks).isEqualTo(AttackerBreaks.X25519)
    }

    @Test
    fun `the handshake has tls sizes and both sides agree`() = test {
        handshake()

        assertThat(state.clientShareBytes).isEqualTo(1_216)
        assertThat(state.serverShareBytes).isEqualTo(1_120)
        assertThat(state.secretsMatch).isTrue()
        assertThat(state.sessionKey).isNotEmpty()
    }

    @Test
    fun `breaking only x25519 does not reveal the message`() = test {
        handshakeAndSend("Attack at dawn")

        attack(AttackerBreaks.X25519)

        assertThat(state.attack).isEqualTo(AttackResult.Failed)
    }

    @Test
    fun `breaking only ml-kem does not reveal the message`() = test {
        handshakeAndSend("Attack at dawn")

        attack(AttackerBreaks.ML_KEM)

        assertThat(state.attack).isEqualTo(AttackResult.Failed)
    }

    @Test
    fun `breaking both reveals the message`() = test {
        handshakeAndSend("Attack at dawn")

        attack(AttackerBreaks.BOTH)

        assertThat(state.attack).isEqualTo(AttackResult.Read("Attack at dawn"))
    }

    @Test
    fun `the message is sealed with aes-256-gcm`() = test {
        handshakeAndSend("Attack at dawn")

        assertThat(state.sealedBytes).isEqualTo(12 + 14 + 16)
        assertThat(state.canAttack).isTrue()
    }

    @Test
    fun `changing what the attacker breaks clears the old result`() = test {
        handshakeAndSend("Attack at dawn")
        attack(AttackerBreaks.BOTH)

        viewModel.onAttackerChange(AttackerBreaks.ML_KEM)

        assertThat(state.attack).isNull()
    }

    @Test
    fun `a new client hello starts over but keeps the message and the attacker choice`() = test {
        handshakeAndSend("Hello")
        viewModel.onAttackerChange(AttackerBreaks.BOTH)

        viewModel.onClientHello()
        advanceUntilIdle()

        assertThat(state).isEqualTo(HybridUiState(clientShareBytes = 1_216, input = "Hello", attackerBreaks = AttackerBreaks.BOTH))
    }

    private fun TestScope.handshake() {
        viewModel.onClientHello()
        advanceUntilIdle()
        viewModel.onServerRespond()
        advanceUntilIdle()
        viewModel.onClientFinish()
        advanceUntilIdle()
    }

    private fun TestScope.handshakeAndSend(text: String) {
        handshake()
        viewModel.onInputChange(text)
        viewModel.onServerSend()
        advanceUntilIdle()
    }

    private fun TestScope.attack(breaks: AttackerBreaks) {
        viewModel.onAttackerChange(breaks)
        viewModel.onAttack()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }
}
