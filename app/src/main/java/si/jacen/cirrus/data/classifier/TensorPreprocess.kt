package si.jacen.cirrus.data.classifier

import android.graphics.Bitmap

/**
 * bitmap → 模型输入张量的共享预处理。
 * 参数是训练管线的契约（见 LitertCloudClassifier 的说明），两端必须一致。
 */
internal object TensorPreprocess {

    const val INPUT_SIZE = 224
    const val NUM_CLASSES = 12

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
