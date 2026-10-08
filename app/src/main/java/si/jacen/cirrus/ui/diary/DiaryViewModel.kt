package si.jacen.cirrus.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.domain.repository.ObservationRepository
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    repository: ObservationRepository,
) : ViewModel() {

    /** Room 表变化 → Flow 重发射 → 这里转为可被 Compose 收集的 UI 状态。 */
    val observations: StateFlow<List<Observation>> = repository.observeObservations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
