# AGENTS.md

## Overview

Shizuku is a fork of [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) — an Android app enabling system-level API access via ADB/root. Written in Kotlin/Java with Groovy Gradle build scripts.

## Build Commands

```bash
# Clone (submodules required)
git clone --recurse-submodules <repo-url>

# Debug build (produces debuggable server — attachable debugger)
./gradlew :manager:assembleDebug

# Release build
./gradlew :manager:assembleRelease

# Beta variant (versionName includes "-beta")
./gradlew :manager:assembleRelease -Pbeta

# Update submodule after clone
git submodule update --init --recursive
```

`:shell:assembleDebug` / `:shell:assembleRelease` runs automatically as a dependency of the manager build — do not invoke it separately.

## Module Structure

| Module | Type | Namespace | Purpose |
|---|---|---|---|
| `:manager` | application | `moe.shizuku.manager` | Main Android app |
| `:server` | library | `moe.shizuku.server` | Server component |
| `:starter` | library | `rikka.shizuku.starter` | Starter component |
| `:shell` | application | `rikka.shizuku.shell` | rish shell; output DEX is copied into `manager/src/main/assets/` |
| `:common` | library | `rikka.shizuku.common` | Shared utilities |
| `api/*` | git submodule | — | `:aidl`, `:rish`, `:shared`, `:api`, `:provider`, `:server-shared` from [Shizuku-API](https://github.com/thedjchi/Shizuku-API) |

## Key Build Facts

- **JDK 21** required; `compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`, `ndkVersion = "29.0.13113456"`.
- **Version** is derived from `git rev-list --count HEAD` (versionCode) and hardcoded base `13.6.0` (versionName). Shallow clones break this.
- **Signing**: reads `signing.properties` in project root (keys: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD`). Missing file falls back to debug keystore — release builds without secrets fail in CI.
- **`:shell` output** is copied as `rish_shizuku.dex` into `manager/src/main/assets/` during assembly.
- **`:manager` release** uses `minifyEnabled = true`, `shrinkResources = true`, and collapses resource names via aapt2.
- `CompileArtProfileTask` is disabled project-wide in `:manager`.
- `androidx.appcompat` and `androidx.profileinstaller` are excluded from `:manager` via configuration excludes.
- `android.nonTransitiveRClass=false` and `android.nonFinalResIds=false` in `gradle.properties`.

## Tools & Libraries

- **Rikka tools**: `refine` (hidden API access), `autoresconfig` (locale config generation), `materialthemebuilder` (theme generation). All at version 4.4.0 / 1.2.2 / 1.5.1.
- **Hidden API**: `dev.rikka.hidden:compat` + `stub` 4.4.0; `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`.
- **libsu** (root access): `com.github.topjohnwu.libsu:core:6.0.0`.
- **Coroutines**: `kotlinx-coroutines-core` + `android` 1.10.2.
- **Material**: `com.google.android.material:material:1.14.0-alpha08` (M3 Expressive).

## Local API Override

To use a local Shizuku-API checkout instead of the submodule, set in `local.properties`:
```
api.useLocal=true
api.dir=../Shizuku-API
```

## Output & Artifacts

- Build output (APKs, mapping files) is copied to `out/` directory.
- APK naming: `shizuku-v<versionName>-<buildType>.apk`.

## No Lint/Typecheck/Test Commands Configured

There are no dedicated lint, typecheck, or test Gradle tasks configured in this repo. `lint.checkReleaseBuilds = false` is set for both `:manager` and `:shell`. The CI workflow (`app.yml`) only runs assemble — no test or lint steps.

## CI

GitHub Actions workflow `app.yml` is manual (`workflow_dispatch`). Builds debug or release, optionally with `-Pbeta`. Release builds create a draft GitHub release with the APK and a git tag.
