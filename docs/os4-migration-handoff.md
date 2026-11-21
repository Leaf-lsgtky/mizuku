# OS4 / Miuix 迁移交接

更新：2026-11-21（Asia/Shanghai）。当前源码与 Debug APK 已编译、打包和模拟器验证；全仓 lint 未通过，具体限制见下文。没有执行 Git 提交或推送。

## 最新：搜索下移与顶部黑边修正（20:10）

- 用户报告搜索展开时页面下移、顶部黑边；已重新阅读官方example的GlassPage、IconPage、搜索helper及Android Manifest，并按目标beta02接口修正。
- 页级几何过渡挂错层级已修：整页仅淡出，缩放/模糊仅用于GlassTopAppBar.contentModifier；底栏保留测量高度防止padding突变，透明后不放置，避免截获结果触摸。
- topInsets与水平安全区分开，Main与独立应用管理Activity显式adjustResize。
- assembleDebug通过；模拟器主路径、键盘显隐/重复搜索/关闭、blur-off均通过。搜索框top始终134px，关闭后标题bounds完全恢复；已实际看过过渡帧，顶部覆盖正常。
- 最新APK及SHA以下文最终APK条目为准。具体证据、参考与限制见 [搜索几何修复](handoff/os4-search-geometry-fix.md)。本任务模拟器已关闭，未操作用户真机。

## 上一批次接续结果（19:35）

- 用户要求继续，持久目标已resume，仍由主对话直接实现。
- 审查并修复液态底栏状态同步：remember不再以每次变化的回调实例为key；rememberUpdatedState接收当前选择/回调；外部选择同步只更新指示器，不再反向发出选择。拖动取消通过独立onDragCancelled回到现有目标页，不把取消当成完成。
- 液态Tab补选中语义，图标不重复朗读相邻标签；重建DampedDragAnimation时同步重建依赖它的按压高光。
- 新构建+lint日志`.oil-ui/os4-migration/logs/continuation-validation.log`：assembleDebug成功，lint346 errors/163 warnings/2 hints，仍未全绿；新的APK已安装，本文SHA与产物已同步。
- 原生回归continuation-checks.py已通过（job `pwsh-6f971d562d4948f0`退出0）：教程/导出picker取消、液态CANCEL保留Settings、完成相同拖动到Home、Tab选择语义与pager同步均实际核对。报告logs/continuation-checks.json、日志continuation-checks-resumed.log。首轮picker启动期间null root失败保留历史，不算通过；后续从已观察到的DocumentsUI状态接续。全部测试仅指定emulator-5556。
- 当前搜索修复APK新SHA256：`1935EB7667CBD5BD5C91498FAC75864195F4D64F60BA437C643C85594907D83C`。最新lint分类continuation-lint-summary.json：346 errors（含218 fatal）/163 warnings/2 hints；本轮液态/拖动/共享宿主源码没有新增lint条目。
- 初次非exported Activity从普通adb shell进入被拒，后续在隔离模拟器adb root后测试；发生过进程未完成启动的ANR，已记录continuation-runtime-diagnostics.log。重新安装新APK后主页面与终端教程已稳定显示，不能把此前桌面截图当通过证据。
- 本轮重新启动同一个隔离API35模拟器，明确所有ADB测试指定emulator-5556；已有无线真机不纳入自动安装/偏好变更。
- 中文语言切换已通过：真实语言菜单选中文（中国），等待Activity配置重建完成后核对主页“选择下方的启动方式”和底栏“应用”，然后从真实菜单恢复跟随系统。报告logs/continuation-locale.json、截图shots/continuation-home-chinese.png。菜单滚动与配置切换期间的测试失败保留日志，未修改业务配置来伪造通过。
- 追加搜索几何修复：对照`E:/workspace/参考/miuix/example/shared/GlassPage.kt`与`GlassSearchOverlay.kt`，整页只淡出，`glassSearchBarTransition`只挂顶栏contentModifier；搜索overlay单独全屏；topInsets只取statusBars/displayCutout顶部，水平安全区独立；Main与独立应用管理Activity显式adjustResize。详情见 [搜索几何交接](handoff/os4-search-geometry-fix.md)。
- 搜索新APK验证报告 `.oil-ui/os4-migration/logs/search-geometry-checks.json`：键盘显示/隐藏top坐标都134px，关闭后标题bounds恢复，重复打开成功；24帧顶部像素检查无黑边；blur-off fallback也通过 `.oil-ui/os4-migration/logs/search-fallback-checks.json`。
- 本轮仅额外修改液态选择/取消处理和选中语义，其余UI方案保持最终自适应契约。交付前关闭本任务模拟器，不触及无线真机。

