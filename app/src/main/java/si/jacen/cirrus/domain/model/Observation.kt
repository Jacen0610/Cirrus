package si.jacen.cirrus.domain.model

/**
 * 领域模型：一次云观察的业务视图。
 * 与 ObservationEntity 字段对应但互不依赖——UI 和业务层只认识这个类型，
 * Room 的存在被隔离在 data 层内部。
 */
data class Observation(
    val id: Long = 0,
    val timestampMillis: Long,
    val cloudLabel: String,
    val confidence: Float,
    val photoFileName: String?,
    val userCorrectedLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
