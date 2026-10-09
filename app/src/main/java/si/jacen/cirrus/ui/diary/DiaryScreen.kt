package si.jacen.cirrus.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import si.jacen.cirrus.R
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.ui.common.LocalPhotoFileStore
import si.jacen.cirrus.ui.common.asCloudGenus
import si.jacen.cirrus.ui.common.labelRes
import si.jacen.cirrus.ui.common.toDateTimeText
import java.io.File

@Composable
fun DiaryScreen(
    onBack: (() -> Unit)?,
    onOpenObservation: (Long) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val query by viewModel.queryValue.collectAsStateWithLifecycle()
    val observations by viewModel.filteredObservations.collectAsStateWithLifecycle()
    val allObservations by viewModel.observations.collectAsStateWithLifecycle()
    val genusCounts by viewModel.genusCounts.collectAsStateWithLifecycle()
    val photoFileStore = LocalPhotoFileStore.current
    val snackbarHostState = remember { SnackbarHostState() }

    val exportDoneText = stringResource(R.string.export_done, "%s")
    val exportNoneText = stringResource(R.string.export_none)
    val exportFailedText = stringResource(R.string.export_failed, "%s")

    LaunchedEffect(viewModel.exportState) {
        when (val state = viewModel.exportState) {
            is ExportState.Done -> {
                snackbarHostState.showSnackbar(exportDoneText.format(state.path))
                viewModel.consumeExportResult()
            }
            ExportState.NoData -> {
                snackbarHostState.showSnackbar(exportNoneText)
                viewModel.consumeExportResult()
            }
            is ExportState.Failed -> {
                snackbarHostState.showSnackbar(exportFailedText.format(state.message))
                viewModel.consumeExportResult()
            }
            ExportState.Idle, ExportState.Busy -> Unit
        }
    }

    DiaryScaffold(
        query = query,
        onQueryChange = viewModel::onQueryChange,
        genusCounts = genusCounts,
        observations = observations,
        allObservationsEmpty = allObservations.isEmpty(),
        exporting = viewModel.exportState == ExportState.Busy,
        onExport = viewModel::exportCorrections,
        onBack = onBack,
        onOpenObservation = onOpenObservation,
        snackbarHostState = snackbarHostState,
        photoFileOf = { observation ->
            observation.photoFileName?.let(photoFileStore::file)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryScaffold(
    query: String,
    onQueryChange: (String) -> Unit,
    genusCounts: List<Pair<String, Int>>,
    observations: List<Observation>,
    allObservationsEmpty: Boolean,
    exporting: Boolean,
    onExport: () -> Unit,
    onBack: (() -> Unit)?,
    onOpenObservation: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    photoFileOf: (Observation) -> File?,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_diary)) },
                navigationIcon = {
                    onBack?.let { back ->
                        TextButton(onClick = back) {
                            Text(stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    TextButton(onClick = onExport, enabled = !exporting) {
                        Text(stringResource(R.string.action_export))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (genusCounts.isNotEmpty()) {
                StatsCard(
                    counts = genusCounts,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            when {
                allObservationsEmpty -> EmptyDiary(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                observations.isEmpty() -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.search_no_result))
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(observations, key = { it.id }) { observation ->
                        DiaryCard(
                            observation = observation,
                            photoFile = photoFileOf(observation),
                            onClick = { onOpenObservation(observation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(counts: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val maxCount = counts.first().second.coerceAtLeast(1)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.title_stats),
                style = MaterialTheme.typography.titleSmall,
            )
            counts.take(6).forEach { (wireName, count) ->
                val genus = wireName.asCloudGenus()
                val label = genus
                    ?.let { stringResource(it.labelRes()) }
                    ?: wireName
                Column {
                    Row {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(count / maxCount.toFloat())
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDiary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_diary_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.empty_diary_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun DiaryCard(
    observation: Observation,
    photoFile: File?,
    onClick: () -> Unit,
) {
    val effectiveWireName = observation.userCorrectedLabel ?: observation.cloudLabel
    val effectiveGenus = effectiveWireName.asCloudGenus()
    val effectiveLabel = effectiveGenus
        ?.let { genus -> stringResource(genus.labelRes()) }
        ?: effectiveWireName

    Card(onClick = onClick) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            photoFile?.let { file ->
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = effectiveLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = observation.timestampMillis.toDateTimeText(),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (observation.userCorrectedLabel != null) {
                    val correctedLabel = observation.userCorrectedLabel.asCloudGenus()
                        ?.let { genus -> stringResource(genus.labelRes()) }
                        ?: observation.userCorrectedLabel
                    Text(
                        text = stringResource(R.string.label_corrected_with_arg, correctedLabel),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
