package si.jacen.cirrus.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import si.jacen.cirrus.data.local.PhotoFileStore
import si.jacen.cirrus.domain.classifier.ClassificationResult
import si.jacen.cirrus.domain.classifier.CloudClassifier
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.domain.repository.ObservationRepository
import java.io.File
import javax.inject.Inject

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data class Ready(
        val results: List<ClassificationResult>,
        val selectedIndex: Int,
    ) : ResultUiState

    data object Saved : ResultUiState
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val classifier: CloudClassifier,
    private val repository: ObservationRepository,
    private val photoStore: PhotoFileStore,
) : ViewModel() {

    private val photoFileName: String =
        savedStateHandle.get<String>(KEY_PHOTO_FILE_NAME)
            ?: error("photoFileName is required")

    val photoFile: File = photoStore.file(photoFileName)

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bitmap = photoStore.loadBitmap(photoFileName, maxDimension = CLASSIFIER_INPUT_SIZE)
            val results = bitmap?.let { classifier.classify(it) }.orEmpty()
            _uiState.value = ResultUiState.Ready(results, selectedIndex = 0)
        }
    }

    fun selectResult(index: Int) {
        _uiState.update { state ->
            if (state is ResultUiState.Ready) state.copy(selectedIndex = index) else state
        }
    }

    /**
     * 保存观察：模型主判 + 置信度原样入库；
     * 用户点选了非首位候选时，把它记为 userCorrectedLabel。
     */
    fun save() {
        val state = _uiState.value as? ResultUiState.Ready ?: return
        val topResult = state.results.firstOrNull() ?: return
        val chosen = state.results[state.selectedIndex]

        viewModelScope.launch {
            repository.addObservation(
                Observation(
                    timestampMillis = System.currentTimeMillis(),
                    cloudLabel = topResult.genus.wireName,
                    confidence = topResult.confidence,
                    photoFileName = photoFileName,
                    userCorrectedLabel = chosen.genus.wireName
                        .takeIf { state.selectedIndex != 0 },
                ),
            )
            _uiState.value = ResultUiState.Saved
        }
    }

    companion object {
        const val KEY_PHOTO_FILE_NAME = "photoFileName"
        private const val CLASSIFIER_INPUT_SIZE = 640
    }
}
