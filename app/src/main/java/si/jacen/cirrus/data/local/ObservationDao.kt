package si.jacen.cirrus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 观察记录的数据访问接口。写操作用 suspend（一次性完成），
 * 读操作返回 Flow（表内容变化时 Room 自动重新发射，驱动 UI 刷新）。
 */
@Dao
interface ObservationDao {

    @Insert
    suspend fun insertObservation(observation: ObservationEntity): Long

    @Query("SELECT * FROM observations ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE id = :id")
    suspend fun getById(id: Long): ObservationEntity?

    @Query("DELETE FROM observations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
