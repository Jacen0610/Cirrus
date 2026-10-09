package si.jacen.cirrus.data.export

import si.jacen.cirrus.domain.model.Observation
import java.io.File

/** 修正数据导出能力（接口供测试替换实现）。 */
interface CorrectionExport {
    /** 无可导出数据时返回 null。 */
    suspend fun export(observations: List<Observation>): File?
}
