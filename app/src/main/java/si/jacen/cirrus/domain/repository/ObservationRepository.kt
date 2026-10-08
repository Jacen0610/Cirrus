package si.jacen.cirrus.domain.repository

import kotlinx.coroutines.flow.Flow
import si.jacen.cirrus.domain.model.Observation

/**
 * 观察记录仓库：对上层暴露的唯一数据入口。
 * 上层（ViewModel/UI）依赖此接口，永远不知道 Room 的存在。
 */
interface ObservationRepository {

    fun observeObservations(): Flow<List<Observation>>

    suspend fun addObservation(observation: Observation): Long

    suspend fun findObservation(id: Long): Observation?

    suspend fun deleteObservation(id: Long)
}
