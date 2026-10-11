package si.jacen.cirrus.ui.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import si.jacen.cirrus.R
import si.jacen.cirrus.ui.theme.CirrusTheme

/** 模型信息页：把训练/部署的真实指标与工程决策直接呈现给用户与审阅者。 */
@Composable
fun ModelInfoScreen(onOpenBenchmark: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.model_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp),
        )

        InfoCard(titleRes = R.string.model_section_model) {
            Text(stringResource(R.string.model_model_desc), style = MaterialTheme.typography.bodySmall)
        }

        InfoCard(titleRes = R.string.model_section_accuracy) {
            Text(stringResource(R.string.model_accuracy_desc), style = MaterialTheme.typography.bodySmall)
        }

        InfoCard(titleRes = R.string.model_section_latency) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.model_latency_cpu1), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.model_latency_cpu4), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.model_latency_gpu), style = MaterialTheme.typography.bodySmall)
            }
        }

        InfoCard(titleRes = R.string.model_section_decisions) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.model_decision_int8), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.model_decision_gpu), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.model_decision_threads), style = MaterialTheme.typography.bodySmall)
            }
        }

        InfoCard(titleRes = R.string.model_section_limitations) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.model_limitation_ood), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.model_limitation_confusable), style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(
            onClick = onOpenBenchmark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Text(stringResource(R.string.action_open_benchmark))
        }
    }
}

@Composable
private fun InfoCard(titleRes: Int, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ModelInfoScreenPreview() {
    CirrusTheme() {
        ModelInfoScreen(onOpenBenchmark = {})
    }
}