# SoundMemo Product Document

## 1. Product Overview

**SoundMemo** is an open source Android voice recorder app focused on fast recording, clean playback, simple file management, and a native Android user experience.

The app is designed for everyday audio capture, including meetings, lectures, interviews, personal notes, practice sessions, reminders, and field recordings. SoundMemo should feel lightweight, reliable, and familiar to Android users by using Android native components instead of custom-heavy UI frameworks.

## 2. Product Goals

SoundMemo aims to provide a recorder that is:

- **Fast**: users can start recording quickly after opening the app.
- **Reliable**: recordings should be safely saved even when the app is interrupted.
- **Simple**: the main user journey should be clear: record, play, manage, and share.
- **Native**: the interface should follow Android design patterns and use Android native components.
- **Open source friendly**: the codebase should be easy to understand, build, contribute to, and customize.
- **Privacy-respecting**: recordings stay on the device unless the user explicitly shares or exports them.

## 3. Target Users

SoundMemo is intended for:

- Students recording lectures or study notes.
- Office workers recording meetings and discussions.
- Journalists or creators recording interviews.
- Musicians recording quick practice ideas.
- General users creating voice memos, reminders, and personal notes.
- Developers looking for a clean open source Android recorder implementation.

## 4. Design Direction

The reference image shows a simple recorder app with three main areas: recording, playback list, and settings. SoundMemo can take inspiration from this structure while modernizing the experience with native Android patterns.

The visual direction should be:

- Clean and minimal.
- Light-first, with dark mode support.
- Soft rounded surfaces.
- Clear recording and playback states.
- Large, easy-to-tap primary actions.
- Simple bottom navigation for major sections.

Recommended primary sections:

1. **Recorder**
2. **Library**
3. **Settings**

## 5. Platform and Technical Direction

### 5.1 Platform

- Android native app.
- Minimum Android version: Android 8.0 / API 26 or later.
- Target latest stable Android SDK.
- Kotlin-first development.

### 5.2 UI Technology

Preferred implementation:

- **Jetpack Compose** for modern native UI.
- **Material 3** components.
- Android system navigation, permissions, storage, notifications, and media APIs.

Alternative implementation if the project wants a more traditional structure:

- XML layouts.
- Material Components for Android.
- ViewModel + LiveData / StateFlow.

The recommended choice is **Jetpack Compose + Material 3** because it is now the modern native Android UI toolkit and is suitable for an open source project.

### 5.3 Architecture

Recommended architecture:

- Kotlin
- MVVM
- Repository pattern
- Room database for recording metadata
- DataStore for settings
- MediaRecorder or AudioRecord for recording
- Media3 / ExoPlayer for playback
- Foreground service for active recording
- WorkManager for optional background cleanup or indexing tasks

Suggested package structure:

```text
app/
  data/
    db/
    model/
    repository/
    storage/
  domain/
    recorder/
    player/
    settings/
  service/
    RecordingService.kt
    PlaybackService.kt
  ui/
    recorder/
    library/
    player/
    settings/
    components/
  util/
```

## 6. Core Features

## 6.1 Recorder

The Recorder screen is the primary entry point of the app.

### User needs

Users should be able to start recording quickly, clearly see that recording is active, pause or resume when needed, and stop safely.

### Functional requirements

- Start recording.
- Pause recording.
- Resume recording.
- Stop and save recording.
- Cancel and discard current recording.
- Show recording duration in real time.
- Show simple waveform or amplitude visualization.
- Save recording automatically when stopped.
- Keep recording active when the screen is off.
- Show persistent recording notification while recording.
- Handle interruptions such as incoming calls, microphone conflicts, low storage, or app backgrounding.

### UI requirements

The Recorder screen should include:

- Top app bar with title or search shortcut.
- Large waveform card.
- Recording timer.
- Main circular record / pause button.
- Stop button when recording is active.
- Optional quick note field after recording is saved.
- Bottom navigation.

### Recording states

```text
Idle
  -> Recording
  -> Paused
  -> Saving
  -> Saved
  -> Error
```

## 6.2 Recording Library

The Library screen lists saved recordings.

### User needs

Users should be able to find, play, rename, share, delete, and organize recordings.

### Functional requirements

- Display all saved recordings.
- Show recording title, date, duration, file size, and format.
- Search recordings by title.
- Sort by newest, oldest, longest, shortest, or name.
- Rename recording.
- Delete recording.
- Share recording.
- Open file location where supported.
- Multi-select recordings for batch actions.
- Move deleted files to Recycle Bin if enabled.

### UI requirements

Each recording item should show:

- Recording name.
- Date and time.
- Duration.
- File size.
- More menu.
- Optional small playback button.

## 6.3 Player

The Player screen or bottom mini-player allows users to play recordings.

### Functional requirements

- Play / pause recording.
- Seek forward and backward.
- Drag progress slider.
- Show current playback time and total duration.
- Playback speed control, for example 0.5x, 1x, 1.5x, 2x.
- Skip silence, optional future feature.
- Continue playback in background.
- Show playback notification with controls.

