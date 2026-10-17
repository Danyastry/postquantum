package com.danyastry.pqc.feature.mlkem.ui

import com.danyastry.pqc.feature.mlkem.MlKemLevel
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
class MlKemViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MlKemViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = MlKemViewModel(dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts at ml-kem-768 with nothing done`() = test {
        assertThat(state.level).isEqualTo(MlKemLevel.ML_KEM_768)
        assertThat(state.bob).isNull()
        assertThat(state.canEncapsulate).isFalse()
    }

    @Test
    fun `bob's keys have the sizes of the chosen level`() = test {
        generate()

        assertThat(state.bob?.encapsulationKeyBytes).isEqualTo(1_184)
        assertThat(state.bob?.decapsulationKeyBytes).isEqualTo(2_400)
        assertThat(state.bob?.seedBytes).isEqualTo(64)
        assertThat(state.canEncapsulate).isTrue()
    }

    @Test
    fun `alice and bob agree on the same secret`() = test {
        generate()
        encapsulate()

        decapsulate()

        assertThat(state.alice?.ciphertextBytes).isEqualTo(1_088)
        assertThat(state.secretsMatch).isTrue()
        assertThat(state.bobSecretFingerprint).isEqualTo(state.alice?.secretFingerprint)
    }

    @Test
    fun `a flipped bit gives bob a different secret without an error`() = test {
        generate()
        encapsulate()

        viewModel.onFlipBit()
        decapsulate()

        assertThat(state.alice?.isTampered).isTrue()
        assertThat(state.canTamper).isFalse()
        assertThat(state.secretsMatch).isFalse()
        assertThat(state.bobSecretFingerprint).isNotEmpty()
    }

    @Test
    fun `bob reads the message alice encrypted with the shared secret`() = test {
        generate()
        encapsulate()
        decapsulate()
        viewModel.onInputChange("Attack at dawn")

        send()
        read()

        assertThat(state.sealedBytes).isEqualTo(12 + 14 + 16)
        assertThat(state.message).isEqualTo(MessageResult.READ)
        assertThat(state.readText).isEqualTo("Attack at dawn")
    }

    @Test
    fun `bob cannot read the message after a flipped bit`() = test {
        generate()
        encapsulate()
        viewModel.onFlipBit()
        decapsulate()
        viewModel.onInputChange("Attack at dawn")

        send()
        read()

        assertThat(state.message).isEqualTo(MessageResult.UNREADABLE)
        assertThat(state.readText).isEmpty()
    }

    @Test
    fun `each level gives its own sizes`() = test {
        mapOf(MlKemLevel.ML_KEM_512 to 768, MlKemLevel.ML_KEM_1024 to 1_568).forEach { (level, ciphertextBytes) ->
            viewModel.onLevelChange(level)
            generate()
            encapsulate()

            assertThat(state.alice?.ciphertextBytes).isEqualTo(ciphertextBytes)
        }
    }

    @Test
    fun `changing the level starts over but keeps the message text`() = test {
        generate()
        encapsulate()
        viewModel.onInputChange("Hello")

        viewModel.onLevelChange(MlKemLevel.ML_KEM_1024)

        assertThat(state).isEqualTo(MlKemUiState(level = MlKemLevel.ML_KEM_1024, input = "Hello"))
    }

    @Test
    fun `a new key pair clears the old exchange`() = test {
        generate()
        encapsulate()
        decapsulate()

        generate()

        assertThat(state.alice).isNull()
        assertThat(state.secretsMatch).isNull()
        assertThat(state.canDecapsulate).isFalse()
    }

    private fun TestScope.generate() {
        viewModel.onGenerateKeys()
        advanceUntilIdle()
    }

    private fun TestScope.encapsulate() {
        viewModel.onEncapsulate()
        advanceUntilIdle()
    }

    private fun TestScope.decapsulate() {
        viewModel.onDecapsulate()
        advanceUntilIdle()
    }

    private fun TestScope.send() {
        viewModel.onSend()
        advanceUntilIdle()
    }

    private fun TestScope.read() {
        viewModel.onRead()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }
}
