# Unsupported Play Store locales

Locales kept here are translated in-app (`res/values-*`) but **not supported
as Google Play store-listing languages**, so they live outside
`fastlane/metadata/android` to avoid `fastlane supply` failing with
`Invalid request` on every release.

- `bs` (Bosnian) — not in Play's supported listing-language list.

If Play ever adds one of these, move its dir back under
`fastlane/metadata/android/<play-code>`.
