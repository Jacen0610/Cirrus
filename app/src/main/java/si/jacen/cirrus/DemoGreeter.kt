package si.jacen.cirrus

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 依赖注入链路的冒烟验证类，确认真实界面落地后删除。
 */
@Singleton
class DemoGreeter @Inject constructor() {

    fun greeting(): String = "Hilt 已装配 · Cirrus"
}
