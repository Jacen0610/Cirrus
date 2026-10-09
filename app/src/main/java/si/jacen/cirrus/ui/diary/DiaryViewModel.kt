package si.jacen.cirrus.ui.diary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import si.jacen.cirrus.data.export.CorrectionExport
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.domain.repository.ObservationRepository
import javax.inject.Inject

sealed interface ExportState {
    data object Idle : ExportState
    data object Busy : ExportState
    data object NoData : ExportState
    data class Done(val path: String) : ExportState
    data class Failed(val message: String) : ExportState
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    repository: ObservationRepository,
    private val correctionExporter: CorrectionExport,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Room 表变化 → Flow 重发射 → 这里转为可被 Compose 收集的 UI 状态。 */
    val observations: StateFlow<List<Observation>> = repository.observeObservations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val queryValue: StateFlow<String> = query.asStateFlow()

    /**
     * 搜索过滤：个人日记的数据量级（数百条）下内存过滤即瞬时完成，
     * 数据量增长后可平滑升级为 Room FTS4（决策见 README）。
     */
    val filteredObservations: StateFlow<List<Observation>> =
        combine(observations, query) { list, text ->
            if (text.isBlank()) list else list.filter { it.matchesQuery(text) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 云属分布统计（按生效标签：修正优先于模型判定），按数量降序。 */
    val genusCounts: StateFlow<List<Pair<String, Int>>> = observations
        .map { list ->
            list.groupingBy { it.userCorrectedLabel ?: it.cloudLabel }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set

    fun onQueryChange(value: String) {
        query.update { value }
    }

    fun exportCorrections() {
        if (exportState == ExportState.Busy) return
        exportState = ExportState.Busy
        viewModelScope.launch {
            exportState = try {
                val file = correctionExporter.export(observations.first())
                if (file == null) ExportState.NoData else ExportState.Done(file.absolutePath)
            } catch (e: Exception) {
                ExportState.Failed(e.message ?: "unknown error")
            }
        }
    }

    fun consumeExportResult() {
        if (exportState != ExportState.Busy) exportState = ExportState.Idle
    }

    private fun Observation.matchesQuery(text: String): Boolean {
        val keyword = text.trim()
        val effective = userCorrectedLabel ?: cloudLabel
        return effective.contains(keyword, ignoreCase = true) ||
            cloudLabel.contains(keyword, ignoreCase = true)
    }
}
