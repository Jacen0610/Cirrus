package si.jacen.cirrus.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import si.jacen.cirrus.data.local.PhotoFileStore

/**
 * 照片文件查找作为应用级单例通过 CompositionLocal 提供给 UI，
 * ViewModel 因此不持有文件系统依赖，保持可在 JVM 单测中构造。
 */
val LocalPhotoFileStore = staticCompositionLocalOf<PhotoFileStore> {
    error("PhotoFileStore not provided")
}
