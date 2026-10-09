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
        val modelBytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(modelBytes)
            .apply { rewind() }
        interpreter = Interpreter(
            modelBuffer,
            Interpreter.Options().apply { setNumThreads(NUM_THREADS) },
        )

        labels = context.assets.open(LABELS_FILE).bufferedReader().readLines()
            .filter { it.isNotBlank() }
        require(labels.size == NUM_CLASSES) {
            "labels.txt 有 ${labels.size} 类，与模型输出 $NUM_CLASSES 维不符"
        }
    }

    override suspend fun classify(bitmap: Bitmap): List<ClassificationResult> =
        withContext(Dispatchers.Default) {
            val input = preprocess(bitmap)
            val output = Array(1) { FloatArray(NUM_CLASSES) }
            interpreter.run(input, output)
            postprocess(output[0])
        }

    /** bitmap → [1][224][224][3] 归一化浮点张量（NHWC）。 */
    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val tensor = Array(INPUT_SIZE) { row ->
            Array(INPUT_SIZE) { col ->
                val pixel = pixels[row * INPUT_SIZE + col]
                floatArrayOf(
                    (((pixel shr 16) and 0xFF) / 255f - MEAN[0]) / STD[0],
                    (((pixel shr 8) and 0xFF) / 255f - MEAN[1]) / STD[1],
                    ((pixel and 0xFF) / 255f - MEAN[2]) / STD[2],
                )
            }
        }
        return arrayOf(tensor)
    }

    /** logits → softmax → 取 top-3 并按 labels 映射回 CloudGenus。 */
    private fun postprocess(logits: FloatArray): List<ClassificationResult> {
        val maxLogit = logits.max()
        val exps = logits.map { exp(it - maxLogit) }
        val sum = exps.sum()
        val probabilities = exps.map { it / sum }

        return labels.indices
            .map { index -> index to probabilities[index] }
            .sortedByDescending { it.second }
            .take(TOP_K)
            .mapNotNull { (index, probability) ->
                CloudGenus.entries
                    .firstOrNull { it.wireName == labels[index] }
                    ?.let { genus -> ClassificationResult(genus, probability) }
            }
    }

    companion object {
        private const val MODEL_FILE = "cloud_classifier.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE = 224
        private const val NUM_CLASSES = 11
        private const val NUM_THREADS = 4
        private const val TOP_K = 3
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
