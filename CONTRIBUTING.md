# Contributing

Small, focused changes are welcome. Include the device, Android/framework versions, target app version, expected layout and actual result when reporting compatibility problems.

- Run `./build.sh` and `./test.sh` before submitting a PR.
- For hook changes, also test actual UI on a scoped target app: enabled/disabled, outer/inner display, portrait/landscape and narrow multi-window. Host tests do not prove framework compatibility.
- Keep actual display density, font scale and window metrics unchanged.
- Scope injection to user-authorized apps; never silently add system processes or broaden framework scope.
- Identify generic hooks semantically; guard app-specific adapters by version. Do not publish third-party APKs or decompiled app source in contributions.
- Keep keys, tokens, device backups and private UI/log content out of commits and issues.
- Contributions use the project's MIT license. Use your own fixtures and record external dependencies in THIRD_PARTY_NOTICES.md.

Builds from GitHub Actions use a disposable local test key. They cannot update the official release APK. No release signing key is stored in the repository or CI.
