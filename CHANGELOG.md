# Changelog

## 2.0.3 — 2026-10-22 (experimental)

- Adds a version-guarded native watch-scene/two-column adapter for YouTube 21.42.243 (1561304941).
- Enables video-left / recommendations-or-comments-right on the inner portrait screen at real 600dp+ widths; keeps actual density, rotation and window measurements unchanged.
- Retains the original scene exclusion and custom player-height conditions. Recommendation headers may contain duplicate channel metadata; this remains experimental.
- Does not include the unsuccessful global size-bucket/aspect-classification experiments or diagnostic hooks.

## 2.0.2 — 2026-10-22 (experimental)

- Adds a version-guarded adapter for Mihon 0.21.2 (38), whose custom/inlined tablet UI checks are outside the generic WindowSizeClass scanner.
- Reads its real Compose window configuration and lowers the tablet predicate to 600dp without modifying display metrics or app preferences.
- Verified the inner portrait home screen changes from bottom navigation to a tablet navigation rail at the original 440 DPI. Full reader/page compatibility is not claimed.

## 2.0.1 — 2026-10-22 (experimental)

- Initial public source release with a searchable per-app switch UI.
- Retains 600dp adaptive-width rules, semantic WindowSizeClass discovery and the version-guarded Flare grid adapter from 2.0.
- Restricts reload broadcasts to the module UID and rejects unknown/unordered configuration requests.
- Adds pinned CI actions, build/host tests, contribution guidance, privacy notes and MIT licensing.
- Build and host test validation only for this revision; 2.0 was tested on the maintainer's MIX Fold 2. No new on-device validation is claimed.
