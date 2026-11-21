# OS4 settings / Appearance Glass migration review

## Scope and method

Read-only source review of `MiuixSettingsScreen.kt` and `AppearanceScreen.kt`, with only the adjacent Scaffold call sites needed to trace popup-host placement. No source code was edited; no Gradle task, device interaction, network lookup, or fixed-beta02 API/source inspection was performed.

## Findings

### Blocking issues

- **None confirmed from the reviewed source.** The port editor resets its text from the committed `tcpPort` every time the preference is opened; Cancel only dismisses, and no draft is applied from that path. The OK path defers applying the value until the dialog exit finishes.

### Requested checks

1. **Port draft cancel/reopen — source logic appears correct.** In `MiuixSettingsScreen.kt:411-416`, each open calls `setTextAndPlaceCursorAtEnd(...)` with the current committed port (blank for 5555), then shows the dialog. Cancel at `:278-281` only sets `showTcpPortDialog = false`; it does not persist the draft. Thus a subsequent preference open overwrites the retained `TextFieldState` draft from committed state. OK computes the input/default at `:284-297` and schedules its apply closure before dismissing.

2. **`GlassDialog.onDismissFinished` — sequencing is explicit and guarded by nullable pending work.** The port dialog handler at `MiuixSettingsScreen.kt:243-252` takes and clears `pendingTcpPortApply`, then invokes it only if non-null. The OK action installs that closure before setting `showTcpPortDialog = false` (`:284-293`); Cancel does not install one (`:278-281`). This avoids applying an ordinary canceled draft and sequences the follow-up restart prompt after the port dialog's exit callback. No callback-order/runtime behavior was exercised, so the library's exact dismissal guarantees remain unverified.

3. **Root Scaffold host — source wiring is consistent, host resolution itself is not verified.** All settings Glass dialogs opt into `renderInRootScaffold = true` (`MiuixSettingsScreen.kt:185-190, 210-215, 243-248, 303-308`). The main Miuix page has an outer Scaffold at `MiuixMainScreen.kt:170-174`, while its nested Scaffold and per-page wrapper suppress their own popup hosts (`:277-278`, `:539-552`). The Appearance activity uses `Os4Scaffold` (`AppearanceActivity.kt:64-75`); its outer Scaffold encloses the page-local backdrop provider, and the inner page Scaffold passes `popupHost = {}` (`Os4Components.kt:76-87, 114-119`). These call sites are structurally aligned with root-host rendering. Whether the pinned beta02 implementation resolves `renderInRootScaffold` to exactly these outer hosts is **not proven** by the two requested screen files or by this source-only review.

4. **`GlassDropdownPreference` — conditional routing and inputs are coherent.** `AppearanceScreen.kt:240-269` routes Miuix mode to `GlassDropdownPreference` with the page backdrop and `renderInRootScaffold = true` (`:248-258`), and Material mode to `WindowDropdownPreference` (`:259-267`). The Miuix appearance route is hosted by `Os4Scaffold` (`AppearanceActivity.kt:54-75`); the Material route is in the Material Scaffold and keeps the non-Glass dropdown path (`:80-102`). Source-level wiring only; popup anchoring, selection feedback, dismissal, accessibility, and actual rendering were not exercised.

## Evidence gaps / not claimed

- No runtime interaction was performed: cancel/reopen, outside/back dismissal, rapid reopen during exit animation, and competing-dialog transitions are not runtime-verified.
- No fixed `0.9.5-beta02` official docs, matching library source, or resolved-artifact API declarations were inspected here. In particular, the semantics of `onDismissFinished`, `renderInRootScaffold`, and `GlassDropdownPreference` beyond the call-site shape are not independently verified.
- No Gradle compilation or tests were run, per request.
- The TCP-port persistence helper implementation was not part of the reviewed screen files; storage behavior for a blank/default port is therefore not independently verified.

## Independent dialog-Activity review

