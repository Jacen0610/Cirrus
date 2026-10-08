package si.jacen.cirrus.domain.classifier

import android.graphics.Bitmap
import si.jacen.cirrus.domain.model.CloudGenus

data class ClassificationResult(
    val genus: CloudGenus,
    val confidence: Float,
)

/**
 * 端侧云属识别能力：输入一张照片，返回按置信度降序的识别结果。
 *
 * 实现将有三代：模拟实现（当前）→ MediaPipe（T9 训练出模型后）→
 * 手写 LiteRT（T10），通过 Hilt 绑定切换，调用方无感。
 *
 * 接口收 Bitmap 是务实取舍：严格分层会引入平台无关的图像抽象，
 * 对纯 Android 应用而言成本大于收益。
 */
interface CloudClassifier {

    suspend fun classify(bitmap: Bitmap): List<ClassificationResult>
}
