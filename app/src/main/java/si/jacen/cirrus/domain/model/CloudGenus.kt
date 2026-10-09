package si.jacen.cirrus.domain.model

/**
 * 云属词表（对齐 CCSN 数据集的 11 个 WMO 类别）。
 * wireName 是与模型输出标签对齐的稳定标识，
 * UI 层用它映射本地化文案。
 */
enum class CloudGenus(val wireName: String) {
    ALTOCUMULUS("altocumulus"),
    ALTOSTRATUS("altostratus"),
    CIRROCUMULUS("cirrocumulus"),
    CIRROSTRATUS("cirrostratus"),
    CIRRUS("cirrus"),
    CONTRAILS("contrails"),
    CUMULONIMBUS("cumulonimbus"),
    CUMULUS("cumulus"),
    NIMBOSTRATUS("nimbostratus"),
    STRATUS("stratus"),
    STRATOCUMULUS("stratocumulus"),
}
