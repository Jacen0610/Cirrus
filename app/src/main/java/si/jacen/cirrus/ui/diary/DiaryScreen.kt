package si.jacen.cirrus.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import si.jacen.cirrus.R
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.ui.common.LocalPhotoFileStore
import si.jacen.cirrus.ui.common.asCloudGenus
import si.jacen.cirrus.ui.common.labelRes
import si.jacen.cirrus.ui.common.toDateTimeText

@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    onOpenObservation: (Long) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val observations by viewModel.observations.collectAsStateWithLifecycle()
    val photoFileStore = LocalPhotoFileStore.current

    DiaryScaffold(
        observations = observations,
        onBack = onBack,
        onOpenObservation = onOpenObservation,
        photoFileOf = { observation ->
            observation.photoFileName?.let(photoFileStore::file)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryScaffold(
    observations: List<Observation>,
    onBack: () -> Unit,
    onOpenObservation: (Long) -> Unit,
    photoFileOf: (Observation) -> java.io.File?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_diary)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (observations.isEmpty()) {
            EmptyDiary(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
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

@Composable
private fun EmptyDiary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
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
    photoFile: java.io.File?,
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
