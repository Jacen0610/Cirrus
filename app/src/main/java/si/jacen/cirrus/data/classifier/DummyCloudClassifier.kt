package si.jacen.cirrus.data.classifier

import android.graphics.Bitmap
import kotlinx.coroutines.delay
import si.jacen.cirrus.domain.classifier.ClassificationResult
import si.jacen.cirrus.domain.classifier.CloudClassifier
import si.jacen.cirrus.domain.model.CloudGenus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 识别层的占位实现：随机返回云属、构造合理的置信度梯度、
 * 并模拟 120–300ms 推理延迟，让 UI 的加载态可被真实观察到。
 * bitmap 参数当前被忽略，真实实现将在此做缩放/归一化等预处理。
 */
@Singleton
class DummyCloudClassifier @Inject constructor() : CloudClassifier {

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult> {
        delay(Random.nextLong(120, 300))

        val candidates = CloudGenus.entries.shuffled()
        val topConfidence = Random.nextFloat() * 0.35f + 0.60f
        val secondConfidence = Random.nextFloat() * (topConfidence - 0.25f) + 0.15f
        val thirdConfidence = Random.nextFloat() * secondConfidence

        return listOf(
            ClassificationResult(candidates[0], topConfidence),
            ClassificationResult(candidates[1], secondConfidence),
            ClassificationResult(candidates[2], thirdConfidence),
        )
    }
}
