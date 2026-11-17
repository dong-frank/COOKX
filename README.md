# COOKX

菜谱 Android App。已初始化 Kotlin + Jetpack Compose 基础工程，包含家庭共享菜谱的本地原型。

产品构思与后续讨论见 [产品想法与讨论记录](docs/product-ideas.md)。

## 运行基础工程

使用 Android Studio 打开本项目根目录，完成 Gradle Sync，选择 `app` 与 `COOKX_API_36` 运行。也可在终端执行：

```sh
source scripts/android-env.sh
android emulator start COOKX_API_36
./gradlew :app:assembleDebug
android run --device=emulator-5554 app/build/outputs/apk/debug/app-debug.apk
```

其他电脑需安装 API 36 SDK，并通过 Studio 配置 SDK 路径，或创建不提交 Git 的 `local.properties`（`sdk.dir=本机SDK路径`）。发布前需按实际账号确定应用 ID；当前开发 ID 为 `com.cookx.app`。

当前可体验：

- 主动创建一个本机家庭；名称由用户填写，不自动创建默认家庭。
- 手动记录菜名、食材与用量、分步做法；也可主动加入两道明确标注的示例菜谱。
- 浏览菜谱与上次制作时间，进入逐步制作，使用一分钟计时器，保存本次制作心得。
- 重启后保留家庭、菜谱与制作记录；Activity 重建时恢复导航、当前步骤和计时截止时间。
- 家庭与助手页面说明后续方向。邀请码协作、点菜评分、库存数量与采购花费、照片上传、小红书和 Agent 尚未接入；囤囤鼠现支持家中已有食材登记。

菜谱首页采用左侧分类、右侧菜品列表，条目仅展示本地插画封面与名称；无照片的自建菜谱使用通用插画，食材、步骤、制作时间等信息保留在详情页。左侧改用家庭选定的常用标签；菜谱可多选预设标签或自定义标签，旧分类在升级时迁为标签。计时器仅在做菜页面显示，不提供后台提醒；离开该页面再重新进入会开始新的制作会话。独立评分尺度、自动扣库存等仍待产品讨论。

### 工程结构与验证

- `app/src/main/kotlin/com/cookx/app/domain`：家庭、菜谱、制作记录与输入校验。
- `data`：本地存储边界，当前采用版本化 JSON + SharedPreferences，在 IO 线程读写；后续可替换为数据库和远端同步。
- `ui`：ViewModel、Material 3 Compose 界面与 Navigation 3 可保存返回栈。
- `src/test`：输入校验、制作时间与序列化测试。
- `src/androidTest`：家庭 → 菜谱 → 分步计时 → 制作记录的设备流程测试，包含 Activity 重建。

当前采用单模块和直接依赖装配，不预先引入账号后端或多模块框架。家庭单例存储、菜谱与制作记录分离是这次基础工程的实现方案，不代表多家庭、权限和云端同步规则已经确定。本地文件损坏时显示错误并保留原数据，不自动清空。

