# Fold App Layout

An experimental Xposed module that lets selected apps enter their existing large-screen layouts at a 600dp window width instead of 840dp, without changing actual DPI, font scaling or window dimensions.

[中文说明](README.md) · [Downloads](https://github.com/wzuivcsyuiopas-cmd/FoldAppLayout/releases) · [MIT License](LICENSE)

## Requirements and use

Android 14+, with a compatible Vector / LSPosed legacy Xposed API framework. Version 2.0 was tested on MIX Fold 2, LineageOS 24 / Android 17 and Vector 2.2. Other combinations are unverified. Version 2.0.2 also validates configuration delivery and the Mihon inner-screen navigation adapter on this device.

1. Install the release APK and enable the module in your framework.
2. Authorize your target apps in framework scope.
3. Open Fold App Layout, search/select apps, then restart them.
4. Deselect an app and restart it to restore its normal rules.

Fresh installs select nothing. Framework scope authorizes injection; the module switch controls its rules. This module does not automatically expand framework scope. A matched-rule count is diagnostic, not proof that every page supports two panes.

## What it supports

- Semantic discovery of newer AndroidX WindowSizeClass, including R8-renamed classes that retain the expected toString marker. It promotes width classes in the 600–839 range to expanded (840), preserving height and posture.
- Named dimension resources: min_window_width_for_expanded_window_size_class → 600dp.
- A version-guarded YouTube 21.42.243 (1561304941) watch adapter relaxes its native scene/split-layout portrait restriction on wide windows. The video stays on the left and recommendations/comments can appear on the right, without rotating or resizing the actual window. This is app-specific behavior, not a generic 840dp override. The recommendation pane may retain duplicate channel metadata.
- A version-guarded Mihon 0.21.2 (38) adapter reads the current Compose window configuration and enables its tablet UI predicate when both current and smallest widths reach 600dp. While enabled, the module width rule takes priority without changing Mihon preferences. It does not promise reader dual-page mode or all custom layouts.
- A version-guarded Flare 1.7.8 (5653) grid adapter reduces minimum column width from 320dp to 280dp.

An app must already have a large-screen UI. Arbitrary custom breakpoints, old inlined Compose classifiers, WebViews, server-side switches and dynamically loaded code are not universally covered. This is window-width based, not hinge detection: narrow split-screen windows retain their normal standard width class. Flare has a separate grid adapter.

## Build

Install JDK 17+, Python 3, curl, Android SDK Platform 36 and Build Tools 36.0.0. Set ANDROID_SDK_ROOT, then run:

```sh
./build.sh
./test.sh
```

Output: build/FoldAppLayout.apk. These scripts directly use official Android SDK tools; no Gradle project is included. The Xposed API jar is compile-only and hash-checked. Default builds generate a local test key in ignored build/. Set SIGNING_KEY and SIGNING_PASSWORD to use your own persistent key. CI uses disposable keys and cannot update official release builds.

Tests cover semantic DEX discovery and width/height boundaries, not real framework injection. See [VALIDATION.md](VALIDATION.md) for evidence and limits, [SECURITY.md](SECURITY.md) for privacy and startup overhead, and [CONTRIBUTING.md](CONTRIBUTING.md) for contributions.

No target APKs, decompiled app code, device backups or private signing keys are distributed. See [third-party notices](THIRD_PARTY_NOTICES.md). The existing package ID local.codex.applayout is retained for update compatibility; this is not an official OpenAI / Codex product.
