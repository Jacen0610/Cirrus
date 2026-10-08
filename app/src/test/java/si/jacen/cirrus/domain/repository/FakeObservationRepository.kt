package si.jacen.cirrus.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import si.jacen.cirrus.domain.model.Observation

/**
 * 纯内存假仓库：domain 接口不依赖任何框架，
 * 因此单测无需数据库与安卓环境即可覆盖 ViewModel 逻辑。
 */
class FakeObservationRepository(
    initial: List<Observation> = emptyList(),
) : ObservationRepository {

    private val observations = MutableStateFlow(initial)

    override fun observeObservations(): Flow<List<Observation>> = observations

    override suspend fun addObservation(observation: Observation): Long {
        observations.update { it + observation }
        return observation.id
    }

    override suspend fun findObservation(id: Long): Observation? =
        observations.value.firstOrNull { it.id == id }

    override suspend fun deleteObservation(id: Long) {
        observations.update { list -> list.filterNot { it.id == id } }
    }
}
