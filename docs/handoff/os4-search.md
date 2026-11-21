# OS4 search migration handoff

## Status

用户要求停止子代理后，lead 已直接接管并补齐源码。当前 MainScreen 和 AppManagement 已完成接口对齐，等待统一编译与模拟器验证。下方旧 partial warning 是交接历史，当前以本节和最终验证记录为准。

- 主壳改为每个Tab独立ScrollBehavior；保持原pager和两种导航，宽短窗口固定小标题。
- 通过 pageContent slot 将 GlassSearchOverlay 放在页面Scaffold的上方，仍位于整页backdrop录制之内；弹窗host位于录制外。
- 正常/搜索列表共用 appManagementItems、grantedStates 与 onToggle；移除原假搜索结果列表。授权仅在 onToggle 写入一次，AppItem只调用回调并保留SecurityException提示。
- 授权缓存和lazy key包含包名+uid，避免多用户同包冲突。搜索结果补齐顶部搜索栏、水平缺口、底部安全区的避让和空结果内容。
- 关闭模糊后不录制主壳场景，GlassSearchOverlay跟随有效backdrop开关使用纯色降级；item对话框始终留在组合，采样外层整页（包含搜索层）而非自身。
- 批量授权保留逐项写入；SecurityException显示同样的ADB受限提示。没有业务服务时不声称授权已运行验证。

## Confirmed contracts

- Target is Miuix `0.9.5-beta02`, pinned to `66248bacc7be29ae6bdc0957de9458b42ac67732`.
- Official local references read: `.oil-ui/os4-migration/reference/glasssearchoverlay.md`, `GlassSearchOverlay.kt`, `GlassTopAppBar.kt`, and `glassdialog.md`.
- `GlassSearchOverlay` owns its text field/query and result scroll boundary; its result content receives the active query. Query filtering should use the existing source package list plus existing system-app filter and sort option.
- The screen currently loads packages, tracks `grantedStates`, refreshes permission states on resume/package updates, and applies system filtering, text query, and name/package sorting in `MiuixAppManagementScreen.kt`.
- Existing `MiuixAppItem` directly invokes `AuthorizationManager.grant/revoke` and then invokes `onToggle`; callers also write through `AuthorizationManager`. Make the callback the single write path, but retain its `SecurityException` handling and ADB-limited GlassDialog.
- The nested main shell has an outer `Scaffold` popup host. The inner page wrapper deliberately uses `popupHost = {}`; Glass dialogs use the root host by default (`renderInRootScaffold = true`). `LocalPageBackdrop` currently comes from the page scene and does not contain the search results scene.
- If the ADB-limited dialog is presented from a search result, pass it a clean backdrop that captures the search result scene but excludes the root popup host/dialog. Do not sample the dialog itself.
- Keep the two existing bottom-navigation alternatives and three tabs. Keep effects-disabled/runtime-shader fallback behavior. Do not change shared Material `SearchStatus`/`SuperSearchBar`, and do not edit the separate `MainPagerState` work.

## Suggested integration boundary

Use a `pageContent` slot on `MiuixAppManagementScreen` so the page wrapper remains outside the overlay while the overlay remains above the page content:

```kotlin
pageContent: @Composable (@Composable (PaddingValues) -> Unit) -> Unit = { content ->
    content(scaffoldPadding)
}
```

The screen root should compose `pageContent { padding -> normal app list }` first and `GlassSearchOverlay` as its sibling afterward. The parent can supply the existing `MiuixMainPageWrapper` through this slot. This avoids placing the overlay beneath the wrapper's top app bar and preserves its position within the page backdrop scene.

Render ordinary and search rows through one shared row/list-content helper, consuming the same `grantedStates` and one `(PackageInfo, Boolean) -> Unit` write callback. The search list must not be a detached duplicate with `isGranted=false` or a no-op callback. Sorting and system filtering remain shared with the normal list; search applies only the overlay's current query.

## Current source-edit warning

Before interruption, edits were made in `MiuixMainScreen.kt` and partially in `MiuixAppManagementScreen.kt`. The management screen now has partial `searchExpanded` / `pageContent` changes and a Glass overlay integration attempt, but the shared `appManagementItems` helper and some imports/authorization cleanup were not completed. In particular, re-check compilation-level consistency and the `MiuixAppItem` write path. The parent agent explicitly took over both sources; this handoff is informational, not an instruction to preserve the partial implementation.

## Verification

No Gradle task was run, per request. Parent agent owns final integration and unified verification. This handoff records source/API facts, not compile, rendered, or device verification.
