package si.jacen.cirrus.ui.common

import androidx.annotation.StringRes
import si.jacen.cirrus.R
import si.jacen.cirrus.domain.model.CloudGenus

/** wireName（与模型输出对齐的稳定标识）→ 枚举，未知标签返回 null。 */
fun String.asCloudGenus(): CloudGenus? =
    CloudGenus.entries.firstOrNull { it.wireName == this }

/** 云属的本地化文案资源 ID——UI 层专用，domain 不感知资源系统。 */
@StringRes
fun CloudGenus.labelRes(): Int = when (this) {
    CloudGenus.CUMULUS -> R.string.genus_cumulus
    CloudGenus.ALTOCUMULUS -> R.string.genus_altocumulus
    CloudGenus.CIRRUS -> R.string.genus_cirrus
    CloudGenus.CIRROSTRATUS -> R.string.genus_cirrostratus
    CloudGenus.STRATOCUMULUS -> R.string.genus_stratocumulus
    CloudGenus.STRATUS -> R.string.genus_stratus
    CloudGenus.CUMULONIMBUS -> R.string.genus_cumulonimbus
}
