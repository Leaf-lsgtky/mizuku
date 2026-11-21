# OS4 设置与外观迁移交接

更新日期：2026-11-21。工作边界是设置页与共用外观页；父代理负责依赖、主题、Activity 与采样宿主。

## 已完成（待集成编译）

- `MiuixSettingsScreen.kt`：四个 `WindowDialog` 改为 `GlassDialog`，始终保持组合直至退场完成，使用 `LocalPageBackdrop.current`；按钮改为 `GlassDialogButton`，确认采用 `Primary`。
- TCP 端口编辑改为 `rememberTextFieldState`、`GlassTextField`、数字键盘、单行输入和文件级 `InputTransformation`，拒绝非 ASCII 数字或超过 5 位的编辑；保留空输入恢复默认端口和 `1..65535` 验证。每次打开从保存值重设草稿，取消不保存。
- TCP 确认通过 `onDismissFinished` 先完成端口对话框退场，再进入可能的重启确认，避免两个 Glass surface 重叠。
- `AppearanceScreen.kt`：提供 `modifier: Modifier = Modifier` 参数作用于根列表，由 Activity 注入正文采样；Miuix 模式使用 `GlassDropdownPreference`，Material 模式使用无需 Miuix popup host 的 `WindowDropdownPreference`。此页原本在两种 Activity 外壳下都使用 Miuix 内容，未将 Material 外壳改成 Glass。
- 外观页条件动画升级为 `CapturedAnimatedVisibility`，两个列表的 overscroll offset 同步到原有 `scrollBehavior.state.overscrollOffset`；去除 SmallTitle 前多余的 12dp 首部空白。
- `Preferences.kt` 核对后确认全是 Material 专用 helper，保持原样。

## 已确认的集成约定

- 所有 Glass 浮层显式 `renderInRootScaffold = true`。父代理计划 outer Scaffold 承载 popup host，inner Scaffold 的 `popupHost = {}` 用于排除整页录制中的 popup/scrim；不能选择 inner host。
- 父代理提供 `compose/components/miuix/Os4Components.kt` 的 `LocalPageBackdrop`。
- 外观页实际参数为 `AppearanceScreen(scrollBehavior: ScrollBehavior, scaffoldPadding: PaddingValues = PaddingValues(0.dp), modifier: Modifier = Modifier)`。
- 未新增/变更 string、偏好键、选项序号、服务启动权限流程或持久化模型。

## API 证据

已读取 `miuix-components`、`miuix-guidelines` 技能及相关规则，参考用户提供的 Mishka 文档，并读取官方仓库 pinned commit `66248bacc7be29ae6bdc0957de9458b42ac67732` 的：

- `docs/components/glass/glassdialog.md` 和 `GlassDialog.kt`：Scaffold host、持续组合、root host、onDismissFinished、按钮 role。
- `docs/components/glass/glassdropdownpreference.md` 和 `GlassDropdownPreference.kt`：可空 Backdrop、root host、disabled 支持。
- `docs/components/glass/glasstextfield.md` 和 `GlassTextField.kt`：状态式输入 overload，lineLimits、inputTransformation、keyboardOptions。
- `docs/components/extended/windowdropdownpreference.md`：Material 外壳无 MiuixPopupHost 时的普通 Window fallback。

## 待验证

- 已运行本子任务涉及文件的 `git diff --check`，无空白错误；已逐段回读主要改动。
- 由父代理统一更新至 Miuix `0.9.5-beta02` 后运行 `:manager:compileDebugKotlin`；目前没有完成编译或真机验证。
- 真机检查 TCP 编辑首次打开/取消重开、空值/0/65535/65536、IME、返回键、确认后重启对话框交接。
- 检查 Material/Miuix 外观页下拉菜单可见、风格切换后 host 生命周期、深浅主题与关闭模糊的可读 fallback。