```sh
source scripts/android-env.sh
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

设备流程测试会清空当前安装的 **COOKX 调试 App 数据**，创建“演示小厨房”和示例制作记录，请使用专用模拟器运行。

初始化参考官方 `android-cli`、`navigation-3` 和 `edge-to-edge` skills。编译配置参照 [AGP 9.0 兼容性](https://developer.android.com/build/releases/agp-9-0-0-release-notes)与 [Compose Compiler 配置](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)。

## 开发环境

本机使用 Apple Silicon，SDK 位于 `~/Library/Android/sdk`。

| 组件 | 配置 |
| --- | --- |
| Android Studio | Rabbit 2 Patch 1 / 2026.2.2.9，`/Applications/Android Studio.app` |
| Java | Studio 自带 JBR / OpenJDK 25.0.4 |
| Android SDK | API 36、Build Tools 36.0.0、Platform Tools |
| 命令行 | Android CLI 1.1.16919734、sdkmanager、avdmanager、ADB 37.0.1 |
| 模拟器 | Emulator 37.2.13、`COOKX_API_36`（Pixel 9） |
| 模拟器镜像 | API 36 / Google APIs / arm64-v8a |

终端配置：

```sh
source scripts/android-env.sh
bash scripts/check-android-env.sh
```

本机的 `~/.zprofile` 和 `~/.zshrc` 已添加 `~/.config/cookx/android-env.sh` 的加载入口，新终端会自动加载。原文件备份为各自的 `.before-cookx-20261117` 文件。其他电脑可自行运行上面的 `source` 命令，不需要复用本机配置。

Android Studio 的 Android SDK Location 应为 `~/Library/Android/sdk`。Gradle JDK 应与终端 `JAVA_HOME` 保持一致。本工程固定 Gradle 9.1.0、AGP 9.0.1、Kotlin / Compose compiler 2.3.20、Compose BOM 2026.03.01、Navigation 3 1.0.1；compileSdk / targetSdk 36，minSdk 24。AGP 使用内置 Kotlin，编译目标为 JVM 17，Gradle 本身运行于 Studio 的 JBR 25。依赖版本见 `gradle/libs.versions.toml`。

启动或关闭项目调试用模拟器：

```sh
source scripts/android-env.sh
emulator -avd COOKX_API_36
# 在另一个终端确认连接
adb devices -l
# 关闭上面启动的模拟器
adb -s emulator-5554 emu kill
```

若连接了多个设备，以 `adb devices -l` 的实际序列号为准。

## 官方 Android skills

来源：[android/skills](https://github.com/android/skills)，Apache-2.0。

37 个 skills 安装在项目的 `.agents/skills/`，包含各自的 references、scripts 等资源；无需全局安装。固定的上游提交和目录映射见 [安装记录](.agents/android-skills-lock.json)，许可证见 [LICENSE.txt](.agents/skills/LICENSE.txt)。这些文件应随项目一起提交。

开发时可使用已安装的 skills。可以直接描述 Android 任务，也可以指定 skill，例如：

- “使用 android-cli 帮我创建 Kotlin + Jetpack Compose 工程。”
- “使用 navigation-3 配置菜谱列表与详情页导航。”
- “使用 edge-to-edge 检查系统栏和内容布局。”
- “使用 testing-setup 规划测试。”

Android CLI 可检查项目 skills：

```sh
source scripts/android-env.sh
android skills list --agent=codex --project=.
```

项目开发约定见 [AGENTS.md](AGENTS.md)。SDK、镜像、`local.properties` 和签名密钥不进入 Git。

## 环境验证（2026-11-17）

- 环境检查脚本通过；Hypervisor.Framework 硬件加速可用。
- `COOKX_API_36` 实际冷启动成功；ADB 返回 `sys.boot_completed=1`，Android 16、`arm64-v8a`。
- 临时目录中的最小 Java Activity 示例，使用 JBR 25 + Gradle 9.1.0 + AGP 9.0.1 + API 36，执行 `:app:assembleDebug` 成功，并完成 APK 安装及 Activity 启动验证。该示例不属于正式 App 工程，也未验证 Compose 依赖。
- 官方 CLI 的 `empty-activity` 模板当时默认引用 API 37，但 SDK 软件源中未提供该平台；正式初始化工程时需核对模板及 SDK 版本。
- Android Studio 已安装并启动，但屏幕捕获工具报错，未验证其首次启动向导；若仍显示向导，请在 Studio 中完成，并使用上述 SDK 路径。

## 基础工程验证（2026-11-17）

- `./gradlew build :app:assembleDebugAndroidTest` 成功，包含 Debug / Release 构建、3 个本地单测和 Lint。
- Lint 无错误，保留 18 条依赖更新、模板资源和 KTX 风格提示；没有为清除更新提示而升级已验证的版本组合。
- `connectedDebugAndroidTest` 的核心设备流程通过；扩展后的最终设备测试也通过，覆盖新增菜谱空输入校验、填写与保存、计时中 Activity 重建、制作心得保存与读取。
- 强制停止 App 后冷启动成功，界面恢复“演示小厨房”、3 道菜与 1 次制作记录，确认本地数据跨进程恢复。
- 已检查 [家庭入口截图](docs/screenshots/welcome.png)和 [菜谱首页截图](docs/screenshots/recipes.png)。当前验证设备为 Pixel 9 / API 36 ARM64，尚未完成真机、平板、不同字体大小的全面验证。
- 模拟器保留已安装的调试 App 和上述测试数据，供直接体验。Release APK 为未签名构建，尚未配置发布签名。

### 点餐式菜谱首页（2026-11-17）

- 保留现有主题配色，左侧分类独立滚动，右侧列表按分类筛选；切换分类回到列表顶部。
- 列表条目仅有图片与菜名，点击进入原详情页；新增菜谱入口位于顶栏。
- 新增 `RecipeBrowserTest` 验证筛选、空分类、状态恢复和点击回调；该组件测试不读写家庭本地数据。
- 分类字段有默认值，兼容未带分类的历史 JSON；本地单测覆盖兼容读取与分类输入处理。
- 本次验证：Debug 构建、Lint（无错误）、4 个本地单测、1 个分类组件设备测试通过；旧数据未清空，并已在模拟器检查最终首页截图。

## 标签与日期餐次（2026-11-17）

- 首页右上角“标签管理”：创建家庭自定义标签、勾选常用标签、上移/下移排序；首页固定保留“全部”和“未打标签”。首页菜品条目仍只显示图片和名称。
- 新增菜谱可多选标签；已有菜谱在详情中点“编辑标签”。同一道菜可出现在多个标签列表。取消常用标签只隐藏入口，不删除菜谱标签。
- 点击“开始制作”时，以设备本地日期和小时预选：05:00–10:00 早餐、10:00–15:00 午餐、15:00–21:00 晚餐，其余为夜宵（起点含、终点不含）。凌晨默认当天，可手动改为前一天。
- 制作完成后的记录页才展示日期和餐次设置：日期可点开日历调整，餐次可点击切换；切换步骤、跨时段、页面重建或完成制作时不自动覆盖已选值。
- 详情与记录展示“日期 · 餐次”，不再显示精确时分。按所选日期、早餐/午餐/晚餐/夜宵顺序及保存时间排序。
- 本地状态升级至 schema 2，升级前在同一私有存储中保留 `state_backup_before_tags_meals` 原始 JSON。旧分类迁为标签，旧记录餐次由历史时间推定，内部保留推定标识（详情页不展示该提示）；重复读取不重复迁移。
- 标签重命名、删除、组合筛选、历史补记独立入口尚未实现。

此次设备组件测试与存储迁移测试使用内存或独立命名的测试存储，不清空现有家庭数据；原 `KitchenJourneyTest` 仍会清空调试 App 的测试数据，运行全套设备测试时请使用专用模拟器。

本次验证：8 个本地单测和 5 个设备测试通过；Debug/Release 构建及 Lint 通过（无错误）。已在实际安装的 App 中核对旧数据升级后的数量与原始备份。界面预览：[标签管理](docs/screenshots/tag-settings.png)、[日期与餐次](docs/screenshots/cooking-meal.png)。实现参考官方 `android-cli`、`navigation-3` 与 `edge-to-edge` skills。

## 准备食材与采购便签（2026-11-17）

- 菜谱详情点击“准备食材”卡片，打开本菜的采购便签；按项勾选已准备的食材，保留用量原文及进度。
- 在“囤囤鼠”登记家中已有食材，可切换“已有／暂时没有”。目前仅登记是否已有，不记录数量、采购入库或花费。
- 首次生成便签时，按当前家庭的已有食材确定匹配并预选；按已确认的“已有就预选”规则处理，不推断是否足量；页面不额外显示解释性提示。
- 匹配只去除可识别的用量后缀，再按名称精确比较，不做食材别名或替代品推断。组合食材行保留原文，由用户手动核对。
- 便签勾选不写入库存，也不增加制作记录或支出。重开保留进度，“重新检查”保留所有手动选择。
- 数据字段以空列表默认值兼容旧记录；便签与已有食材信息保存在原家庭的本机存储内。采购便签不是做菜完成状态。
- 新增 `ShoppingMemoTest` 与 `ShoppingMemoUiTest`，覆盖匹配、家庭隔离、手动选择优先、重复行、重开持久化及库存不变；设备测试使用独立存储，不清空当前家庭数据。

本次验证：12 个本地单测、2 个采购便签设备测试通过；Debug/Release 构建及 Lint 通过。已在实际 App 验证入口、勾选、返回重开及库存/制作记录不变。[采购便签截图](docs/screenshots/shopping-memo.png)。实现参考官方 `android-cli`、`navigation-3` 与 `edge-to-edge` skills。
