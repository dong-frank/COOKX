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
- 家庭、囤囤鼠与助手页面说明后续方向。邀请码协作、点菜评分、采购库存、照片上传、小红书和 Agent 尚未接入。

菜品卡片使用文字占位封面。计时器仅在做菜页面显示，不提供后台提醒；离开该页面再重新进入会开始新的制作会话。独立评分尺度、自动扣库存等仍待产品讨论。

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
