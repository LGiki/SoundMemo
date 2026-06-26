# Repository Guidelines

## Project Structure & Module Organization

SoundMemo is a single-module native Android app. Application code lives in `app/src/main/java/net/lgiki/soundmemo`. Keep UI in `ui/` by feature (`recorder`, `library`, `player`, `settings`), domain models and controllers in `domain/`, persistence in `data/`, foreground audio work in `service/`, and shared helpers in `util/`. Android resources live in `app/src/main/res`, including localized strings in `values-zh-rCN` and `values-zh-rTW`. Unit tests belong in `app/src/test`, and device or emulator tests belong in `app/src/androidTest`. Architecture notes are in `docs/ARCHITECTURE.md`.

## Build, Test, and Development Commands

- `./gradlew :app:assembleDebug` builds a debug APK and is the minimum pre-submit check.
- `./gradlew :app:testDebugUnitTest` runs local JVM tests under `app/src/test`.
- `./gradlew :app:connectedDebugAndroidTest` runs instrumentation and Compose UI tests on a connected device or emulator.
- `./gradlew :app:check` runs the available verification tasks for the app module.

Open the project in Android Studio for emulator workflows, previews, and SDK management.

## Coding Style & Naming Conventions

Use Kotlin with Java 11 bytecode, Jetpack Compose for UI, Material 3 components, Coroutines/Flow for async state, Room for recording metadata, and DataStore for settings. Follow Android Kotlin style: 4-space indentation, `PascalCase` types and composables, `camelCase` functions and properties, and `UPPER_SNAKE_CASE` constants only when idiomatic. Name Compose screens `FeatureScreen.kt`, view models `FeatureViewModel.kt`, and repositories `ThingRepository.kt`. Keep user-facing text in string resources, updating all supported locales when practical.

## Implementation Notes

The app targets Android API 26+ and currently uses a small manual dependency container in `SoundMemoContainer` exposed by `SoundMemoApplication`; extend that wiring before introducing a dependency-injection framework. Recordings are staged in app cache, then published to the selected save location: app-specific files under `Music/recordings` or public device storage under `Music/SoundMemo` via MediaStore where available. Sharing and deletion must go through `RecordingStorage` so both file paths and content URIs work. Recording metadata lives in the Room `recordings` table, so schema changes require explicit migrations in `SoundMemoDatabase`. Settings use the `soundmemo_settings` Preferences DataStore. When adding location, export, sharing, or storage behavior, preserve the local-first model and keep metadata writes opt-in where existing settings make them opt-in.

## Testing Guidelines

Use JUnit for local tests and AndroidX test libraries for instrumentation. Add focused unit tests for formatting, repositories, state holders, and view-model logic. Use instrumentation tests for platform behavior such as Room integration, foreground-service flows, and Compose interactions. Name tests after the behavior under test, for example `recordingState_startsPausedFalse`.

## Commit & Pull Request Guidelines

The existing history uses Conventional Commits such as `feat: initial SoundMemo Android app` and `feat(i18n): add internationalization with Chinese language support`. Continue with concise prefixes like `feat`, `fix`, `docs`, `test`, or `refactor`, adding a scope when useful. Pull requests should be small and focused, describe behavior changes, list commands run, link related issues, and include screenshots or screen recordings for visible UI changes.

## Privacy & Configuration

Preserve the app’s local-first privacy model: no accounts, ads, analytics, or cloud sync unless explicitly designed and documented. Do not commit `local.properties`, keystores, generated APKs, or machine-specific SDK paths.
