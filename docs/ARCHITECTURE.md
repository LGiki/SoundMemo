# Architecture

SoundMemo uses a Kotlin-first MVVM structure with a small manual dependency container.

## Layers

- `data/db`: Room database and DAO for recording metadata.
- `data/repository`: recording metadata operations and recycle-bin behavior.
- `data/settings`: DataStore-backed app settings.
- `data/storage`: app-specific file creation and FileProvider sharing.
- `domain/recorder`: recorder state models shared by the foreground service and UI.
- `domain/player`: Media3 ExoPlayer wrapper and playback state.
- `service`: Android services for long-running recording.
- `ui`: Compose screens, theme, and ViewModels.

Recordings are stored in app-specific storage under `files/recordings`. Metadata is stored in Room. Settings are stored in Preferences DataStore.

