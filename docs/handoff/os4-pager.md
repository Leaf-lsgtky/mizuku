# OS4 Pager 导航锁修复交接

## 已落盘修改

目标文件：`manager/src/main/java/moe/shizuku/manager/compose/MainPagerState.kt`

- 新增 `navGeneration` 请求代次。每次接受新的目标页导航时先递增代次，再取消旧 `navJob`。
- 将滚动包在 `try/finally` 中；正常结束或取消后，只有代次仍属于当前请求的任务才能清除 `isNavigating` 并清空 `navJob`。旧任务即使在取消时进入 `finally`，也不会释放新请求的导航锁。
- `selectedPage` 更新及滚动目标逻辑保持原样。动画仍为 `EaseInOut` tween，持续时间公式未改：`100 * distance + 100`，相邻页距离按 `coerceAtLeast(2)` 计算，因此保持 300ms 平滑滚动。

## 核对记录

- 已先读取修改前的 `MainPagerState.kt`，并在修改后重新读取确认代次检查位于 `finally`。
- 未改动其他源码文件；新增本交接文档。
- 按要求未运行 Gradle，故没有声称编译或自动化测试通过。
