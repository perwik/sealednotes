# SealedNotes

An open-source, offline-first, encrypted replacement for Google Keep.

**Your notes are sealed — closed to everyone but you.**

## Features

- **Encrypted storage** — All notes are stored in a SQLCipher-encrypted database. No one can read your data without the encryption key.
- **Fully offline** — No internet connection required. No cloud sync. No accounts. No tracking.
- **Notes & checklists** — Create text notes and checkbox lists, just like Google Keep.
- **Reminders** — Set time-based reminders with local notifications. Supports recurring reminders (daily, weekly, monthly).
- **Labels** — Organize notes with labels. Filter by label from the navigation drawer.
- **Search** — Full-text search across all notes, titles, and checklist items.
- **Pin & archive** — Pin important notes to the top. Archive notes you want to keep but not see daily.
- **Dark mode** — Clean, minimal dark interface.
- **Easy migration** — Export the encrypted database file and encryption key. Copy to a new phone via USB. Done.

## Privacy

SealedNotes collects **zero** data. There is no analytics, no telemetry, no network activity of any kind. The app requests no internet permission. Your notes never leave your device unless you explicitly export them.

## Installation

### F-Droid
*(Coming soon)*

### Direct APK
Download the latest release from the [GitHub Releases](../../releases) page.

### Build from source
1. Clone this repository
2. Open in Android Studio (Ladybug or newer)
3. Build and run on your device or emulator

## Backup & Migration

1. Go to **Settings → Export database** to save the encrypted database file
2. Go to **Settings → Export encryption key** to get your key as a QR code or text string
3. On your new device, install SealedNotes
4. Go to **Settings → Import encryption key** and enter/scan your key
5. Go to **Settings → Import database** and select your backup file
6. Done — all your notes are restored

⚠️ **Keep your encryption key safe.** Without it, your backup cannot be restored. This is by design.

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3 (dark theme)
- Room + SQLCipher for encrypted storage
- Android Keystore for key management
- AlarmManager for reminders
- No proprietary dependencies

## Requirements

- Android 8.0 (API 26) or higher

## License

GPLv3 — see [LICENSE](LICENSE) for details.
