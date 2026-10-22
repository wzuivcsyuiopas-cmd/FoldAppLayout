# Security and privacy

This experimental module runs inside apps that the user authorizes through their Xposed framework. Such access is powerful: install only trusted builds and select the narrowest needed scope.

The module itself has no INTERNET permission, analytics or remote configuration. QUERY_ALL_PACKAGES is used to list installed launchable apps and validate requesting package/UID ownership. Preferences are private and backup is disabled. The module does not read messages, account credentials or app databases.

Configuration requests use an explicit ordered broadcast. The receiving module validates the action and the OS-provided sender UID before returning that sender's switch. Reload broadcasts share sender identity and the injected receiver checks the module UID. This is why Android 14+ is required. Isolated processes without the app's UID are not supported.

The hook engine scans the scoped app's own APK locally. Obfuscated, optimized, packed or dynamically loaded implementations may not be supported. Initial configuration is synchronous before app initialization (up to a 3-second response wait), followed by a DEX scan; a cold start may be slower. A timeout disables changes for that process. Restart the target app after fixing configuration access.

Do not post credentials, private conversations, full app data or device backups in public issues. For sensitive reports use GitHub's private vulnerability reporting when available; public compatibility reports should contain only redacted diagnostic details.
