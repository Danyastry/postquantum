package com.danyastry.pqc.feature.mldsa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.danyastry.pqc.core.ui.components.SegmentedSelector
import com.danyastry.pqc.core.ui.theme.PqcTheme
import com.danyastry.pqc.feature.mldsa.MlDsaLevel
import com.danyastry.pqc.feature.mldsa.R
import com.danyastry.pqc.feature.mldsa.SigningMode
import java.util.Locale

private const val ED25519_SIGNATURE_BYTES = 64
private const val ED25519_PUBLIC_KEY_BYTES = 32

@Composable
fun MlDsaRoute(
    modifier: Modifier = Modifier,
    viewModel: MlDsaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MlDsaScreen(
        state = state,
        onLevelChange = viewModel::onLevelChange,
        onGenerateKeys = viewModel::onGenerateKeys,
        onMessageChange = viewModel::onMessageChange,
        onContextChange = viewModel::onContextChange,
        onModeChange = viewModel::onModeChange,
        onSign = viewModel::onSign,
        onFlipBit = viewModel::onFlipSignatureBit,
        onCheckMessageChange = viewModel::onCheckMessageChange,
        onCheckContextChange = viewModel::onCheckContextChange,
        modifier = modifier,
    )
}

@Composable
fun MlDsaScreen(
    state: MlDsaUiState,
    onLevelChange: (MlDsaLevel) -> Unit,
    onGenerateKeys: () -> Unit,
    onMessageChange: (String) -> Unit,
    onContextChange: (String) -> Unit,
    onModeChange: (SigningMode) -> Unit,
    onSign: () -> Unit,
    onFlipBit: () -> Unit,
    onCheckMessageChange: (String) -> Unit,
    onCheckContextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.mldsa_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedSelector(
            options = MlDsaLevel.entries,
            selected = state.level,
            label = { stringResource(it.labelRes) },
            onSelect = onLevelChange,
        )
        Text(
            text = stringResource(R.string.mldsa_level_hint, state.level.nistCategory),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepCard(stringResource(R.string.mldsa_step_keys)) {
            Button(onClick = onGenerateKeys, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.mldsa_generate))
            }
            state.keys?.let { keys ->
                MonoText(pluralStringResource(R.plurals.mldsa_public_key, keys.publicKeyBytes, keys.publicKeyBytes))
                MonoText(pluralStringResource(R.plurals.mldsa_private_key, keys.privateKeyBytes, keys.privateKeyBytes, keys.seedBytes))
                MonoText(stringResource(R.string.mldsa_fingerprint, keys.fingerprint))
            }
        }
        SignCard(state = state, onMessageChange = onMessageChange, onContextChange = onContextChange, onModeChange = onModeChange, onSign = onSign, onFlipBit = onFlipBit)
        if (state.signature != null) {
            VerifyCard(state = state, onCheckMessageChange = onCheckMessageChange, onCheckContextChange = onCheckContextChange)
        }
        SizesCard(state.level)
    }
}

