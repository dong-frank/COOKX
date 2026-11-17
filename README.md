# COOKX

以家庭共用菜谱为核心的 Kotlin + Jetpack Compose Android App。

产品讨论见 [产品记录](docs/product-ideas.md)，开发约定见 [AGENTS.md](AGENTS.md)，当前数据结构见 [菜谱模型](docs/recipe-model.md)。

囤囤鼠的[最终设计交接](docs/designs/pantry-fridge/README.md)集中保存定稿、交互预览、模型来源／授权及交互规则；现已采用交付渲染帧接入 App。

## 当前功能

- 囤囤鼠使用冰箱与水槽场景：点击冰箱开关物品面板，三个瓶子共用调料入口；在面板内添加、查看及修改名称和数量。仅显示当前家庭有库存的条目；零数量不参与采购便签预勾选。

- 家庭由用户主动创建；菜谱首页为左侧标签、右侧图片与名称。
- 食材独立记录名称和用量，步骤按备菜、烹饪两个阶段保存。
- 录入时逐项添加食材、步骤，选择操作类型、关联本步食材和用量；可配置火候、温度、切制尺寸、完成标准与计时。
- 支持自定义步骤和标准模板步骤；读取 JSON 中六个已确认模板。支持食材用量、可选动作、下料顺序及按需计时，具体做法保存为菜谱内副本。
- 制作模式按两阶段顺序播放，显示操作图标、本步食材和参数；固定底栏提供上一页、计时器、下一页。计时器打开时预填步骤时长，未配置则为 1 分钟，点击开始才倒计时；切换步骤不重置运行中的计时。
- 完成页采用“出锅啦”结果卡片，按当前家庭的该菜谱记录展示首次 NEW；首次进入完成页播放交接包中的衔接音效，同次制作不重复播放。
- 只在完成页显示日期与餐次设置；开始制作时自动预选，完成后保存日期、餐次与心得。
- 读取 JSON 中 33 个预设标签，支持自定义多标签，以及家庭常用标签选择和排序；首页隐藏没有菜谱的标签。
- 点击“准备食材”打开采购便签，按食材 ID 保留进度；可与囤囤鼠的已有食材登记联动。勾选不改变库存或花费。

照片上传、邀请码联网协作、点菜评分、库存数量及采购花费、小红书导入和 Agent 尚未接入。小红书调研生成的 JSON 仍是实验草稿，不会自动写入 App。

## 从干净数据开始

本次按用户要求停用旧字符串食材／步骤模型，使用 **schema 3** 和独立存储 `cookx_kitchen_v3`，不迁移 v1/v2 数据。

旧的分类兼容字段、历史餐次推定及旧数据备份逻辑已移除。新家庭不会自动获得示例菜谱；空列表中可主动选择“加入示例菜谱”。示例也使用新的结构化模型。

清空已安装 App 的全部数据（包括家庭、菜谱、记录、便签、库存与旧版本备份）：

```sh
source scripts/android-env.sh
adb -s emulator-5554 shell pm clear com.cookx.app
```

这不会修改 Git 文件、产品文档或 Mac 上的小红书调研资料。

## 开发环境与运行

本机为 Apple Silicon，Android Studio 位于 `/Applications/Android Studio.app`，SDK 位于 `~/Library/Android/sdk`。终端环境脚本使用 Studio 自带 JBR 25.0.4。

| 组件 | 当前配置 |
| --- | --- |
| Gradle / AGP | 9.1.0 / 9.0.1 |
| Kotlin / Compose Compiler | 2.3.20 |
| Compose BOM / Navigation 3 | 2026.03.01 / 1.0.1 |
| Android SDK | compileSdk / targetSdk 36，minSdk 24 |
| 编译字节码目标 | JVM 17 |
| 模拟器 | Pixel 9，`COOKX_API_36`，ARM64 |

AGP 使用内置 Kotlin，不重复应用 `org.jetbrains.kotlin.android`。依赖统一在 [版本目录](gradle/libs.versions.toml) 管理。

使用 Android Studio 打开项目根目录同步后运行 `app`；终端也可运行：

```sh
source scripts/android-env.sh
bash scripts/check-android-env.sh
android emulator start COOKX_API_36
./gradlew :app:assembleDebug
android run --device=emulator-5554 app/build/outputs/apk/debug/app-debug.apk
```

其他电脑需安装 API 36 SDK，通过 Studio 或未提交的 `local.properties` 配置 SDK。发布前按实际账号确定应用 ID；当前开发 ID 为 `com.cookx.app`。

本机 `~/.zprofile` 和 `~/.zshrc` 会加载 `~/.config/cookx/android-env.sh`；原文件备份为各自的 `.before-cookx-20261117` 文件。SDK、本机配置、构建产物和签名密钥不进入 Git。

## 工程与验证

- `domain`：版本化食材、阶段、步骤、模板、来源、待确认项和业务校验。
- `src/main/assets/presets`：预设标签及模板的唯一正式 JSON 清单，App 直接读取。
- `data`：版本化 JSON 本地仓储，在 IO 线程读写；校验通过后才覆盖保存。
- `ui`：ViewModel、Compose 编辑器与 Navigation 3；编辑草稿、导航和当次制作状态可保存恢复。
- `src/test`：引用与版本校验、模板副本、日期餐次、标签、采购便签等本地测试。
- `src/androidTest`：干净家庭核心流程、结构化编辑、模板绑定、计时恢复和本地存储等设备测试。

```sh
source scripts/android-env.sh
./gradlew build :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

2026-11-18 已验证：Debug／Release 构建与 Lint、22 项单元测试、囤囤鼠／采购便签／存储的 7 项相关设备用例。设备全流程测试会清空调试 App 的 v3 家庭数据并使用测试家庭，请使用专用模拟器。Release 构建尚未配置正式签名。

当前页面：[首页](docs/screenshots/recipes.png)、[结构化详情](docs/screenshots/recipe-detail.png)、[两阶段制作](docs/screenshots/cooking-structured.png)、[计时器](docs/screenshots/cooking-timer.png)、[清空后首次启动](docs/screenshots/clean-start.png)、[首次出锅](docs/screenshots/cooking-result-first.png)、[再次出锅](docs/screenshots/cooking-result-repeat.png)。

囤囤鼠实机截图：[默认场景](docs/screenshots/pantry-scene-closed.png)、[冰箱物品面板](docs/screenshots/pantry-scene-open.png)、[调料面板](docs/screenshots/pantry-scene-spices.png)。

## 官方 Android skills

项目内包含 37 个 [官方 Android skills](https://github.com/android/skills)，按任务选择阅读，不批量加载。来源提交见 [安装记录](.agents/android-skills-lock.json)，许可证见 [LICENSE.txt](.agents/skills/LICENSE.txt)。

本次实现使用 `android-cli`、`navigation-3`、`edge-to-edge`；App 内不添加实现说明或随意增加解释性小字。
