# SoundMemo

SoundMemo is a native Android voice recorder focused on fast recording, clean playback, local file management, and a privacy-respecting default experience.

## Features

- Record M4A/AAC audio locally.
- Pause, resume, stop, and discard active recordings.
- Keep recording active through a foreground service notification.
- Browse saved recordings with search and sorting.
- Rename, share, delete, restore, and permanently remove recordings.
- Play recordings with seek, skip, and speed controls.
- Configure theme, dynamic color, recording bitrate, playback speed, and recycle-bin retention.

## Build

```bash
./gradlew :app:assembleDebug
```

The app targets the latest configured Android SDK and supports Android 8.0/API 26 and later.

## Privacy

SoundMemo has no account system, ads, analytics, or cloud sync. Recordings are stored in app-specific local storage unless the user explicitly shares them.

