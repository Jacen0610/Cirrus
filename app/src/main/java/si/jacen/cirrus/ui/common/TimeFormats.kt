package si.jacen.cirrus.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

fun Long.toDateTimeText(): String =
    formatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
