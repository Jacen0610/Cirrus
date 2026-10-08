package si.jacen.cirrus.ui.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import si.jacen.cirrus.data.local.PhotoFileStore
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.domain.repository.ObservationRepository
import java.io.File
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Ready(val observation: Observation?) : DetailUiState
    data object Deleted : DetailUiState
}

@HiltViewModel
class ObservationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ObservationRepository,
    private val photoFileStore: PhotoFileStore,
) : ViewModel() {

    private val observationId: Long =
        savedStateHandle[KEY_OBSERVATION_ID] ?: error("observation id is required")

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Ready(repository.findObservation(observationId))
        }
    }

    fun photoFileOf(observation: Observation): File? =
        observation.photoFileName?.let(photoFileStore::file)

    fun delete() {
        viewModelScope.launch {
            repository.deleteObservation(observationId)
            _uiState.value = DetailUiState.Deleted
        }
    }

    companion object {
        const val KEY_OBSERVATION_ID = "id"
    }
}
