package si.jacen.cirrus.data.classifier

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import si.jacen.cirrus.domain.classifier.ClassificationResult
import si.jacen.cirrus.domain.classifier.CloudClassifier
import si.jacen.cirrus.domain.model.CloudGenus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * 端侧推理实现：加载 assets 中自训练的 MobileNetV4 tflite 模型。
 *
 * 预处理参数是训练管线的契约（training/scripts/export.py 的 meta），
 * 两端必须一致，否则精度会静默劣化：
 * 输入 224×224、NHWC、像素归一化 (v/255 - mean) / std（ImageNet 统计）、
 * 输出 11 类 logits（此处做 softmax）。
 */
@Singleton
class LitertCloudClassifier @Inject constructor(
    @ApplicationContext context: Context,
) : CloudClassifier {

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        val modelBytes = context.assets.open(TensorPreprocess.MODEL_FILE).use { it.readBytes() }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(modelBytes)
            .apply { rewind() }
        interpreter = Interpreter(
            modelBuffer,
            Interpreter.Options().apply { setNumThreads(NUM_THREADS) },
        )

        // 输出维度以 labels.txt 为准，兼容后续类别数变化（如负样本类）
        labels = TensorPreprocess.readLabels(context)
        require(labels.size >= 2) { "labels.txt 类别数异常: ${labels.size}" }
    }

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult> =
        withContext(Dispatchers.Default) {
            val input = TensorPreprocess.bitmapToInput(bitmap)
            val output = Array(1) { FloatArray(labels.size) }
            interpreter.run(input, output)
            postprocess(output[0])
        }

    /** logits → softmax → 取 top-3 并按 labels 映射回 CloudGenus。
     *  最高置信度低于阈值时返回空（拒识）：softmax 必然输出某个类别，
     *  无云/非天空照片需要靠阈值挡在门外。阈值待真实数据校准。 */
    private fun postprocess(logits: FloatArray): List<ClassificationResult> {
        val maxLogit = logits.max()
        val exps = logits.map { exp(it - maxLogit) }
        val sum = exps.sum()
        val probabilities = exps.map { it / sum }

        if (probabilities.max() < MIN_CONFIDENCE) return emptyList()

        val ranked = labels.indices
            .map { index -> index to probabilities[index] }
            .sortedByDescending { it.second }

        // 负样本类命中且位居首位 → 判定"未识别到云"，直接拒识
        if (ranked.first().let { (index, _) ->
                labels[index] == CloudGenus.NON_CLOUD.wireName
            }
        ) return emptyList()

        return ranked
            .take(TOP_K)
            .mapNotNull { (index, probability) ->
                CloudGenus.entries
                    .firstOrNull { it.wireName == labels[index] }
                    ?.let { genus -> ClassificationResult(genus, probability) }
            }
    }

    companion object {
        // 实测（Pixel 7, Benchmark 页）：1 线程与 4 线程延迟相同（~16.6ms），
        // 算子过小导致多线程无收益——用单线程以降低功耗与发热
        private const val NUM_THREADS = 1
        private const val TOP_K = 3

        // 拒识阈值：低于此置信度视为"未识别到云"。
        // 当前为启发值；更优做法是给训练集补充负样本类（见 README 路线图）
        private const val MIN_CONFIDENCE = 0.45f
    }
}
