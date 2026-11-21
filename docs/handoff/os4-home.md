# OS4 Home GlassDialog 生命周期修复交接

## 修改范围

只改了 `manager/src/main/java/moe/shizuku/manager/compose/screens/MiuixHomeScreen.kt` 中 `MiuixAccessibilityDialog` 的对话框组合；新增本交接文档。没有改其他源码。

## 已实施

- 将原先按 `dialogKind` 分支创建的多个 `GlassDialog`（以及分支中的 `MiuixShowPermissionDialog`）合并为一个始终留在该 Composable 组合中的 `GlassDialog`，由 `show = show && dialogKind >= 0` 驱动显隐。各 `dialogKind` 只切换内部正文和标题，避免关闭期间分支切换导致宿主实例被移除/替换。
- `dialogKind` 在打开时通过 `LaunchedEffect(show)` 求值；关闭时不重置，因此退出动画期间保留正在关闭的标题和正文。`-1` 初始状态不显示空白对话框；新一轮打开重新计算目标类型。
- 将权限提示正文及动作内联到稳定宿主的正文分支；删除无其他调用者的私有 `MiuixShowPermissionDialog`。继续保持其取消/继续路径：取消关闭；继续通过现有回调关闭并跳转无障碍设置。

## 未改及理由

- 其他 `GlassDialog` 调用点（关于、停止服务、ADB 命令/发现/配对、WADB/USB 提示、自动化令牌确认）均由其 Composable 在父层持续调用、通过 `show` 控制，并非按 `show` 条件移除；本次目标问题不在这些点，不改。
- `MiuixAccessibilityDialog` 的权限判断顺序、三种提示内容与原有关闭/跳转动作保持不变；未改权限检查或导航策略。
- 不添加退出完成后的 `dialogKind` 清理：保留内容直到下一轮打开时重算，避免在动画结束前清掉退出所需正文；隐藏的 GlassDialog 仍由其 `show` 状态控制。

## 核对与验证边界

- 已读取目标文件、项目随附 `.oil-ui/os4-migration/reference/glassdialog.md` 与 `GlassDialog.kt`。项目说明明确要求关闭时保留调用直到退出动画结束；源码将 `show` 转发给 `OverlayDialog`。
- 静态检查确认 `MiuixAccessibilityDialog` 现在仅有一个稳定的 `GlassDialog` 调用；对该源码运行 `git diff --check` 无输出。
- 未运行 Gradle；未声称编译或设备运行时验证通过。
