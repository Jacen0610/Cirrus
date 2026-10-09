package si.jacen.cirrus.data.classifier

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 推理性能基准：同一张输入分别跑 CPU(1/4 线程) 与 GPU delegate，
 * 输出延迟统计。每个配置独立创建 Interpreter（互不共享模型缓冲区）。
 */
@Singleton
class ClassifierBenchmark @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class ConfigurationResult(
        val label: String,
        val available: Boolean,
        val avgMs: Double,
        val minMs: Double,
        val maxMs: Double,
        val note: String? = null,
    )

    suspend fun run(rounds: Int = 20): List<ConfigurationResult> = withContext(Dispatchers.Default) {
        // 延迟与图像内容无关，用纯色位图即可，避免依赖用户照片
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF87CEEB.toInt())
        }
        val input = TensorPreprocess.bitmapToInput(bitmap)
        val output = Array(1) { FloatArray(TensorPreprocess.NUM_CLASSES) }
        val modelBytes = context.assets.open(MODEL_FILE).use { it.readBytes() }

        val results = mutableListOf<ConfigurationResult>()

        results += measure(
            label = "CPU · 1 线程",
            modelBytes = modelBytes,
            input = input,
            output = output,
            rounds = rounds,
            options = Interpreter.Options().apply { setNumThreads(1) },
        )
        results += measure(
            label = "CPU · 4 线程",
            modelBytes = modelBytes,
            input = input,
            output = output,
            rounds = rounds,
            options = Interpreter.Options().apply { setNumThreads(4) },
        )

        val compatibilityList = CompatibilityList()
        if (compatibilityList.isDelegateSupportedOnThisDevice) {
            results += measure(
                label = "GPU delegate",
                modelBytes = modelBytes,
                input = input,
                output = output,
                rounds = rounds,
                options = Interpreter.Options().apply {
                    addDelegate(GpuDelegate(compatibilityList.bestOptionsForThisDevice))
                },
            )
        } else {
            results += ConfigurationResult(
                label = "GPU delegate",
                available = false,
                avgMs = 0.0,
                minMs = 0.0,
                maxMs = 0.0,
                note = "该设备/驱动不支持 GPU 委托",
            )
        }

        results
    }

    private fun measure(
        label: String,
        modelBytes: ByteArray,
        input: Any,
        output: Any,
        rounds: Int,
        options: Interpreter.Options,
    ): ConfigurationResult {
        val buffer = ByteBuffer.allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(modelBytes)
            .apply { rewind() }

        return try {
            Interpreter(buffer, options).use { interpreter ->
                repeat(WARMUP_ROUNDS) { interpreter.run(input, output) }

                val timesMicros = LongArray(rounds) {
                    val start = System.nanoTime()
                    interpreter.run(input, output)
                    (System.nanoTime() - start) / 1_000
                }
                ConfigurationResult(
                    label = label,
                    available = true,
                    avgMs = timesMicros.average() / 1_000,
                    minMs = timesMicros.min() / 1_000.0,
                    maxMs = timesMicros.max() / 1_000.0,
                )
            }
        } catch (t: Throwable) {
            ConfigurationResult(
                label = label,
                available = false,
                avgMs = 0.0,
                minMs = 0.0,
                maxMs = 0.0,
                note = t.message?.take(80),
            )
        }
    }

    companion object {
        private const val MODEL_FILE = "cloud_classifier.tflite"
        private const val WARMUP_ROUNDS = 3
    }
}
