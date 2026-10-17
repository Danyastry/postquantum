package com.danyastry.pqc.feature.mlkem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.pqc.core.ui.components.CipherTextField
import com.danyastry.pqc.core.ui.components.SegmentedSelector
import com.danyastry.pqc.core.ui.theme.PqcTheme
import com.danyastry.pqc.feature.mlkem.MlKemLevel
import com.danyastry.pqc.feature.mlkem.R
import java.util.Locale

private const val X25519_BYTES = 32

@Composable
fun MlKemRoute(
    modifier: Modifier = Modifier,
    viewModel: MlKemViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MlKemScreen(
        state = state,
        onLevelChange = viewModel::onLevelChange,
        onGenerateKeys = viewModel::onGenerateKeys,
        onEncapsulate = viewModel::onEncapsulate,
        onFlipBit = viewModel::onFlipBit,
        onDecapsulate = viewModel::onDecapsulate,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onRead = viewModel::onRead,
        modifier = modifier,
    )
}

@Composable
fun MlKemScreen(
    state: MlKemUiState,
    onLevelChange: (MlKemLevel) -> Unit,
    onGenerateKeys: () -> Unit,
    onEncapsulate: () -> Unit,
    onFlipBit: () -> Unit,
    onDecapsulate: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.mlkem_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedSelector(
            options = MlKemLevel.entries,
            selected = state.level,
            label = { stringResource(it.labelRes) },
            onSelect = onLevelChange,
        )
        Text(
            text = stringResource(R.string.mlkem_level_hint, state.level.nistCategory, state.level.comparableAesBits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepCard(stringResource(R.string.mlkem_step_keys)) {
            Button(onClick = onGenerateKeys, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.mlkem_generate))
            }
            state.bob?.let { bob ->
                MonoText(pluralStringResource(R.plurals.mlkem_encapsulation_key, bob.encapsulationKeyBytes, bob.encapsulationKeyBytes))
                MonoText(pluralStringResource(R.plurals.mlkem_decapsulation_key, bob.decapsulationKeyBytes, bob.decapsulationKeyBytes, bob.seedBytes))
                MonoText(stringResource(R.string.mlkem_fingerprint, bob.fingerprint))
            }
        }
        StepCard(stringResource(R.string.mlkem_step_encapsulate), stringResource(R.string.mlkem_encapsulate_hint)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEncapsulate, enabled = state.canEncapsulate) {
                    Text(stringResource(R.string.mlkem_encapsulate))
                }
                OutlinedButton(onClick = onFlipBit, enabled = state.canTamper) {
                    Text(stringResource(R.string.mlkem_flip_bit))
                }
            }
            state.alice?.let { alice ->
                MonoText(pluralStringResource(R.plurals.mlkem_ciphertext, alice.ciphertextBytes, alice.ciphertextBytes))
                MonoText(stringResource(R.string.mlkem_alice_secret, alice.secretFingerprint))
                if (alice.isTampered) {
                    Text(
                        text = stringResource(R.string.mlkem_tampered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        StepCard(stringResource(R.string.mlkem_step_decapsulate)) {
            Button(onClick = onDecapsulate, enabled = state.canDecapsulate, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.mlkem_decapsulate))
            }
            if (state.bobSecretFingerprint.isNotEmpty()) {
                MonoText(stringResource(R.string.mlkem_bob_secret, state.bobSecretFingerprint))
            }
            state.secretsMatch?.let { match ->
                Text(
                    text = stringResource(if (match) R.string.mlkem_match else R.string.mlkem_mismatch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (match) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
        StepCard(stringResource(R.string.mlkem_step_message)) {
            CipherTextField(
                value = state.input,
                onValueChange = onInputChange,
                label = stringResource(R.string.mlkem_input_label),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSend, enabled = state.canSend) {
                    Text(stringResource(R.string.mlkem_send))
                }
                OutlinedButton(onClick = onRead, enabled = state.canRead) {
                    Text(stringResource(R.string.mlkem_read))
                }
            }
            if (state.sealedBytes > 0) {
                MonoText(pluralStringResource(R.plurals.mlkem_sealed, state.sealedBytes, state.sealedBytes))
            }
            when (state.message) {
                MessageResult.READ -> Text(
                    text = stringResource(R.string.mlkem_read_text, state.readText),
                    style = MaterialTheme.typography.bodyLarge,
                )
                MessageResult.UNREADABLE -> Text(
                    text = stringResource(R.string.mlkem_unreadable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                null -> Unit
            }
        }
        SizesCard(state.level)
    }
}

@Composable
private fun SizesCard(level: MlKemLevel) {
    StepCard(stringResource(R.string.mlkem_sizes_title), stringResource(R.string.mlkem_sizes_note)) {
        MonoText(stringResource(R.string.mlkem_sizes_public_key, level.encapsulationKeyBytes, ratio(level.encapsulationKeyBytes)))
        MonoText(stringResource(R.string.mlkem_sizes_ciphertext, level.ciphertextBytes, ratio(level.ciphertextBytes)))
    }
}

@Composable
private fun StepCard(title: String, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun MonoText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
}

private fun ratio(bytes: Int): String = String.format(Locale.US, "%.0f", bytes.toDouble() / X25519_BYTES)

private val MlKemLevel.labelRes: Int
    get() = when (this) {
        MlKemLevel.ML_KEM_512 -> R.string.mlkem_level_512
        MlKemLevel.ML_KEM_768 -> R.string.mlkem_level_768
        MlKemLevel.ML_KEM_1024 -> R.string.mlkem_level_1024
    }

@Preview(showBackground = true)
@Composable
private fun MlKemScreenPreview() {
    PqcTheme {
        MlKemScreen(
            state = MlKemUiState(
                bob = BobKeys(encapsulationKeyBytes = 1_184, decapsulationKeyBytes = 2_400, seedBytes = 64, fingerprint = "3a:91:0c:5e:77:12:d4:8b"),
                alice = AliceResult(ciphertextBytes = 1_088, secretFingerprint = "5b:e1:02:7c:aa:93:4f:10"),
                bobSecretFingerprint = "5b:e1:02:7c:aa:93:4f:10",
                secretsMatch = true,
                input = "Attack at dawn",
            ),
            onLevelChange = {},
            onGenerateKeys = {},
            onEncapsulate = {},
            onFlipBit = {},
            onDecapsulate = {},
            onInputChange = {},
            onSend = {},
            onRead = {},
        )
    }
}
