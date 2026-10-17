package com.danyastry.pqc.feature.slhdsa.ui

import com.danyastry.pqc.feature.slhdsa.HashFamily
import com.danyastry.pqc.feature.slhdsa.SlhDsaParameters
import com.danyastry.pqc.feature.slhdsa.SlhDsaStrength
import com.danyastry.pqc.feature.slhdsa.SlhDsaVariant
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
class SlhDsaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var now = 0L
    private val clock = { now.also { now += 7_000_000 } }
    private lateinit var viewModel: SlhDsaViewModel

    private val state
        get() = viewModel.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SlhDsaViewModel(clock = clock, dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with sha2 128f, the fastest set`() = test {
        assertThat(state.parameters).isEqualTo(SlhDsaParameters(SlhDsaStrength.BITS_128, SlhDsaVariant.FAST, HashFamily.SHA2))
        assertThat(state.parameters.name).isEqualTo("SLH-DSA-SHA2-128f")
        assertThat(state.canSign).isFalse()
    }

    @Test
    fun `keys are tiny and the time is measured`() = test {
        generate()

        assertThat(state.keys?.publicKeyBytes).isEqualTo(32)
        assertThat(state.keys?.privateKeyBytes).isEqualTo(64)
        assertThat(state.keys?.millis).isEqualTo(7)
    }

    @Test
    fun `a fresh signature verifies`() = test {
        generate()
        sign()

        verify()

        assertThat(state.signature?.bytes).isEqualTo(17_088)
        assertThat(state.verify?.isValid).isTrue()
        assertThat(state.verify?.millis).isEqualTo(7)
    }

    @Test
    fun `a changed message does not verify`() = test {
        generate()
        sign()

        viewModel.onMessageChange("Pay Alice 900 dollars")
        assertThat(state.verify).isNull()
        verify()

        assertThat(state.verify?.isValid).isFalse()
    }

    @Test
    fun `a flipped bit does not verify`() = test {
        generate()
        sign()

        viewModel.onFlipBit()
        verify()

        assertThat(state.signature?.isTampered).isTrue()
        assertThat(state.canTamper).isFalse()
        assertThat(state.verify?.isValid).isFalse()
    }

    @Test
    fun `the small variant gives a smaller signature`() = test {
        viewModel.onVariantChange(SlhDsaVariant.SMALL)
        generate()

        sign()

        assertThat(state.parameters.name).isEqualTo("SLH-DSA-SHA2-128s")
        assertThat(state.signature?.bytes).isEqualTo(7_856)
    }

    @Test
    fun `shake works the same way`() = test {
        viewModel.onHashChange(HashFamily.SHAKE)
        generate()
        sign()

        verify()

        assertThat(state.parameters.name).isEqualTo("SLH-DSA-SHAKE-128f")
        assertThat(state.verify?.isValid).isTrue()
    }

    @Test
    fun `changing parameters starts over but keeps the message`() = test {
        generate()
        sign()
        viewModel.onMessageChange("Hello")

        viewModel.onStrengthChange(SlhDsaStrength.BITS_192)

        assertThat(state).isEqualTo(SlhDsaUiState(parameters = SlhDsaParameters(strength = SlhDsaStrength.BITS_192), message = "Hello"))
    }

    @Test
    fun `shows what it is busy with and ignores taps meanwhile`() = test {
        viewModel.onGenerateKeys()
        assertThat(state.work).isEqualTo(Work.GENERATING)

        viewModel.onStrengthChange(SlhDsaStrength.BITS_256)
        advanceUntilIdle()

        assertThat(state.work).isNull()
        assertThat(state.parameters.strength).isEqualTo(SlhDsaStrength.BITS_128)
        assertThat(state.keys).isNotNull()
    }

    private fun TestScope.generate() {
        viewModel.onGenerateKeys()
        advanceUntilIdle()
    }

    private fun TestScope.sign() {
        viewModel.onSign()
        advanceUntilIdle()
    }

    private fun TestScope.verify() {
        viewModel.onVerify()
        advanceUntilIdle()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        advanceUntilIdle()
        block()
    }
}
