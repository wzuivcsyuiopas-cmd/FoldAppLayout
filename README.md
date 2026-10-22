# Fold App Layout

[![Build and test](https://github.com/wzuivcsyuiopas-cmd/FoldAppLayout/actions/workflows/build.yml/badge.svg)](https://github.com/wzuivcsyuiopas-cmd/FoldAppLayout/actions/workflows/build.yml)

**Experimental / 实验版** · [下载 APK](https://github.com/wzuivcsyuiopas-cmd/FoldAppLayout/releases) · [English](README.en.md) · [MIT License](LICENSE)

为折叠屏按应用提前触发大屏 UI。提供应用列表、搜索、独立开关和规则识别状态，支持的布局从 840dp 门槛提前到 600dp，保持实际 DPI、字体缩放和窗口尺寸。

这是 Xposed 模块，需要 Android 14+ 和兼容的 Vector / LSPosed 框架。2.0 版实机验证环境为 MIX Fold 2、LineageOS 24 / Android 17、Vector 2.2；其他环境尚未实机验证。

## 使用

1. 安装 APK，在框架中启用模块。
2. 框架作用域中授权要调整的应用。
3. 打开 Fold App Layout，搜索并勾选这些应用，再重新打开目标应用。
4. 取消勾选恢复原规则。全新安装默认不选择任何应用。

框架作用域决定能否注入，模块开关决定是否调整。此版本不会自动扩大框架作用域。设置只保存在手机本地；应用之间通过带发送者身份的显式广播读取自己的开关，不使用联网服务或公共可读配置文件。

目标应用启动后会报告“已匹配 N 项布局规则”或“未找到可用布局入口”。匹配代表找到了接口，仍需检查实际界面；无法保证每个应用、每个页面都会变成双栏。

## 支持范围

- 识别新版 AndroidX `WindowSizeClass(minWidthDp=…, minHeightDp=…)`，包括保留这一语义标记的 R8 混淆版本。将原本 600–839 档的宽度分类提升为 840，保留高度与折叠姿态。
- 按资源名称调整 `min_window_width_for_expanded_window_size_class` 为 600dp。
- Flare 1.7.8（versionCode 5653）有独立时间线网格适配：最小列宽 320dp → 280dp，使当前内屏竖屏容得下双列。其他 Flare 版本不会套用这条专用规则。
- Mihon 0.21.2（versionCode 38）有独立平板判定适配：读取 Compose 当前窗口的真实配置，将其平板界面入口提前到 600dp。当前宽度和最小宽度均须达到 600dp，因此窄窗口与外屏不会仅因设备型号被强制视为平板。启用该规则时以模块宽度判断为准；不改变 Mihon 自己的设置值。
- 当前验证：Mihon 0.21.2 的内屏首页平板导航；Claude 1.260923.20 使用通用分类识别，Gmail 2026.10.05.998732664.Release 使用资源规则，Flare 1.7.8 使用通用规则与网格适配。

不覆盖任意硬编码判断、旧版 Compose 内联分类、WebView/CSS 断点、服务端开关或根本没有大屏布局的应用。低于 600dp 的标准宽度分类不提升；资源规则也按实际窗口宽度判断。这里的 600dp 是可用窗口宽度标准，不是铰链传感器判断：分屏窗口和其他设备同样按窗口大小生效。

## 构建与测试

需要 JDK 17+、Python 3、curl、Android SDK Platform 36、Build Tools 36.0.0。

```sh
export ANDROID_SDK_ROOT=/path/to/Android/sdk
./build.sh
./test.sh
```

输出 `build/FoldAppLayout.apk`。构建会下载并核对 Xposed API 82 的 SHA-256；默认在忽略的 `build/` 中生成本地签名密钥。正式维护时可通过 `SIGNING_KEY` 和 `SIGNING_PASSWORD` 指定自己的密钥；升级必须使用同一签名。请勿发布密钥。

测试使用自写、不同包名的 DEX 样本检验语义扫描、工厂/包装构造器识别，以及 393、599、600、697、839、840 等宽度边界和高度保留。它不替代框架注入、折叠切换和应用真实界面验证。

## 代码结构

- `MainActivity`：应用选择界面。
- `ConfigClient` / `ConfigReceiver`：经过发送者 UID 校验的配置通信。
- `Hook`：每个目标进程的启用状态和重新加载。
- `DexLayoutScanner`：只在目标应用本机扫描 APK 中的布局类标记；不修改 APK。
- `HookEngine`：通用宽度分类、资源规则及带版本保护的专用适配。

源码采用 [MIT 许可证](LICENSE)。不包含目标应用 APK、反编译代码、手机备份或签名私钥。依赖说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)，贡献规范见 [CONTRIBUTING.md](CONTRIBUTING.md)，隐私与已知限制见 [SECURITY.md](SECURITY.md)。

保留 `local.codex.applayout` 包名以兼容现有安装；本项目不是 OpenAI / Codex 官方产品。当前采用直接调用 Android 官方构建工具的轻量脚本，暂无 Gradle 工程；Android Studio 用户可导入源码阅读，通过脚本构建。

GitHub Actions 的 APK 使用临时测试签名，不能覆盖安装正式 Release。正式 APK 保持同一发布签名，私钥不进入仓库或 CI。启动时需要读取配置并扫描布局类，冷启动可能有额外耗时。2.0.2 已在当前手机验证配置读取及 Mihon 内屏导航布局，其他页面与阅读器双页能力不作保证，详见 [VALIDATION.md](VALIDATION.md)。
