# OS4 迁移子任务交接：应用管理页

更新时间：2026-11-21（Asia/Shanghai）。负责 `/root/os4_app_list`，不做 Git 提交。

## 修改边界

- 代码仅修改 `manager/src/main/java/moe/shizuku/manager/compose/screens/MiuixAppManagementScreen.kt`。
- 此文件本来就不拥有 Scaffold 或顶栏；主壳 `MiuixMainScreen` 继续拥有顶栏、pager、正文 backdrop 和 padding。
- 根代理负责全局 Miuix 0.9.5-beta02 依赖和 `compose/components/miuix/Os4Components.kt`，本页消费其中 `LocalPageBackdrop`。
- Material 页面和业务数据所有者保持原实现。

## 已完成

- 应用列表保留单一 list-owned nested-scroll 连接；`overScrollVertical(onOffsetChanged = ...)` 将回弹偏移传入同一个 `scrollBehavior.state.overscrollOffset`。
- ADB 受限提示从独立 WindowDialog 迁到 Scaffold 托管的 `GlassDialog`，消费有效 `LocalPageBackdrop`，确认按钮改 `GlassDialogButton` 的 Primary 角色。
- 提示始终保持组合，用 `show` 控制显示，保留退出动画。
- HTML 提示继续使用 Android TextView 和原链接处理，并跟随 Miuix 主题设置文字颜色。
- 应用图标统一 40dp / 8dp 圆角，占位与已加载图标尺寸一致。
- 移除列表末尾重复 16dp Spacer，保留 contentPadding 的 16dp 底部留白。
- 刷新入口在已有 loading/refresh 中不再重复触发。

## 接口证据

已阅读两个 Miuix skills 及 page-shell、settings-and-overlays、state-and-performance 相关规则；阅读用户给的 Mishka 两份参考文档并比对其 GlassDialogTextButton / ListEditDialog。

官方文档固定 commit `66248bacc7be29ae6bdc0957de9458b42ac67732`：

- `docs/components/glass/glassdialog.md`：GlassDialog 的 Scaffold 宿主、nullable backdrop、show 生命周期；GlassDialogButton 的 role/content API。
- `docs/components/basic/pulltorefresh.md`：刷新状态和 contentPadding。
- `docs/guide/utils.md` / skill 的 scroll-refresh-and-overscroll 合同：onOffsetChanged 与 nestedScroll 的列表所有权。

## 验证及剩余事项

- 已对源码做局部审查；等待根代理完成依赖/helper 后统一编译。
- 尚无 compile-verified / runtime-verified 结论。
- 真机待验：列表回弹与顶栏同步、刷新完成、搜索期间弹窗宿主、HTML 链接、暗色/关闭 blur/不支持 shader 时的不透明回退。
- 原有包名/图标查询在组合中同步进行的问题不属于本轮局部视觉改动，未展开无关数据层重构。

## 追加子任务：三个独立二级页

根代理后续授权并发文件归属扩大到 `AdbPairingTutorialScreen.kt`、`ShellTutorialScreen.kt`、`StarterScreen.kt`，已完成第一轮代码迁移。

- 三页在 `LocalIsMiuix.current` 下使用根代理约定的 `Os4Scaffold(title, onBack, closeAction = false, content)`；Material 分支继续保留原 Scaffold/控件。
- 根 scrolling Column 先 `.fillMaxSize().then(contentModifier)`，再 overscroll / nestedScroll / scroll / padding，让正文在 Glass 栏后继续录制；同一个 ScrollBehavior 驱动顶栏与回弹。
- 两份教程的业务动作和原 launcher 仍在原屏幕所有者中，Miuix 内容改用 Miuix Card/Text/TextButton，12dp 外部沟槽/组间距、16dp 卡片内边距和底部留白。
- Starter 保留输出/错误分类/3 秒成功后关闭逻辑。Miuix 终端使用 Miuix 语义色和等宽 body2；错误弹窗放在 Os4Scaffold 内容中以获得 popup host。
- Starter `GlassDialog` 保持组合，`retainedErrorMessageRes` 在退出动画期间保留 summary，`onDismissFinished` 再清理；正常 Material AlertDialog 的显示逻辑保持原路径。
- `Os4Scaffold` 的返回/关闭图标与本地化由根代理负责。
- 官方 pinned `docs/components/basic/button.md`、`card.md` 及 `theme/TextStyles.kt` 已核对对应 API。
- 这三份文件 `git diff --check` 已通过。调用方已写好，等待根代理 helper 落盘后统一编译；尚无编译或真机结论。
- 后续真机待验：教程纵向滚动/顶栏回弹、所有设置与导出动作、Starter 关闭/自动返回/错误弹窗 Back 和外部关闭、窄矮视口与暗色/无 shader 回退。
