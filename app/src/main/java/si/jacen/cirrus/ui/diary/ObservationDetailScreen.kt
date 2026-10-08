package si.jacen.cirrus.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import si.jacen.cirrus.ui.common.LocalPhotoFileStore
import si.jacen.cirrus.ui.common.asCloudGenus
import si.jacen.cirrus.ui.common.labelRes
import si.jacen.cirrus.ui.common.toDateTimeText
import kotlin.math.roundToInt

@Composable
fun ObservationDetailScreen(
    onBack: () -> Unit,
    viewModel: ObservationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoFileStore = LocalPhotoFileStore.current

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.Deleted) onBack()
    }

    ObservationDetailScaffold(
        uiState = uiState,
        photoFileOf = { observation ->
            observation.photoFileName?.let(photoFileStore::file)
        },
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObservationDetailScaffold(
    uiState: DetailUiState,
    photoFileOf: (si.jacen.cirrus.domain.model.Observation) -> java.io.File?,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_observation_detail)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when (val state = uiState) {
            DetailUiState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            DetailUiState.Deleted -> Box(modifier)

            is DetailUiState.Ready -> {
                val observation = state.observation
                if (observation == null) {
                    Box(modifier, contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.detail_missing))
                    }
                } else {
                    DetailContent(
                        observation = observation,
                        photoFile = photoFileOf(observation),
                        modifier = modifier,
                        onRequestDelete = { showDeleteDialog = true },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) { Text(stringResource(R.string.dialog_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailContent(
    observation: si.jacen.cirrus.domain.model.Observation,
    photoFile: java.io.File?,
    modifier: Modifier = Modifier,
    onRequestDelete: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (photoFile != null) {
            AsyncImage(
                model = photoFile,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }

        val modelGenus = observation.cloudLabel.asCloudGenus()
        InfoRow(
            label = stringResource(R.string.label_model_result),
            value = modelGenus
                ?.let { stringResource(it.labelRes()) }
                .orEmpty() + " " + stringResource(
                R.string.confidence_percent,
                (observation.confidence * 100).roundToInt(),
            ),
        )

        observation.userCorrectedLabel?.let { corrected ->
            val correctedGenus = corrected.asCloudGenus()
            InfoRow(
                label = stringResource(R.string.label_corrected),
                value = correctedGenus
                    ?.let { stringResource(it.labelRes()) }
                    ?: corrected,
            )
        }

        InfoRow(
            label = stringResource(R.string.label_time),
            value = observation.timestampMillis.toDateTimeText(),
        )

        Button(
            onClick = onRequestDelete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Text(stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
