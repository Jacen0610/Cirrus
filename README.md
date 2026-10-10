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

## 模型迭代（CCSN 2,543 张 / 11 云属）

同一管线三轮调参，全部可复现（`training/runs/*/metrics.csv`）：

| 轮次 | 配置 | val top1 | 结论 |
|---|---|---|---|
| exp001 | 直觉配置（30 epoch, lr 3e-4） | 49.9% | 严重过拟合：train 95% vs val 50% |
| exp002 | +强增强（旋转/狠裁剪）/标签平滑/低 LR | 49.3% | 过拟合治愈但欠训练 |
| exp003 | +100 epoch 长训 | **55.7%** | **采用**：测试集 top1 50.8% / top3 76.7% |

**混淆矩阵洞察**：视觉独特类召回高——航迹云 87%、层积云 83%、积雨云 68%；
形态相近类（层云 19%、卷云 32%、高积云 35%）在 2.5k 数据量下不可分。
因此产品设计为 **top-3 候选 + 用户一键修正**：模型收窄范围，人做终裁，
修正记录可导出为带标签照片集（数据飞轮），直接反哺下一轮微调。

## 端侧实测（Pixel 7, Tensor G2）

同一 float32 模型，Benchmark 页实测（预热 3 次、计时 20 次取统计）：

| 配置 | 平均 | 最快 | 最慢 |
|---|---|---|---|
| CPU · 1 线程 | 16.8 ms | 16.4 | 18.1 |
| CPU · 4 线程 | 16.6 ms | 16.4 | 17.1 |
| GPU delegate | 不可用* | — | — |

\* litert-gpu 1.4.2 的 CompatibilityList 对新系统的保守判定（Pixel 7 的 Mali-G710 硬件实际支持）。
经评估决定不启用 GPU 委托：CPU 延迟已无感，GPU 收益（约 16ms → 5ms）不可感知，
且需引入首初始化编译开销与 fp16 精度差异的权衡。

**发现**：该规模模型（3.5M 参数 / 224 输入）算子细小，多线程的调度开销恰好抵消并行收益——
生产配置因此固定单线程（同延迟、更省电）。模型算力 371 MFLOPs，float32 体积 10.09 MB。

## 自定义品牌图

启动页（Splash）与主页中心 Logo 共用同一张图片资源：`app/src/main/res/drawable/brand_logo.xml`
（默认为矢量白云）。换成自己的图只需三步：

1. 准备图片：建议 **512×512、透明背景的 PNG/WebP**；用于启动页时内容居中、四周留约 1/6 边距（系统会裁入圆形，太贴边会被裁掉）；
2. 把文件放到 `app/src/main/res/drawable-nodpi/brand_logo.png`（`nodpi` 目录避免按屏幕密度被二次缩放）；
3. **删除**默认的 `app/src/main/res/drawable/brand_logo.xml`（同名资源只允许存在一个，否则构建报 duplicate resource），重新构建安装——启动页与主页同时生效。

启动页背景色改 `res/values/colors.xml` 里的 `cirrus_sky`；主页 Logo 底色同步使用该颜色。

## 构建

```bash
# 调试版
./gradlew :app:assembleDebug

# 发布版（首次需生成签名密钥）
keytool -genkeypair -v -keystore cirrus.jks -keyalg RSA -keysize 2048 -validity 10000 -alias cirrus
# 然后创建 keystore.properties（模板）：
#   storeFile=../cirrus.jks
#   storePassword=...
#   keyAlias=cirrus
#   keyPassword=...
./gradlew :app:assembleRelease
```

要求：JDK 25（Gradle 会按 `gradle-daemon-jvm.properties` 自动配置）。

## 路线图

- [x] Phase 1：拍摄 → 识别 → 保存 → 日记闭环（搜索/统计/修正导出）
- [x] Phase 2：CCSN 数据集微调 MobileNetV4，float32 部署（`training/`）
- [x] Phase 3：推理 Benchmark 页（CPU 线程对比实测；int8/GPU 经评估延后，见上）
- [ ] Phase 4+：修正数据飞轮反哺微调、更多数据集（TJNU 申请中）、天气提示

## License

[MIT](LICENSE)
