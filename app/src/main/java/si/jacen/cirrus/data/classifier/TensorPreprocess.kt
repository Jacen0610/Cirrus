package si.jacen.cirrus.data.classifier

import android.content.Context
import android.graphics.Bitmap

/**
 * bitmap → 模型输入张量的共享预处理，以及模型资产的统一读取入口。
 * 输出维度以 labels.txt 实际行数为准——换模型（如增减类别）无需改代码。
 */
internal object TensorPreprocess {

    const val MODEL_FILE = "cloud_classifier.tflite"
    const val LABELS_FILE = "labels.txt"
    const val INPUT_SIZE = 224

    fun readLabels(context: Context): List<String> =
        context.assets.open(LABELS_FILE).bufferedReader().readLines()
            .filter { it.isNotBlank() }

    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    /** 返回 NHWC [1][224][224][3] 归一化浮点张量。 */
    fun bitmapToInput(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
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
}