### UI requirements

The player should include:

- Recording title.
- Progress bar.
- Current time and duration.
- Play / pause button.
- Skip backward and skip forward buttons.
- Optional waveform timeline in later versions.

## 6.4 Recycle Bin

The Recycle Bin provides a safety layer before permanent deletion.

### Functional requirements

- Deleted recordings are moved to Recycle Bin.
- Restore recording.
- Permanently delete recording.
- Empty Recycle Bin.
- Auto-delete items after a configurable number of days.

This feature can be included in v1.0 if implementation cost is acceptable, or moved to v1.1.

## 6.5 Settings

The Settings screen allows users to customize recording behavior and app appearance.

### Functional requirements

Settings should include:

- App theme: system, light, dark.
- Dynamic color support on Android 12+.
- Recording format: m4a, aac, wav, or opus depending on implementation.
- Audio quality / bitrate.
- Sample rate.
- Audio source: microphone, camcorder, voice recognition, voice communication where supported.
- Save location.
- File naming pattern.
- Start recording automatically after app launch.
- Keep screen awake while recording.
- Show or hide recording notification where Android allows.
- Recycle Bin retention period.
- About SoundMemo.
- Open source licenses.

## 7. Recording Format Strategy

Recommended default:

- Container: M4A
- Codec: AAC
- Bitrate: 128 kbps or 192 kbps
- Sample rate: 44.1 kHz

Reasoning:

- Good compatibility across Android devices.
- Good balance between quality and file size.
- Easy to share.

Optional advanced formats:

- WAV for lossless recording.
- Opus for efficient speech recording.

For v1.0, M4A/AAC is enough. WAV and Opus can be added later.

## 8. Permissions

SoundMemo should request only necessary permissions.

Required permissions:

- `RECORD_AUDIO`
- `POST_NOTIFICATIONS` on Android 13+ for recording and playback notifications

Optional permissions depending on implementation:

- Storage access through Android system picker or MediaStore.
- No broad storage permission should be required for modern Android versions.

Permission UX requirements:

- Explain why microphone permission is needed before requesting it.
- If permission is denied, show a clear recovery path.
- Do not request unnecessary permissions at first launch.

## 9. Storage and File Management

Recommended approach:

- Store recordings in app-specific storage by default.
- Allow export/share through Android Sharesheet.
- Optionally allow users to choose a public folder using the Storage Access Framework.
- Store metadata in Room database.

Recording metadata should include:

```text
id
name
filePath or uri
duration
fileSize
format
bitrate
sampleRate
createdAt
updatedAt
isDeleted
deletedAt
note
```

## 10. Notifications

### Recording notification

When recording is active, SoundMemo should show a foreground service notification.

Notification actions:

- Pause / resume.
- Stop.

### Playback notification

When audio is playing in the background, SoundMemo should show a media notification.

Notification actions:

- Play / pause.
- Skip backward.
- Skip forward.
- Stop.

## 11. Accessibility

SoundMemo should be usable by people relying on accessibility tools.

Requirements:

- All buttons must have meaningful content descriptions.
- Color should not be the only indicator of recording state.
- Text should scale with system font size.
- Touch targets should be at least 48dp.
- Important recording states should be announced to screen readers.
- The waveform should be decorative unless it provides meaningful interaction.

## 12. Privacy and Security

SoundMemo should be privacy-first.

Requirements:

- No account required.
- No cloud sync in v1.0.
- No analytics by default.
- No ads.
- No upload of recordings unless the user explicitly shares them.
- Clear privacy statement in the repository.

Optional future features:

- Local-only transcription.
- User-controlled cloud backup.
- App lock.
- Encrypted recordings.

## 13. Open Source Requirements

The project should be contributor-friendly.

Repository should include:

- `README.md`
- `PRODUCT.md`
- `CONTRIBUTING.md`
- `CODE_OF_CONDUCT.md`
- `LICENSE`
- Issue templates
- Pull request template
- Basic architecture documentation
- Screenshots or demo GIFs

Recommended license:

- Apache License 2.0 for permissive use.

Alternative:

- GPLv3 if the project wants stronger copyleft requirements.

## 14. Non-Goals for v1.0

The first version should avoid becoming too complex.

Not included in v1.0:

- Cloud sync.
- Online transcription.
- AI summarization.
- Collaborative sharing.
- Complex audio editing.
- Multi-track recording.
- Account system.
- Advertising or monetization SDKs.

## 15. MVP Scope

The MVP should include:

- Start, pause, resume, stop recording.
- Save recordings locally.
- Recording list.
- Search recordings.
- Rename, share, delete recordings.
- Basic audio playback.
- Foreground recording notification.
- Basic settings for format and quality.
- Light and dark theme.

## 16. Version Plan

### v0.1 Prototype

- Basic recording.
- Basic playback.
- Local file saving.
- Simple recording list.

### v0.5 Alpha

- Compose UI.
- Room metadata database.
- Foreground recording service.
- Search and sorting.
- Rename and delete.

