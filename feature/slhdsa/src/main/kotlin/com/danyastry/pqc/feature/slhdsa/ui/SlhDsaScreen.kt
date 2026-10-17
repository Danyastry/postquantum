package com.danyastry.pqc.feature.slhdsa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.pqc.core.ui.components.SegmentedSelector
import com.danyastry.pqc.core.ui.theme.PqcTheme
import com.danyastry.pqc.feature.slhdsa.HashFamily
import com.danyastry.pqc.feature.slhdsa.R
import com.danyastry.pqc.feature.slhdsa.SlhDsaParameters
import com.danyastry.pqc.feature.slhdsa.SlhDsaStrength
import com.danyastry.pqc.feature.slhdsa.SlhDsaVariant

@Composable
fun SlhDsaRoute(
    modifier: Modifier = Modifier,
    viewModel: SlhDsaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SlhDsaScreen(
        state = state,
        onStrengthChange = viewModel::onStrengthChange,
        onVariantChange = viewModel::onVariantChange,
        onHashChange = viewModel::onHashChange,
        onGenerateKeys = viewModel::onGenerateKeys,
        onMessageChange = viewModel::onMessageChange,
        onSign = viewModel::onSign,
        onFlipBit = viewModel::onFlipBit,
        onVerify = viewModel::onVerify,
        modifier = modifier,
    )
}

@Composable
fun SlhDsaScreen(
    state: SlhDsaUiState,
    onStrengthChange: (SlhDsaStrength) -> Unit,
    onVariantChange: (SlhDsaVariant) -> Unit,
    onHashChange: (HashFamily) -> Unit,
    onGenerateKeys: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSign: () -> Unit,
    onFlipBit: () -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parameters = state.parameters
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.slhdsa_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedSelector(SlhDsaStrength.entries, parameters.strength, { stringResource(it.labelRes) }, onStrengthChange)
        SegmentedSelector(SlhDsaVariant.entries, parameters.variant, { stringResource(it.labelRes) }, onVariantChange)
        SegmentedSelector(HashFamily.entries, parameters.hash, { stringResource(it.labelRes) }, onHashChange)
        Text(
            text = stringResource(R.string.slhdsa_parameters, parameters.name, parameters.strength.nistCategory),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(if (parameters.variant == SlhDsaVariant.SMALL) R.string.slhdsa_variant_hint_small else R.string.slhdsa_variant_hint_fast),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepCard(stringResource(R.string.slhdsa_step_keys)) {
            Button(onClick = onGenerateKeys, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.work == Work.GENERATING) R.string.slhdsa_generating else R.string.slhdsa_generate))
            }
            state.keys?.let { keys ->
                MonoText(pluralStringResource(R.plurals.slhdsa_public_key, keys.publicKeyBytes, keys.publicKeyBytes))
                MonoText(pluralStringResource(R.plurals.slhdsa_private_key, keys.privateKeyBytes, keys.privateKeyBytes))
                MonoText(stringResource(R.string.slhdsa_fingerprint, keys.fingerprint))
                MonoText(stringResource(R.string.slhdsa_took, keys.millis.toInt()))
            }
        }
        StepCard(stringResource(R.string.slhdsa_step_sign)) {
            OutlinedTextField(
                value = state.message,
                onValueChange = onMessageChange,
                label = { Text(stringResource(R.string.slhdsa_message_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSign, enabled = state.canSign, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.work == Work.SIGNING) R.string.slhdsa_signing else R.string.slhdsa_sign))
            }
            if (state.work == Work.SIGNING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (parameters.variant == SlhDsaVariant.SMALL) {
                    Text(
                        text = stringResource(R.string.slhdsa_signing_slow),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.signature?.let { signature ->
                MonoText(pluralStringResource(R.plurals.slhdsa_signature, signature.bytes, signature.bytes))
                MonoText(stringResource(R.string.slhdsa_fingerprint, signature.fingerprint))
                MonoText(stringResource(R.string.slhdsa_took, signature.millis.toInt()))
                if (signature.isTampered) {
                    Text(
                        text = stringResource(R.string.slhdsa_tampered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedButton(onClick = onFlipBit, enabled = state.canTamper, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.slhdsa_flip_bit))
                }
            }
        }
        StepCard(stringResource(R.string.slhdsa_step_verify), stringResource(R.string.slhdsa_verify_hint)) {
            Button(onClick = onVerify, enabled = state.canVerify, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.work == Work.VERIFYING) R.string.slhdsa_verifying else R.string.slhdsa_verify))
            }
            state.verify?.let { result ->
                Text(
                    text = stringResource(if (result.isValid) R.string.slhdsa_valid else R.string.slhdsa_invalid, result.millis.toInt()),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (result.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
        SizesCard(parameters)
    }
}

@Composable
private fun SizesCard(current: SlhDsaParameters) {
    StepCard(stringResource(R.string.slhdsa_sizes_title), stringResource(R.string.slhdsa_sizes_note)) {
        SlhDsaStrength.entries.forEach { strength ->
            SlhDsaVariant.entries.forEach { variant ->
                val row = SlhDsaParameters(strength, variant, current.hash)
                val isCurrent = strength == current.strength && variant == current.variant
                Text(
                    text = pluralStringResource(R.plurals.slhdsa_size_row, row.signatureBytes, row.name, row.signatureBytes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
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

private val SlhDsaStrength.labelRes: Int
    get() = when (this) {
        SlhDsaStrength.BITS_128 -> R.string.slhdsa_strength_128
        SlhDsaStrength.BITS_192 -> R.string.slhdsa_strength_192
        SlhDsaStrength.BITS_256 -> R.string.slhdsa_strength_256
    }

private val SlhDsaVariant.labelRes: Int
    get() = when (this) {
        SlhDsaVariant.SMALL -> R.string.slhdsa_variant_small
        SlhDsaVariant.FAST -> R.string.slhdsa_variant_fast
    }

private val HashFamily.labelRes: Int
    get() = when (this) {
        HashFamily.SHA2 -> R.string.slhdsa_hash_sha2
        HashFamily.SHAKE -> R.string.slhdsa_hash_shake
    }

@Preview(showBackground = true)
@Composable
private fun SlhDsaScreenPreview() {
    PqcTheme {
        SlhDsaScreen(
            state = SlhDsaUiState(
                keys = KeysInfo(publicKeyBytes = 32, privateKeyBytes = 64, fingerprint = "3a:91:0c:5e:77:12:d4:8b", millis = 4),
                signature = SignatureInfo(bytes = 17_088, fingerprint = "9d:44:b7:20:6e:c1:08:f3", millis = 85),
                verify = VerifyResult(isValid = true, millis = 3),
            ),
            onStrengthChange = {},
            onVariantChange = {},
            onHashChange = {},
            onGenerateKeys = {},
            onMessageChange = {},
            onSign = {},
            onFlipBit = {},
            onVerify = {},
        )
    }
}
