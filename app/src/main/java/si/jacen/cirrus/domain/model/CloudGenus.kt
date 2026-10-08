package si.jacen.cirrus.domain.model

/**
 * 云属词表。wireName 是与模型输出标签对齐的稳定标识，
 * UI 层用它映射本地化文案。类别集合以 TJNU 数据集为准，
 * T9 拿到真实模型后如有出入在此收口。
 */
enum class CloudGenus(val wireName: String) {
    CUMULUS("cumulus"),
    ALTOCUMULUS("altocumulus"),
    CIRRUS("cirrus"),
    CIRROSTRATUS("cirrostratus"),
    STRATOCUMULUS("stratocumulus"),
    STRATUS("stratus"),
    CUMULONIMBUS("cumulonimbus"),
}