### Decision: retain `WindowDialog` in this round

The three activities are not ordinary content destinations with a known page Scaffold host. Each is a dedicated, externally launched or task-isolated dialog activity, and each relies on `finish()` / result delivery as part of its contract. Keep the Miuix `WindowDialog` path for this migration round; replacing it with page-hosted Glass would change the host/window and lifecycle boundary before there is evidence that the caller/result behavior remains equivalent.

- **`RequestPermissionActivity.kt`**: `RequestPermissionActivity` extends `AppActivity` and uses the manifest `@style/GrantPermissions` host (`AndroidManifest.xml:122-128`). That style is transparent, floating, dimmed, and dialog-animated (`styles.xml:57-65`). The Miuix permission confirmation uses `WindowDialog(show = true, onDismissRequest = {})` (`RequestPermissionActivity.kt:141-168`), intentionally preventing an incidental dismiss from skipping the permission decision. Its allow/deny callbacks dispatch the remote permission result and immediately `finish()` (`:106-125`). The ADB-limited branch also uses `WindowDialog` and finishes through `onDismiss` (`:206-227`). **Retain Window**: this is an external authorization transaction in a transparent floating Activity, not a child of the app's page Scaffold; a Glass/root-host conversion could alter dimming, outside-dismiss behavior, and when the result Activity finishes.

- **`LegacyIsNotSupportedActivity.kt`**: the Activity is externally exported with the same transparent/floating `GrantPermissions` style (`AndroidManifest.xml:147-158`, `styles.xml:57-65`). Both Miuix branches use `WindowDialog(show = true, ...)` (`LegacyIsNotSupportedActivity.kt:105-139`, `:186-207`). Dismissal sets `RESULT_ERROR` and calls `finish()` (`:65-89`); the V3 action starts `MainActivity` and then dismisses/finishes (`:74-79`, `:127-133`). **Retain Window**: the dialog is the visible surface of an authorization handoff from another app, and the start-another-Activity-then-finish ordering depends on this Activity staying a separate host.

- **`BugReportDialogActivity.kt`**: this Activity is explicitly declared with `@style/Theme.App.DialogHost`, `taskAffinity=""`, `excludeFromRecents="true"`, and `launchMode="singleTask"` (`AndroidManifest.xml:116-121`). The style sets `windowIsTranslucent=true`, transparent background, and no title (`styles.xml:119-123`). The Miuix branch uses `WindowDialog(show = true, onDismissRequest = onDismissAction, ...)` (`BugReportDialogActivity.kt:76-172`), and the dismiss action cancels the worker notification before `finish()` (`:71-74`). It can be launched from the settings screen (`MiuixSettingsScreen.kt:454-458`) or from a notification PendingIntent with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` (`AdbStartWorker.kt:184-190`). **Retain Window**: the transparent, task-isolated dialog Activity is deliberate for both entry paths; moving the content to a page/root Glass host would change task/window ownership and the finish/notification cleanup boundary.

### Follow-up verification required before any Window-to-Glass change

- Verify on a device/emulator that each transparent Activity retains dimming, bounds, back/outside-dismiss behavior, and visual layering when the Miuix `WindowDialog` is shown.
- For `RequestPermissionActivity`, exercise allow, deny, binder-timeout, and back/outside-dismiss paths; verify exactly one remote result dispatch and no premature `finish()`.
- For `LegacyIsNotSupportedActivity`, exercise both metadata branches and the `Open Shizuku` action; verify the caller receives `RESULT_ERROR` after the intended handoff and no duplicate Activity remains.
- For `BugReportDialogActivity`, launch both from Settings and from the notification PendingIntent; verify `singleTask` reuse, notification cancellation on dismiss, and return/task-stack behavior.
- Confirm the resolved beta02 `WindowDialog` implementation and any proposed Glass/root-host alternative on the actual artifact, then compile/runtime-test before changing these hosts. No such runtime or artifact verification was performed in this review.