@Composable
private fun SignCard(
    state: MlDsaUiState,
    onMessageChange: (String) -> Unit,
    onContextChange: (String) -> Unit,
    onModeChange: (SigningMode) -> Unit,
    onSign: () -> Unit,
    onFlipBit: () -> Unit,
) {
    StepCard(stringResource(R.string.mldsa_step_sign)) {
        TextInput(value = state.message, onValueChange = onMessageChange, label = stringResource(R.string.mldsa_message_label))
        TextInput(
            value = state.context,
            onValueChange = onContextChange,
            label = stringResource(R.string.mldsa_context_label),
            supportingText = stringResource(if (state.isContextTooLong) R.string.mldsa_context_too_long else R.string.mldsa_context_hint),
            isError = state.isContextTooLong,
        )
        SegmentedSelector(
            options = SigningMode.entries,
            selected = state.mode,
            label = { stringResource(it.labelRes) },
            onSelect = onModeChange,
        )
        Text(
            text = stringResource(state.mode.hintRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onSign, enabled = state.canSign && !state.isContextTooLong, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mldsa_sign))
        }
        state.signature?.let { signature ->
            MonoText(pluralStringResource(R.plurals.mldsa_signature, signature.bytes, signature.bytes))
            MonoText(stringResource(R.string.mldsa_signature_fingerprint, signature.fingerprint))
            signature.sameAsPrevious?.let { same ->
                Text(
                    text = stringResource(if (same) R.string.mldsa_same_as_previous else R.string.mldsa_different_from_previous),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (signature.isTampered) {
                Text(
                    text = stringResource(R.string.mldsa_tampered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = onFlipBit, enabled = state.canTamper, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.mldsa_flip_bit))
            }
        }
    }
}

@Composable
private fun VerifyCard(state: MlDsaUiState, onCheckMessageChange: (String) -> Unit, onCheckContextChange: (String) -> Unit) {
    StepCard(stringResource(R.string.mldsa_step_verify), stringResource(R.string.mldsa_verify_hint)) {
        TextInput(value = state.checkMessage, onValueChange = onCheckMessageChange, label = stringResource(R.string.mldsa_check_message_label))
        TextInput(value = state.checkContext, onValueChange = onCheckContextChange, label = stringResource(R.string.mldsa_check_context_label))
        state.isValid?.let { valid ->
            Text(
                text = stringResource(if (valid) R.string.mldsa_valid else R.string.mldsa_invalid),
                style = MaterialTheme.typography.titleSmall,
                color = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SizesCard(level: MlDsaLevel) {
    StepCard(stringResource(R.string.mldsa_sizes_title), stringResource(R.string.mldsa_sizes_note)) {
        MonoText(stringResource(R.string.mldsa_sizes_signature, level.signatureBytes, ratio(level.signatureBytes, ED25519_SIGNATURE_BYTES)))
        MonoText(stringResource(R.string.mldsa_sizes_public_key, level.publicKeyBytes, ratio(level.publicKeyBytes, ED25519_PUBLIC_KEY_BYTES)))
    }
}

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit, label: String, supportingText: String? = null, isError: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
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

private fun ratio(bytes: Int, classicalBytes: Int): String = String.format(Locale.US, "%.0f", bytes.toDouble() / classicalBytes)

private val MlDsaLevel.labelRes: Int
    get() = when (this) {
        MlDsaLevel.ML_DSA_44 -> R.string.mldsa_level_44
        MlDsaLevel.ML_DSA_65 -> R.string.mldsa_level_65
        MlDsaLevel.ML_DSA_87 -> R.string.mldsa_level_87
    }

private val SigningMode.labelRes: Int
    get() = when (this) {
        SigningMode.HEDGED -> R.string.mldsa_mode_hedged
        SigningMode.DETERMINISTIC -> R.string.mldsa_mode_deterministic
    }

private val SigningMode.hintRes: Int
    get() = when (this) {
        SigningMode.HEDGED -> R.string.mldsa_mode_hedged_hint
        SigningMode.DETERMINISTIC -> R.string.mldsa_mode_deterministic_hint
    }

@Preview(showBackground = true)
@Composable
private fun MlDsaScreenPreview() {
    PqcTheme {
        MlDsaScreen(
            state = MlDsaUiState(
                keys = KeysInfo(publicKeyBytes = 1_952, privateKeyBytes = 4_032, seedBytes = 32, fingerprint = "3a:91:0c:5e:77:12:d4:8b"),
                signature = SignatureInfo(bytes = 3_309, fingerprint = "9d:44:b7:20:6e:c1:08:f3", mode = SigningMode.HEDGED, sameAsPrevious = false),
                checkMessage = "Pay Alice 100 dollars",
                checkContext = "payments",
                isValid = true,
            ),
            onLevelChange = {},
            onGenerateKeys = {},
            onMessageChange = {},
            onContextChange = {},
            onModeChange = {},
            onSign = {},
            onFlipBit = {},
            onCheckMessageChange = {},
            onCheckContextChange = {},
        )
    }
}
