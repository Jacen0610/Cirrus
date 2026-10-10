package si.jacen.cirrus.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import si.jacen.cirrus.R
import si.jacen.cirrus.ui.common.labelRes
import kotlin.math.roundToInt

@Composable
fun ResultScreen(
    onSaved: () -> Unit,
    onDiscard: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is ResultUiState.Saved) onSaved()
    }

    ResultScaffold(onDiscard = onDiscard) { padding ->
        when (val state = uiState) {
            ResultUiState.Loading -> LoadingBox(Modifier.padding(padding))
            ResultUiState.Saved -> LoadingBox(Modifier.padding(padding))

            is ResultUiState.Ready -> ReadyContent(
                state = state,
                photoFile = viewModel.photoFile,
                onSelect = viewModel::selectResult,
                onSave = viewModel::save,
                onDiscard = onDiscard,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultScaffold(onDiscard: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_result)) },
                navigationIcon = {
                    TextButton(onClick = onDiscard) {
                        Text(stringResource(R.string.action_discard))
                    }
                },
            )
        },
    ) { padding -> content(padding) }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReadyContent(
    state: ResultUiState.Ready,
    photoFile: java.io.File,
    onSelect: (Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 固定布局：照片用 weight 吃掉剩余高度（超出裁剪），候选与按钮始终可见
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = photoFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(16.dp)),
        )

        if (state.results.isEmpty()) {
            Text(stringResource(R.string.result_no_candidates))
        } else {
            Text(stringResource(R.string.result_hint))
            state.results.forEachIndexed { index, result ->
                CandidateRow(
                    label = stringResource(result.genus.labelRes()),
                    confidencePercent = (result.confidence * 100).roundToInt(),
                    selected = index == state.selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSave,
            enabled = state.results.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_save))
        }
        TextButton(
            onClick = onDiscard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Text(stringResource(R.string.action_discard))
        }
    }
}

@Composable
private fun CandidateRow(
    label: String,
    confidencePercent: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.confidence_percent, confidencePercent))
    }
}