## 用户要求与最终决定

- 目标工程：`E:/workspace/mizuku`。参考 Mishka 的 Glass 迁移文档、UI 规范与 Mishka / InstallerX-Revived 实现。
- **用户最新要求优先：全部由主对话研究实现，不再使用子代理。** 早期子任务文件只保留作历史记录，不能据此重新委派。
- 重点核对：主页状态卡、底栏长度、搜索框、TopAppBar 按钮间距；补齐启动日志等此前没有 Miuix 分支的页面。
- **底栏最终是内容自适应宽度。** 已撤掉早期强制铺满和总宽 440dp 的方案；旧截图与旧检查点不能作为最终标准。
- 保留 Material 分支、三 Tab / Activity 导航、原业务和偏好键。没有引入另一套导航框架，也没有新增宽屏 rail。

## 已完成的实现

### 共享宿主与 Glass 采样

- [Os4Components.kt](../manager/src/main/java/moe/shizuku/manager/compose/components/miuix/Os4Components.kt) 提供 `Os4Scaffold`、`LocalPageBackdrop`、宽短窗口固定标题和内容自适应 Glass 底栏。
- 外层 Scaffold 的 popupHost 承载弹窗；录制其中的页面与栏，内层 Scaffold 使用空 popupHost。正文单独录制供顶栏采样，所有录制先绘制不透明 surface。
- 录制 modifier 位于 inset / padding 之前；Glass 栏不再外叠旧高斯模糊，也不裁掉渐隐带。
- 用户关闭 blur 或 RuntimeShader 不支持时，不附加对应采样 modifier，组件使用纯色 fallback；标题过渡 blur 同步关闭。
- 返回图标 28dp、RTL 镜像；主壳与二级宿主的顶栏 actions 统一 8dp 间距。

### 底栏：最终自适应规则

- [MiuixMainScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/MiuixMainScreen.kt) 的外层容器只负责居中和安全区，胶囊不强制铺满屏幕。
- 液态底栏保留 [FloatingBottomBar.kt](../manager/src/main/java/moe/shizuku/manager/compose/components/FloatingBottomBar.kt) 的 `IntrinsicSize.Min`，与 InstallerX 同源参考一致；父层不再通过 fillMaxWidth 锁死它。每项最小 76dp，22dp 图标，11sp / 14sp 单行文字。
- 原生 `GlassNavigationBar` 的指示器布局不支持直接 intrinsic 查询：实际测试曾触发无限宽度尺寸越界，已经修复。
- `ContentSizedGlassNavigationBar` 先根据当前语言、字体与官方 label 规格测量文字，结合每项最小可交互尺寸、官方内边距计算内容宽度，传入有限约束；唯一总宽上限是当前可用安全宽度。**这里没有固定总宽或屏幕占比。**
- 使用短导航标题 Apps / 应用 / 應用程式，完整页面名仍用于页面标题与无障碍描述。
- 液态折射代码同步 beta02 的 material 描边、sensor 包和浮点 downscaleFactor；使用实际应用深浅色，传感器 State 在 draw 阶段读取。

### 首页状态卡

- [MiuixHomeScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/MiuixHomeScreen.kt) 对照 InstallerX 大状态卡与 Mishka 语义色：16dp 内边距、148dp 最小高度、170dp 边角水印、标题与辅助文案分层。
- [MiuixStatusColors.kt](../manager/src/main/java/moe/shizuku/manager/compose/theme/MiuixStatusColors.kt) 集中管理运行 / 停止 / 过渡状态的深浅色，动态取色保留对应角色。
- 未运行态补启动指引；Starting / Stopping 观察原 StateMachine，未创建平行业务状态。
- 应用统计入口接回真实 Apps Tab，静态 API 统计去掉无效点击。
- 对话框保留组合直至退出，ADB 草稿每次打开重置；无障碍提示使用一个稳定 GlassDialog 宿主。

### 搜索、设置和补迁移页面

