# CampusConnect 🚀

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)
![Min SDK](https://img.shields.io/badge/Min%20SDK-25-blue)
![Version](https://img.shields.io/badge/Version-1.0.0-informational)

CampusConnect is a modern college ecosystem app that unifies academic collaboration, events, placements, and student communities in one Android experience.

---

## 1) Overview 🎯

CampusConnect is an Android application designed to improve day-to-day campus life through a centralized, mobile-first platform.

### Who is it for?
- College students
- Student club/society coordinators
- Placement/career coordinators
- College administrators and moderators

### Core purpose
- Make campus resources easier to discover and use
- Improve communication through real-time updates and notifications
- Support role-based workflows for students and admins

---

## 2) Features ✨

### Notes Sharing 📚
- Upload notes (PDF) with metadata
- Download and view notes
- Filter by subject/semester
- Search support for quick access
- Admin moderation hooks for content quality

### Meetings & Announcements 📢
- Create and manage online/offline events
- View event details and participation count
- Register for events
- Schedule reminders for upcoming meetings

### Placement & Career 💼
- Browse placement opportunities
- View role/company details
- Apply to opportunities through app workflows
- Placement-focused screens and detail views

### Societies & Clubs 🏛️
- Multiple societies support
- Society-level updates and announcements
- Society post tracking and auto-notification triggers
- Join/leave/management hooks with role-based controls

### Seniors Directory 🎓
- Browse senior profiles
- Profile-level details and mentoring context
- Quick access to networking links (for example LinkedIn)

### Smart Notifications 🔔
- Firebase Cloud Messaging (FCM) integration
- Topic subscriptions (including app-wide announcements)
- Notification deep linking into specific app routes
- Firestore-triggered auto notifications for selected modules

### Admin Panel 🛡️
- Admin activity logs and operational visibility
- User and permission management
- Content moderation pathways
- Role-based access control (RBAC) for sensitive actions

---

## 3) Tech Stack 🧰

### Android app
- Kotlin
- Jetpack Compose
- Material 3
- MVVM architecture
- Hilt dependency injection
- Coroutines + StateFlow

### Firebase services
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- Firebase Cloud Messaging (FCM)
- Firebase Analytics
- Firebase Crashlytics

### Media and backend tooling
- Cloudinary (PDF/image asset workflows)
- Node.js scripts and Firebase Cloud Functions (`cloud-functions/`)

---

## 4) Architecture 🧱

### MVVM (text diagram)

```text
UI (Compose Screens)
        |
        v
ViewModel (State + Business Logic)
        |
        v
Repository Layer
   |          |
   v          v
Firebase   Cloudinary
(Auth/DB/FCM/Storage)
```

### Repository pattern
- ViewModels expose state and intents to UI
- Repositories abstract data sources and side effects
- Firebase/Cloudinary access remains outside composable UI code

### Notification pipeline (high level)

```text
Firestore/Cloud Function Events
          |
          v
FCM Topic/Message Dispatch
          |
          v
AppFirebaseMessagingService
          |
          v
NotificationHelper + NotificationIntentRouter
          |
          v
Deep link route consumed by MainActivity/Main navigation
```

---

## 5) Screenshots 🖼️

> Replace placeholders with actual screenshots before release.

### Login / Auth
<img src="docs/screenshots/auth-phone.png" alt="Auth Screen" width="260" />

### Notes Module
<img src="docs/screenshots/notes-phone.png" alt="Notes Screen" width="260" />

### Events / Announcements
<img src="docs/screenshots/events-phone.png" alt="Events Screen" width="260" />

### Placement & Career
<img src="docs/screenshots/placement-phone.png" alt="Placement Screen" width="260" />

### Admin Panel
<img src="docs/screenshots/admin-phone.png" alt="Admin Screen" width="260" />

---

## 6) Setup & Installation ⚙️

### Prerequisites
- Android Studio (latest stable)
- JDK 11+
- Android SDK configured locally
- Firebase project access
- A connected Android device or emulator

### Clone project

```bash
git clone <your-repo-url>
cd campus-connect
```

### Firebase setup
1. Create/select your Firebase project.
2. Register Android app package(s) used by this project.
3. Enable required services:
   - Authentication
   - Firestore
   - Storage
   - Cloud Messaging
   - Analytics/Crashlytics (optional but recommended)

### `google-services.json` setup
1. Download `google-services.json` from Firebase Console.
2. Place it at:

```text
app/google-services.json
```

### Cloudinary setup
1. Create a Cloudinary account and product environment.
2. Configure cloud credentials according to your chosen secure strategy.
3. Validate upload/download behavior for notes and profile/event media.

### Build & run

```bash
./gradlew assembleDebug
./gradlew :app:installDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat :app:installDebug
```

---

## 7) Notification System 🔔

### How FCM works in CampusConnect
- App initializes notification channels and FCM integration at startup
- Device token handling is managed in app startup/FCM service paths
- Notifications can be triggered from backend flows and Firestore-driven logic

### Topics
- App-wide topic subscription is supported (for example `all_students`)
- Additional topic subscriptions can be managed through notification subscription utilities

### Deep linking flow
- Incoming notification payload is parsed
- Route is resolved by `NotificationIntentRouter`
- `MainActivity` and navigation layer consume that route
- User lands directly in the relevant module/screen

---

## 8) Admin Panel 🛡️

### Admin capabilities
- View admin activity logs
- Manage users and permissions
- Moderate content and perform operational actions
- Access role-gated controls in navigation and screens

### Role-based access
- Permissions are enforced by profile role/claims and repository checks
- High-impact actions (for example management/moderation) are restricted to privileged roles

### How to set super admin
- Use backend/admin scripts in `scripts/` and `cloud-functions/` to bootstrap elevated access
- Typical flow:
  1. Initialize admin environment
  2. Create or identify target user
  3. Assign elevated claims/roles
  4. Re-authenticate or refresh token in app

> Review and run the provided scripts carefully in a secure environment before production use.

---

## 9) Contributing 🤝

Contributions are welcome.

1. Fork the repository.
2. Create a branch.
3. Implement changes with tests and documentation updates.
4. Run quality/build checks.
5. Open a pull request with a clear summary.

Recommended local checks:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:compileDebugUnitTestKotlin
./gradlew assembleDebug
```

---

## 10) License 📄

This project is licensed under the terms in `LICENSE`.

---

## 11) Contact 📬

- Open an issue in this repository for bug reports and feature requests.
- For maintainership/contact details, add project owner email or profile link here.

---

If you find this project useful, consider starring ⭐ the repository.
