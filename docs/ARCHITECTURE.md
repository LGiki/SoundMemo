# Architecture

SoundMemo uses a Kotlin-first MVVM structure with a small manual dependency container.

## Layers

- `data/db`: Room database and DAO for recording metadata.
- `data/repository`: recording metadata operations and recycle-bin behavior.
- `data/settings`: DataStore-backed app settings.
- `data/storage`: staged recording files, app-file publishing, MediaStore publishing, and share/delete URI resolution.
- `domain/recorder`: recorder state models shared by the foreground service and UI.
- `domain/player`: Media3 ExoPlayer wrapper and playback state.
- `service`: Android services for long-running recording.
- `ui`: Compose screens, theme, and ViewModels.

Recordings are first captured to an app cache staging file, then published to the user-selected save location. App files are stored in app-specific external Music storage under `Music/recordings`, falling back to internal `files/recordings` if external storage is unavailable. Device storage publishes to public `Music/SoundMemo` through MediaStore on Android 10 and later, or a public Music file on older devices with storage permission. Custom folders use Android's system folder picker, persist write access to the selected tree URI, and store each recording's document URI in metadata. Metadata is stored in Room. Settings are stored in Preferences DataStore.

Standard RIFF/WAV output rotates at the format's size ceiling without splitting a PCM frame. Each completed WAV part is published and inserted as an independent library recording in one Room batch. Cache files left by process interruption are never deleted automatically; once no recording workflow is active, the app offers the user a delete-or-keep recovery prompt.
