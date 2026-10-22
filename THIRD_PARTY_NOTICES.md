# Third-party dependencies

- Xposed API 82 (`de.robv.android.xposed:api:82`), by rovo89. Compile-only API stubs downloaded from https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar; its Maven POM declares Apache License 2.0: https://api.xposed.info/de/robv/android/xposed/api/82/api-82.pom. SHA-256 is pinned in build.sh. Stub classes are not packaged in the APK; the user's framework provides the runtime API.
- Android SDK Platform 36 and Build Tools 36.0.0 are external build tools, installed separately under their own terms.
- AndroidX WindowSizeClass names and semantics are used for interoperability. No AndroidX runtime library or target application code is bundled.

The repository contains original module source and self-authored test fixtures. It does not redistribute Gmail, Claude or Flare APKs/source. Product names belong to their respective owners; this project is not affiliated with them, Google, Anthropic, LineageOS or Xposed framework maintainers.
