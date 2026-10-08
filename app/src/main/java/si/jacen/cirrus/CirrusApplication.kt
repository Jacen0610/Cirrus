package si.jacen.cirrus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。@HiltAndroidApp 让 Hilt 在编译期生成应用级依赖容器，
 * 是全项目所有 @Inject 注入的根；必须在 Manifest 注册才会生效。
 */
@HiltAndroidApp
class CirrusApplication : Application()
