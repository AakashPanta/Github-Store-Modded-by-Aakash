# ReleaseHub MVP

ReleaseHub is an Android-first GitHub release browser and installer built with Kotlin, Compose Multiplatform, Coroutines, and Gradle.

## What this MVP includes

- Browse featured Android-friendly open-source repositories
- Search GitHub repositories in real time
- Open a repository and inspect its releases
- Filter release assets down to installable APK files
- One-click download + install for APK assets
- Local cache for downloaded APKs
- Library tab to revisit cached artifacts

## Architecture

This project mirrors a thin SDK + local store design:

- `core-model`
  - Shared domain models used by all modules
- `sdk-github`
  - Thin GitHub SDK for repository search and release discovery
- `local-store`
  - Android cache + installer implementation for downloaded APK files
- `composeApp`
  - Shared UI, presentation logic, and orchestration
- `androidApp`
  - Android entry point, permissions, manifest, and dependency wiring

## Key product decisions

- Android-first installation flow for the MVP
- Shared Compose UI so the app can expand later without rewriting the presentation layer
- Public GitHub API integration without user auth in the MVP
- Clear separation between browsing, network integration, and file installation concerns

## Run

1. Open the project in Android Studio or IntelliJ IDEA with Android support.
2. Let Gradle sync.
3. Run the `androidApp` configuration on an Android device or emulator.
4. Search for a repository, open its releases, and tap **Install APK**.

## Permissions and behavior

- Uses `REQUEST_INSTALL_PACKAGES` for sideloading flows
- Uses a `FileProvider` so downloaded APKs can be handed to Android's package installer
- Downloaded APKs are stored in app-private storage and indexed in a lightweight JSON cache

## Suggested beta checklist

- Verify download/install flow on Android 12, 13, 14, and 15
- Test rate-limit behavior without authentication
- Validate release parsing against projects with multiple assets
- Confirm cached APK reinstall flow after app relaunch
- Collect UX feedback on the browse/search/detail screens before adding more features

## Deferred features

- Authentication
- Ratings/reviews
- Social sharing
- Background download workers
- Update tracking
- Multi-platform installation flows
