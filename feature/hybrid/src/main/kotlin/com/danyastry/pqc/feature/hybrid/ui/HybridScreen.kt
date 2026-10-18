package com.danyastry.pqc.feature.hybrid.ui

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
import com.danyastry.pqc.feature.hybrid.R

@Composable
fun HybridRoute(
    modifier: Modifier = Modifier,
    viewModel: HybridViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HybridScreen(
        state = state,
        onClientHello = viewModel::onClientHello,
        onServerRespond = viewModel::onServerRespond,
        onClientFinish = viewModel::onClientFinish,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onServerSend,
        onAttackerChange = viewModel::onAttackerChange,
        onAttack = viewModel::onAttack,
        modifier = modifier,
    )
}

@Composable
fun HybridScreen(
    state: HybridUiState,
    onClientHello: () -> Unit,
    onServerRespond: () -> Unit,
    onClientFinish: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttackerChange: (AttackerBreaks) -> Unit,
    onAttack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.hybrid_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepCard(stringResource(R.string.hybrid_step_client), stringResource(R.string.hybrid_client_hint)) {
            Button(onClick = onClientHello, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hybrid_client_hello))
            }
            if (state.clientShareBytes > 0) {
                MonoText(pluralStringResource(R.plurals.hybrid_client_share, state.clientShareBytes, state.clientShareBytes))
            }
        }
        StepCard(stringResource(R.string.hybrid_step_server), stringResource(R.string.hybrid_server_hint)) {
            Button(onClick = onServerRespond, enabled = state.canRespond, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hybrid_server_respond))
            }
            if (state.serverShareBytes > 0) {
                MonoText(pluralStringResource(R.plurals.hybrid_server_share, state.serverShareBytes, state.serverShareBytes))
                MonoText(stringResource(R.string.hybrid_server_secret, state.serverSecret))
            }
        }
        StepCard(stringResource(R.string.hybrid_step_finish)) {
            Button(onClick = onClientFinish, enabled = state.canFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hybrid_finish))
            }
            state.secretsMatch?.let { match ->
                MonoText(stringResource(R.string.hybrid_client_secret, state.clientSecret))
                Text(
                    text = stringResource(R.string.hybrid_secret_parts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(if (match) R.string.hybrid_match else R.string.hybrid_mismatch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (match) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                MonoText(stringResource(R.string.hybrid_session_key, state.sessionKey))
            }
        }
        StepCard(stringResource(R.string.hybrid_step_message)) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                label = { Text(stringResource(R.string.hybrid_input_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSend, enabled = state.canSend, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hybrid_send))
            }
            if (state.sealedBytes > 0) {
                MonoText(pluralStringResource(R.plurals.hybrid_sealed, state.sealedBytes, state.sealedBytes))
            }
        }
        StepCard(stringResource(R.string.hybrid_step_attacker), stringResource(R.string.hybrid_attacker_hint)) {
            SegmentedSelector(
                options = AttackerBreaks.entries,
                selected = state.attackerBreaks,
                label = { stringResource(it.labelRes) },
                onSelect = onAttackerChange,
            )
            Button(onClick = onAttack, enabled = state.canAttack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hybrid_attack))
            }
            when (val attack = state.attack) {
                is AttackResult.Read -> Text(
                    text = stringResource(R.string.hybrid_attack_read, attack.text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                AttackResult.Failed -> Text(
                    text = stringResource(R.string.hybrid_attack_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                null -> Unit
            }
        }
        StepCard(stringResource(R.string.hybrid_sizes_title), stringResource(R.string.hybrid_sizes_note)) {
            MonoText(stringResource(R.string.hybrid_sizes_client))
            MonoText(stringResource(R.string.hybrid_sizes_server))
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

private val AttackerBreaks.labelRes: Int
    get() = when (this) {
        AttackerBreaks.X25519 -> R.string.hybrid_breaks_x25519
        AttackerBreaks.ML_KEM -> R.string.hybrid_breaks_mlkem
        AttackerBreaks.BOTH -> R.string.hybrid_breaks_both
    }

@Preview(showBackground = true)
@Composable
private fun HybridScreenPreview() {
    PqcTheme {
        HybridScreen(
            state = HybridUiState(
                clientShareBytes = 1_216,
                serverShareBytes = 1_120,
                serverSecret = "5b:e1:02:7c:aa:93:4f:10",
                clientSecret = "5b:e1:02:7c:aa:93:4f:10",
                sessionKey = "9d:44:b7:20:6e:c1:08:f3",
                sealedBytes = 42,
                attack = AttackResult.Failed,
            ),
            onClientHello = {},
            onServerRespond = {},
            onClientFinish = {},
            onInputChange = {},
            onSend = {},
            onAttackerChange = {},
            onAttack = {},
        )
    }
}