- [MiuixAppManagementScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/MiuixAppManagementScreen.kt) 使用官方 GlassSearchOverlay，与 Mishka 的顶栏入口、Glass Search/Close 图标与页面过渡对齐。
- 搜索层位于页面 Scaffold 之上；正常列表与搜索结果共用授权状态和回调，移除旧假结果列表 / 空操作。每次授权只写入一次，包名 + uid 标识多用户条目。
- 无结果、键盘、返回 / 关闭、水平缺口和底部安全区均有实际接法。
- [MiuixSettingsScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/MiuixSettingsScreen.kt) 使用 Glass 对话框 / 状态式端口输入；取消不保存，确认退场后再交接重启提示。
- [AppearanceScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/AppearanceScreen.kt) 使用 GlassDropdownPreference，Material 外壳使用 Window fallback；主题 / blur 继续经原 ThemeStore 持久化。
- [StarterScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/StarterScreen.kt)、[AdbPairingTutorialScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/AdbPairingTutorialScreen.kt)、[ShellTutorialScreen.kt](../manager/src/main/java/moe/shizuku/manager/compose/screens/ShellTutorialScreen.kt) 新增完整 Miuix / Os4Scaffold 分支，保留原 Material 分支与业务。
- [SettingsActivity.kt](../manager/src/main/java/moe/shizuku/manager/settings/SettingsActivity.kt) 与 [ApplicationManagementActivity.kt](../manager/src/main/java/moe/shizuku/manager/management/ApplicationManagementActivity.kt) 的独立入口也随当前 UI 主题分流，避免仍直接加载 Material 页面。
- 独立透明权限、兼容性提示、报告 Activity 保留原 Miuix WindowDialog 宿主，以保留窗口 / 任务 / 结果回传生命周期；没有机械替换为无正确宿主的 GlassDialog。

## 必要构建调整

- Miuix 全套实际使用 Android 坐标 `0.9.5-beta02`；新增 Glass / Glass icons / material，navigationevent 改为 AndroidX 1.2.0。
- 官方固定证据 commit：`66248bacc7be29ae6bdc0957de9458b42ac67732`。已读取 pinned GlassTopAppBar / SearchOverlay / NavigationBar / Dialog / material 文档与源码；参考项目不同版本的 API 不作目标签名证据。
- AAR 实际要求 compile SDK 37 minor 2；对应 Compose / AndroidX 要求 AGP >= 9.1。工程已对齐 AGP 9.5.1、Gradle 9.9.1、Kotlin 与 Compose compiler 2.4.21。
- Java target 21、minSdk 24 保留。Manifest 只保留 blur overrideLibrary，minSdk 由 Gradle 定义。
- AGP 9 当前通过 `android.builtInKotlin=false` 与 `android.newDsl=false` 保留原 Kotlin / legacy variant 接入；这些兼容选项有 AGP 10 弃用限制，未用全局 warning suppression 掩盖。
- Compose API 同步为 computedStateOf / CapturedAnimatedVisibility，相关字段使用 TextFieldState。

## 已实际验证

- `git diff --check` 通过。
- `:manager:compileDebugKotlin` 通过。
- `:manager:assembleDebug` 通过，四 ABI 原生部分已构建。
- 最终源码构建与lint日志：[continuation-validation.log](../.oil-ui/os4-migration/logs/continuation-validation.log)。`assembleDebug`完成，整条命令退出1来自后续lint错误；没有把lint失败当成编译通过的证据。
- 本轮原生回归报告：[continuation-checks.json](../.oil-ui/os4-migration/logs/continuation-checks.json)，包含液态拖动CANCEL/完成、选中语义、pager同步、教程与真实导出picker取消。
- 最终 APK：[shizuku-v13.6.0.r97-thedjchi-debug.apk](../manager/build/outputs/apk/debug/shizuku-v13.6.0.r97-thedjchi-debug.apk)。SHA256：`1935EB7667CBD5BD5C91498FAC75864195F4D64F60BA437C643C85594907D83C`。
- API 35 隔离模拟器：三 Tab、真实点击切页、搜索输入 / 无匹配 / 关闭、端口草稿取消重开、65536 禁用确认、独立设置入口与外观入口、深浅主题 / blur 切换、启动失败后的 Glass 提示与日志页面。
- Glass 底栏在手机 / 宽屏中的 Tab 区域换算宽度为约 218.29dp / 217.33dp，说明宽屏没有拉长；不是以像素宽度直接比较不同 density。
- 主对话亲自查看了普通字体与 200% 字体、宽短窗口、液态条、深浅色、搜索和日志截图。宽屏固定小标题，大字体标题可换行。
- 顶栏按钮中心间距实测 137px（420dpi），减默认 44dp 视觉按钮尺寸为 8.19dp，处于像素取整容差内；48dp 无障碍触摸范围不能直接当视觉按钮边界。
- 最终交互报告：[final-interaction-result.json](../.oil-ui/os4-migration/logs/final-interaction-result.json)、[native-checks.json](../.oil-ui/os4-migration/logs/native-checks.json)。原生 Window / Activity 返回栈测试要用正确入口；自动化启动 Main 时使用 CLEAR_TOP，避免把旧 Task 顶部的独立 Settings 当成 Main。

