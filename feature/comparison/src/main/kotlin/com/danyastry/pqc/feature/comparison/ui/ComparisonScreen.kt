package com.danyastry.pqc.feature.comparison.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.pqc.core.ui.theme.PqcTheme
import com.danyastry.pqc.feature.comparison.Benchmark
import com.danyastry.pqc.feature.comparison.KeyExchangeResult
import com.danyastry.pqc.feature.comparison.R
import com.danyastry.pqc.feature.comparison.SignatureResult
import java.util.Locale

@Composable
fun ComparisonRoute(
    modifier: Modifier = Modifier,
    viewModel: ComparisonViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ComparisonScreen(state = state, onRun = viewModel::onRun, modifier = modifier)
}

@Composable
fun ComparisonScreen(state: ComparisonUiState, onRun: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.comparison_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRun, enabled = !state.isRunning, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.total > 0) R.string.comparison_run_again else R.string.comparison_run))
        }
        state.measuring?.let { name ->
            Text(
                text = stringResource(R.string.comparison_measuring, name, state.done + 1, state.total),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(progress = { state.done.toFloat() / state.total }, modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = pluralStringResource(R.plurals.comparison_method, Benchmark.RUNS, Benchmark.RUNS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.keyExchanges.isNotEmpty()) {
            ResultsCard(stringResource(R.string.comparison_key_exchange_title)) {
                state.keyExchanges.forEachIndexed { index, result ->
                    if (index > 0) HorizontalDivider()
                    KeyExchangeRow(result)
                }
            }
        }
        if (state.signatures.isNotEmpty()) {
            ResultsCard(stringResource(R.string.comparison_signatures_title)) {
                state.signatures.forEachIndexed { index, result ->
                    if (index > 0) HorizontalDivider()
                    SignatureRow(result)
                }
            }
        }
        if (!state.isRunning && state.signatures.isNotEmpty()) {
            Text(
                text = stringResource(R.string.comparison_takeaway),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun KeyExchangeRow(result: KeyExchangeResult) {
    SchemeRow(result.name, result.isQuantumSafe) {
        MonoText(stringResource(R.string.comparison_kex_sizes, result.publicKeyBytes, result.wireBytes))
        MonoText(stringResource(R.string.comparison_kex_times, millis(result.keyGenMillis), millis(result.sendMillis), millis(result.receiveMillis)))
    }
}

@Composable
private fun SignatureRow(result: SignatureResult) {
    SchemeRow(result.name, result.isQuantumSafe) {
        MonoText(stringResource(R.string.comparison_sig_sizes, result.publicKeyBytes, result.signatureBytes))
        MonoText(stringResource(R.string.comparison_sig_times, millis(result.keyGenMillis), millis(result.signMillis), millis(result.verifyMillis)))
    }
}

@Composable
private fun SchemeRow(name: String, isQuantumSafe: Boolean, details: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = MaterialTheme.typography.titleSmall)
            SafetyBadge(isQuantumSafe)
        }
        details()
    }
}

@Composable
private fun SafetyBadge(isQuantumSafe: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isQuantumSafe) colors.primaryContainer else colors.errorContainer,
        contentColor = if (isQuantumSafe) colors.onPrimaryContainer else colors.onErrorContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(if (isQuantumSafe) R.string.comparison_quantum_safe else R.string.comparison_classical),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ResultsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun MonoText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
}

internal fun millis(value: Double): String = when {
    value < 1 -> String.format(Locale.US, "%.2f ms", value)
    value < 100 -> String.format(Locale.US, "%.1f ms", value)
    else -> String.format(Locale.US, "%.0f ms", value)
}

@Preview(showBackground = true)
@Composable
private fun ComparisonScreenPreview() {
    PqcTheme {
        ComparisonScreen(
            state = ComparisonUiState(
                keyExchanges = listOf(
                    KeyExchangeResult("X25519", false, 32, 32, 0.4, 0.5, 0.3),
                    KeyExchangeResult("ML-KEM-768", true, 1_184, 1_088, 0.2, 0.2, 0.2),
                ),
                signatures = listOf(
                    SignatureResult("Ed25519", false, 32, 64, 0.3, 0.4, 0.6),
                    SignatureResult("ML-DSA-65", true, 1_952, 3_309, 0.5, 1.2, 0.4),
                ),
                total = 14,
                done = 4,
            ),
            onRun = {},
        )
    }
}
