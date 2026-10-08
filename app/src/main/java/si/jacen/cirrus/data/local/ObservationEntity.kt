package si.jacen.cirrus.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次云观察的持久化记录。
 *
 * 设计要点：
 * - photoFileName 只存文件名（照片本体在内部存储 photos/ 目录），
 *   存绝对路径会在重装/迁移后失效；
 * - userCorrectedLabel 预留用户改判字段，避免日后表结构变更引入迁移；
 * - latitude/longitude 可空：定位权限未接入前先存 null。
 */
@Entity(
    tableName = "observations",
    indices = [Index("timestampMillis")],
)
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val cloudLabel: String,
    val confidence: Float,
    val photoFileName: String?,
    val userCorrectedLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
