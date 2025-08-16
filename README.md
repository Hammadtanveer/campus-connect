# CampusConnect (Android, Jetpack Compose)

An open-source campus-first app where students find and share course-specific notes, follow societies, and connect with seniors.

- License: see LICENSE (MIT).
- Contributing: see CONTRIBUTING.md and CODE_OF_CONDUCT.md.
- Security: see SECURITY.md.

## Build & run
- Requirements: Android Studio Flamingo+ (or latest), JDK 17, Android SDK.
- Open this folder in Android Studio and let Gradle sync.
- Run the app module on an emulator or device.

## Modules
- app: Jetpack Compose UI, Navigation, simple ViewModel state.

## Notable UI
- Bottom nav: Notes, Seniors, Societies.
- Drawer: Profile, Downlode (Downloads), Add Account.

## Downloads (Downlode) screen
- Shows an empty state initially.
- "Add sample" adds a mock note; Remove removes an item; Manage clears all for now.

## Maintenance
- Ignore generated artifacts: .gitignore covers build/, .gradle/, .idea/, .kotlin/.
- Cleanup scripts:
  - Windows PowerShell: scripts/clean.ps1
  - Bash: scripts/clean.sh

## Private docs
- local_only/CampusConnect_PRD.md contains internal PRD and is git-ignored.
