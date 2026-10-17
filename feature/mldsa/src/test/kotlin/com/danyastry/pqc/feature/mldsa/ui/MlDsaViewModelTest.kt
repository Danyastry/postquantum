package com.danyastry.pqc.feature.mldsa.ui

import com.danyastry.pqc.feature.mldsa.MlDsaLevel
import com.danyastry.pqc.feature.mldsa.SigningMode
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
class MlDsaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MlDsaViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = MlDsaViewModel(dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts at ml-dsa-65 in hedged mode with a sample message`() = test {
        assertThat(state.level).isEqualTo(MlDsaLevel.ML_DSA_65)
        assertThat(state.mode).isEqualTo(SigningMode.HEDGED)
        assertThat(state.message).isEqualTo("Pay Alice 100 dollars")
        assertThat(state.context).isEqualTo("payments")
        assertThat(state.canSign).isFalse()
    }

    @Test
    fun `keys have the sizes of the chosen level`() = test {
        generate()

        assertThat(state.keys?.publicKeyBytes).isEqualTo(1_952)
        assertThat(state.keys?.privateKeyBytes).isEqualTo(4_032)
        assertThat(state.keys?.seedBytes).isEqualTo(32)
        assertThat(state.canSign).isTrue()
    }

    @Test
    fun `a fresh signature is valid`() = test {
        generate()

        sign()

        assertThat(state.signature?.bytes).isEqualTo(3_309)
        assertThat(state.signature?.sameAsPrevious).isNull()
        assertThat(state.checkMessage).isEqualTo("Pay Alice 100 dollars")
        assertThat(state.isValid).isTrue()
    }

    @Test
    fun `changing one letter of the message makes it invalid`() = test {
        generate()
        sign()

        viewModel.onCheckMessageChange("Pay Alice 900 dollars")
        advanceUntilIdle()

        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `changing the context makes it invalid`() = test {
        generate()
        sign()

        viewModel.onCheckContextChange("login")
        advanceUntilIdle()

        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `putting the message back makes it valid again`() = test {
        generate()
        sign()
        viewModel.onCheckMessageChange("Pay Alice 900 dollars")
        advanceUntilIdle()

        viewModel.onCheckMessageChange("Pay Alice 100 dollars")
        advanceUntilIdle()

        assertThat(state.isValid).isTrue()
    }

    @Test
    fun `a flipped bit in the signature makes it invalid`() = test {
        generate()
        sign()

        viewModel.onFlipSignatureBit()
        advanceUntilIdle()

        assertThat(state.signature?.isTampered).isTrue()
        assertThat(state.canTamper).isFalse()
        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `hedged signatures differ from the previous one`() = test {
        generate()
        sign()

        sign()

        assertThat(state.signature?.sameAsPrevious).isFalse()
    }

    @Test
    fun `deterministic signatures repeat`() = test {
        generate()
        viewModel.onModeChange(SigningMode.DETERMINISTIC)
        sign()

        sign()

        assertThat(state.signature?.mode).isEqualTo(SigningMode.DETERMINISTIC)
        assertThat(state.signature?.sameAsPrevious).isTrue()
        assertThat(state.isValid).isTrue()
    }

    @Test
    fun `a context over 255 bytes blocks signing`() = test {
        generate()

        viewModel.onContextChange("x".repeat(256))

        assertThat(state.isContextTooLong).isTrue()
        sign()
        assertThat(state.signature).isNull()
    }

    @Test
    fun `changing the level starts over but keeps the inputs`() = test {
        generate()
        sign()
        viewModel.onMessageChange("Hello")

        viewModel.onLevelChange(MlDsaLevel.ML_DSA_87)
        generate()
        sign()

        assertThat(state.message).isEqualTo("Hello")
        assertThat(state.keys?.publicKeyBytes).isEqualTo(2_592)
        assertThat(state.signature?.bytes).isEqualTo(4_627)
        assertThat(state.signature?.sameAsPrevious).isNull()
    }

    private fun TestScope.generate() {
        viewModel.onGenerateKeys()
        advanceUntilIdle()
    }

    private fun TestScope.sign() {
        viewModel.onSign()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }
}
