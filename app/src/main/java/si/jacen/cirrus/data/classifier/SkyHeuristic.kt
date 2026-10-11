package si.jacen.cirrus.data.classifier

import android.graphics.Bitmap
import android.graphics.Color

/**
 * 天空启发式：判定照片上部是否以天空为主（明亮 + 蓝色系或灰白系）。
 * 作为端侧分类前的粗筛——softmax 模型没有"非云"输出空间，
 * 室内/地面/绿植等非天空照片由这里挡下（走拒识路径）。
 *
 * 已知局限（颜色投票、无纹理分析）：纯白墙面等极少数边角可能漏过；
 * 橙红色晚霞会被误拒（暖色不在允许色域内）——如需支持可加平滑度判定。
 */
internal object SkyHeuristic {

    private const val SAMPLE_SIZE = 64
    private const val UPPER_FRACTION = 0.66f
    private const val MIN_SKY_PIXEL_RATIO = 0.55f
    private const val MIN_BRIGHTNESS = 0.20f
    private const val MAX_SATURATION_FOR_NEUTRAL = 0.35f
    private const val BLUE_HUE_MIN = 185f
    private const val BLUE_HUE_MAX = 260f

    fun looksLikeSky(bitmap: Bitmap): Boolean {
        val scaled = Bitmap.createScaledBitmap(bitmap, SAMPLE_SIZE, SAMPLE_SIZE, true)
        val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
        scaled.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)

        val rows = (SAMPLE_SIZE * UPPER_FRACTION).toInt()
        val hsv = FloatArray(3)
        var skyLike = 0
        var total = 0
        for (y in 0 until rows) {
            for (x in 0 until SAMPLE_SIZE) {
                Color.colorToHSV(pixels[y * SAMPLE_SIZE + x], hsv)
                val isSkyPixel = hsv[2] >= MIN_BRIGHTNESS && (
                    hsv[1] <= MAX_SATURATION_FOR_NEUTRAL ||
                        hsv[0] in BLUE_HUE_MIN..BLUE_HUE_MAX
                    )
                if (isSkyPixel) skyLike++
                total++
            }
        }
        return skyLike.toFloat() / total >= MIN_SKY_PIXEL_RATIO
    }
}