### v1.0 Stable

- Polished recorder, library, player, and settings screens.
- Reliable background recording.
- Recording notification controls.
- Playback notification controls.
- Share recordings.
- Permission handling.
- Accessibility pass.
- Open source documentation.

### v1.1

- Recycle Bin.
- File naming templates.
- More audio quality options.
- Optional waveform timeline.

### v2.0

- Local transcription.
- Tags and folders.
- Encrypted recordings.
- Backup/export tools.

## 17. Main User Flows

### 17.1 Record a memo

```text
Open app
-> Tap Record
-> View timer and waveform
-> Pause or resume if needed
-> Tap Stop
-> Recording is saved
-> User can rename, play, share, or delete it
```

### 17.2 Play a recording

```text
Open Library
-> Tap a recording
-> Player opens or mini-player appears
-> Tap Play
-> Seek or change playback speed
-> Stop or return to Library
```

### 17.3 Manage recordings

```text
Open Library
-> Search or sort recordings
-> Open more menu
-> Rename, share, delete, or view details
```

### 17.4 Change recording quality

```text
Open Settings
-> Select Recording quality
-> Choose bitrate or preset
-> Future recordings use the new setting
```

## 18. UI Screen Requirements

## 18.1 Recorder Screen

Main elements:

- Top app bar.
- Waveform card.
- Timer.
- Record / pause button.
- Stop button.
- Bottom navigation.

Empty state:

- Show a calm visual area and a clear record button.

Recording state:

- Show active waveform movement.
- Show elapsed time.
- Show pause and stop actions.

Paused state:

- Show paused label.
- Keep elapsed time visible.
- Show resume and stop actions.

## 18.2 Library Screen

Main elements:

- Search bar.
- List of recordings.
- Sort/filter control.
- More menu for each item.
- Optional mini-player at the bottom.

Empty state:

- Message: “No recordings yet.”
- Action: “Start recording.”

## 18.3 Player Screen

Main elements:

- Recording title.
- Date and file information.
- Progress slider.
- Current time and duration.
- Play / pause.
- Skip backward / forward.
- Playback speed.

## 18.4 Settings Screen

Main sections:

- Appearance.
- Recording.
- Storage.
- Playback.
- Privacy.
- About.

## 19. Error Handling

SoundMemo should handle common failure cases clearly.

Common errors:

- Microphone permission denied.
- Microphone already used by another app.
- Low storage.
- File save failed.
- Recording service killed by system.
- Unsupported audio format on device.
- Playback failed because file is missing or corrupted.

Error messages should be short, human-readable, and actionable.

Example:

```text
Recording could not start because another app is using the microphone.
```

## 20. Quality Requirements

### Performance

- App launch should feel instant.
- Recorder screen should be ready quickly.
- Recording list should remain smooth with hundreds of files.
- Waveform animation should not cause high battery usage.

### Reliability

- Recording should continue when the screen is off.
- Recording should survive normal app backgrounding.
- Partial recordings should be recoverable when possible.

### Battery

- Avoid unnecessary background work.
- Keep waveform rendering lightweight.
- Use foreground service only when recording.

## 21. Suggested Android Components

SoundMemo should use native Android components such as:

- Jetpack Compose
- Material 3
- ViewModel
- Kotlin Coroutines
- StateFlow
- Room
- DataStore
- Foreground Service
- MediaRecorder or AudioRecord
- Media3 / ExoPlayer
- MediaStore or Storage Access Framework
- Notification channels
- Android Sharesheet

## 22. Future Ideas

Potential features after v1.0:

- Tags and folders.
- Favorite recordings.
- Local speech-to-text transcription.
- Local AI summary.
- Import external audio files.
- Trim recording.
- Noise reduction.
- Silence skipping.
- Home screen quick record widget.
- Wear OS quick recording companion.
- Automatic backup to user-selected folder.
- Encrypted private recordings.

## 23. Success Metrics

Because SoundMemo is open source and privacy-first, success should not depend on invasive analytics.

Possible non-invasive metrics:

- GitHub stars.
- GitHub forks.
- Issues opened and resolved.
- Pull requests merged.
- Release downloads.
- User feedback in GitHub Discussions.
- Crash reports only if users explicitly opt in.

## 24. Product Principles

SoundMemo should follow these principles:

1. **Record first**: recording should always be one tap away.
2. **Do not lose audio**: reliability is more important than visual polish.
3. **Stay local by default**: user recordings belong to the user.
4. **Use native Android behavior**: avoid unnecessary custom patterns.
5. **Keep the app understandable**: advanced features should not make basic recording harder.
6. **Respect contributors**: the project should be easy to build, test, and improve.

## 25. Summary

SoundMemo is a simple, native, open source Android voice recorder app. The first stable version should focus on reliable local recording, clean playback, basic recording management, foreground recording support, and a polished Material 3 interface. Advanced features such as transcription, encryption, tagging, and cloud backup can be added later without compromising the core experience.