## 截图与证据入口

所有过程证据位于忽略的 `.oil-ui/os4-migration/`，正式应用代码不依赖它。

- 最终浅色主页 / 内容宽度：[final-home-light.png](../.oil-ui/os4-migration/shots/final-home-light.png)
- 最终深色主页 / Glass 底栏：[content-sized-phone.png](../.oil-ui/os4-migration/shots/content-sized-phone.png)
- 宽屏：[content-sized-wide.png](../.oil-ui/os4-migration/shots/content-sized-wide.png)
- 200% 字体：[content-sized-large-font.png](../.oil-ui/os4-migration/shots/content-sized-large-font.png)
- 液态底栏：[liquid-content-sized.png](../.oil-ui/os4-migration/shots/liquid-content-sized.png)
- 搜索与无匹配：[aligned-search-no-match.png](../.oil-ui/os4-migration/shots/aligned-search-no-match.png)
- 实际失败端口后的启动日志：[starter-log.png](../.oil-ui/os4-migration/shots/starter-log.png)，[错误提示](../.oil-ui/os4-migration/shots/starter-error.png)
- 端口草稿：[port-reopened.png](../.oil-ui/os4-migration/shots/port-reopened.png)，[非法值](../.oil-ui/os4-migration/shots/port-invalid.png)
- 本轮终端教程：[continuation-shell-final.png](../.oil-ui/os4-migration/shots/continuation-shell-final.png)；配对教程：[continuation-pairing-unobstructed.png](../.oil-ui/os4-migration/shots/continuation-pairing-unobstructed.png)
- 中文主页与自适应底栏：[continuation-home-chinese.png](../.oil-ui/os4-migration/shots/continuation-home-chinese.png)，[语言验证报告](../.oil-ui/os4-migration/logs/continuation-locale.json)
- 液态拖动取消：[continuation-liquid-cancel.png](../.oil-ui/os4-migration/shots/continuation-liquid-cancel.png)；完成拖动：[continuation-liquid-drag-complete.png](../.oil-ui/os4-migration/shots/continuation-liquid-drag-complete.png)

## 验证限制与历史失败

- 全仓 `:manager:lintDebug` 最近一次报告为 **346 errors / 163 warnings / 2 hints**，未通过，不能声称 lint 全绿。[报告](../manager/build/reports/lint-results-debug.html) 与 [本轮分类记录](../.oil-ui/os4-migration/logs/continuation-lint-summary.json) 保留。包括旧资源默认值、ExtraTranslation、MissingTranslation、旧 API / LocalContext 使用；只确认了部分与 HEAD 相同，未断言每一项都是历史问题。
- 新文案维护英文与简繁中文，其余语言明确使用英文 fallback；没有批量伪造翻译或全局关闭 lint。
- 首轮软件 GPU 出现 shader 创建阻塞和系统 ANR；改为 host GPU 并稳定系统后，最终原生流程通过。不能据此宣称所有设备性能已验证。
- 原生 Glass intrinsic 尺寸崩溃属于本轮实现错误，已通过内容预测量 + 有限约束修复；**不要改回给原生 GlassNavigationBar 直接查询 IntrinsicSize.Min**。
- 没有连接真机，未验证真实 Root / ADB 成功启动与第三方客户端授权全流程、所有旧 Android 版本和全部语言 / RTL。启动日志测试使用隔离模拟器的真实失败 localhost:1，没有伪造成功数据。
- 用户要求停止子代理后，视觉和交互由主对话完成自查，没有另派视觉评分代理。

## 复现环境与接续方式

- 当前本机 SDK：`E:/Android/Sdk`；Gradle 缓存位于 `E:/Android/.gradle`。
- 预装 JDK 21 不能正常启动；为任务下载并验证了 Temurin 21，位于 `.oil-ui/os4-migration/tools/temurin21/jdk-21.0.13.1+2`。只在命令进程设置 JAVA_HOME，未改全局配置。
- 正常环境用自己的 JDK 21 + SDK 37.2 执行 wrapper，无需复制过程工具或更改签名。
- 测试模拟器是本任务独立 AVD `mizuku-os4-api35`、port 5556，最终已请求关闭并收集退出结果；未操作其他设备。恢复时用 `.oil-ui/os4-migration/avd` 作为 ANDROID_AVD_HOME，host GPU 启动。
- 新对话先读本文、检查 Git 状态和最新差异；不要覆盖已有改动，不要按旧子任务文档恢复固定宽度方案。若有新用户要求再调整，不重复全量迁移。
