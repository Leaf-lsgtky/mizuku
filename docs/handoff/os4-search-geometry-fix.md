# 搜索展开下移与顶部黑边修复

更新：2026-11-21 20:10（Asia/Shanghai）。状态：编译、主路径与关闭模糊的fallback回归均已通过。全部由主对话执行。

## 用户报告

点击搜索后整个页面向下移动，顶部出现黑边。用户指定重新参考 `E:/workspace/参考/miuix/example`。

## 已实际对照

- example/shared/src/commonMain/kotlin/GlassPage.kt：Scaffold只用graphicsLayer读取searchBarAlpha；GlassTopAppBar.contentModifier承载glassSearchBarTransition；搜索覆盖层是全尺寸兄弟层。
- IconPage.kt采用同样的页级淡出/顶栏前景变形分离。
- example/android/src/main/AndroidManifest.xml指定adjustResize。
- 目标beta02的官方pinned GlassSearchOverlay文档与源码也明确在顶栏contentModifier使用该过渡。参考example的HEAD与目标发布件并不完全相同，因此未复制其自绘旧搜索实现。

## 根因与修改

1. 之前将glassSearchBarTransition挂到整个页面Scaffold，导致整页包含背景一起向中心缩小/模糊，可能暴露窗口背景。现在整页只alpha淡出，几何过渡仅作用于顶栏foreground。
2. 之前搜索展开会直接移除bottomBar，导致shell传下的bottom padding突变，原列表/空态位置跟着移动。现在底栏保持测量高度、跟随alpha淡出，透明后不放置child以让搜索结果接收触摸；搜索中也不派发导航选择。
3. 搜索topInsets之前传完整systemBars，混入导航栏和水平安全区。现在top仅消费statusBars/displayCutout的Top，水平刘海/navigationBars独立处理。
4. MainActivity和独立ApplicationManagementActivity显式使用adjustResize，键盘由搜索组件内部imePadding处理。
5. 独立应用管理页保留顶栏测量，仅淡出其bar和normal body；顶栏前景单独变形，避免showTopBar=false引发padding跳变。

## 文件边界

- compose/screens/MiuixMainScreen.kt
- compose/screens/MiuixAppManagementScreen.kt
- compose/components/miuix/Os4Components.kt
- management/ApplicationManagementActivity.kt
- manager/src/main/AndroidManifest.xml

用户偏好、授权写入、过滤/排序和导航状态所有者不变。

## 验证结果

- git diff --check通过；`:manager:assembleDebug`实际BUILD SUCCESSFUL（3m5s），日志 `.oil-ui/os4-migration/logs/search-fix-build.log`。
- 新APK已安装到隔离API35模拟器。SHA256：`1935EB7667CBD5BD5C91498FAC75864195F4D64F60BA437C643C85594907D83C`。
- 原生搜索检查通过：搜索输入/无匹配、Back隐藏键盘、关闭和重复打开均正常。IME显示/隐藏时搜索框bounds都为`[32,134][907,260]`，没有下移；关闭前后页面标题bounds都为`[68,265][1005,364]`。
- 报告 `.oil-ui/os4-migration/logs/search-geometry-checks.json`，AndroidRuntime错误日志为空。
- 已保留修复前后4秒原生录屏；从新录屏抽取24帧（12fps）检查，覆盖原页、搜索展开和键盘进入。主对话实际查看第4、7、16、24帧和最终截图，顶部背景覆盖正常。像素检查报告为logs/search-edge-frames.json。
- 新截图：shots/search-fix-after-ime.png、search-fix-after-ime-hidden.png、search-fix-after-dismiss.png；视频shots/search-after.mp4，帧目录shots/search-after-frames。
- 具体用户真机的黑边未在本次模拟器中完整复现；已修复源码中与example不一致的整页缩放、inset混用和bottom padding跳变，并验证修复后模拟器坐标与背景稳定。没有把模拟器结果夸大成全机型结论。
- 关闭blur的fallback回归也通过：仍为134px顶部坐标，取消后返回Apps，测试后已恢复用户偏好。报告logs/search-fallback-checks.json。
- 初次直接从普通adb shell启动未exported的AppearanceActivity被系统拒绝；改为真实的Main → Settings → Appearance入口完成回归。没有修改Activity导出权限，也没有覆盖真机应用。
- 本轮不使用子代理，不安装或更改无线真机上的应用；未重新运行已知有项目级错误的全仓lint，原迁移交接中的lint限制继续适用。
