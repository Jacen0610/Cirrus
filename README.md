# Cirrus ☁️

拍下天空，识别云属，记录每一次观察。端侧 AI 云类识别观察日记——识别全程离线，数据只存在你的手机上。

<!-- CI badge：仓库推到 GitHub 后取消注释并替换 OWNER/REPO
[![Android CI](https://github.com/OWNER/REPO/actions/workflows/android-ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/android-ci.yml)
-->

## 功能

- 📷 拍摄天空照片（CameraX）
- 🧠 端侧云属识别（本地推理，无网络依赖；当前为模拟实现，真实模型训练中）
- ✅ Top-3 候选 + 用户手动修正
- 📔 观察日记：时间线列表、详情、删除
- 🔒 数据完全本地（Room + 内部存储）

## 架构

```
ui (Compose + ViewModel, UDF)
 │
domain  ──  业务模型与接口（Observation / ObservationRepository / CloudClassifier）
 │
data  ──  实现层（Room、照片文件、分类器实现）
 │
Android SDK / 端侧推理
```

- UI 层：Jetpack Compose + Material 3，单向数据流（StateFlow → collectAsStateWithLifecycle）
- 数据层：Room（Flow 驱动的响应式查询）+ Repository 模式
- 识别层：`CloudClassifier` 接口 + 策略模式，模拟实现 / MediaPipe / 手写 LiteRT 通过 Hilt 切换
- 依赖注入：Hilt（KSP）

## 技术栈

| 类别 | 选择 |
|---|---|
| 语言 | Kotlin 2.2 |
| UI | Jetpack Compose（BOM 2026.02）+ Material 3 |
| 构建 | AGP 9.4 + Gradle 9 + version catalog |
| DI | Hilt 2.60 |
| 持久化 | Room 2.8 |
| 相机 | CameraX 1.6 |
| 图片加载 | Coil 3 |
| 测试 | JUnit + kotlinx-coroutines-test |

## 构建

```bash
./gradlew :app:assembleDebug
```

要求：JDK 25（Gradle 会按 `gradle-daemon-jvm.properties` 自动配置）。

## 路线图

- [x] Phase 1：拍摄 → 识别（模拟）→ 保存 → 日记闭环
- [ ] Phase 2：TJNU 数据集微调 MobileNetV4，int8 量化部署（`training/`）
- [ ] Phase 3：手写 LiteRT 推理 + CPU/GPU 延迟 Benchmark 页
- [ ] Phase 4：全文搜索、统计图表、release 发布

## License

[MIT](LICENSE)
