# Birdy for Android

Birdy is a small Android app whose main feature is a responsive Jetpack Glance
home-screen widget. It consumes the public `Blockabsbebsh/birdy-feed` feed; no
Nuthatch key or other credential is present in the app.

## Automatic behavior

- Fetches immediately when the app opens or the first widget is added.
- Checks the feed hourly through WorkManager, downloading images only when the
  feed's `generatedAt` changes.
- Downloads all five birds and all widget sizes before activating a new feed.
- Keeps the previous complete feed if a download fails.
- Schedules local rotation at the next `rotationMinutes` boundary, so rotation
  works offline and does not require an image download.
- Uses small, medium, or large backend images according to the widget shape.
- Uses the prepared rectangular crop for wide widgets and the prepared square
  crop for square widgets. Unusually tall Android widgets use `Fit` so Android
  does not crop the bird a second time; empty space is the unavoidable fallback.
- Tapping the bird name opens Wikipedia. Manual refresh is available in the app;
  the widget's empty/loading state can also be tapped to retry its first sync.

The app's **Check GitHub feed for updates** button only performs an unauthenticated
GET of the public `latest.json`. It cannot start a GitHub Actions workflow. When
`generatedAt` has not changed, the existing cached feed intentionally remains
visible.

## Build

Use Android Studio 2026.1.3 or run `./gradlew assembleDebug` with Android SDK 36.
The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install

Download the release APK from this repository's **Releases** page, copy it to
the Android phone, and allow installation from that source if Android asks.
Launch Birdy once and tap **Add Birdy to home screen**, or long-press the home
screen and select **Widgets → Birdy**. The APK contains no API key and only
requests network access.

Release APKs are signed with an owner-held key that is intentionally excluded
from this public repository. Local `assembleDebug` builds use Android's normal
debug signing and cannot be installed as an update over a release build.
