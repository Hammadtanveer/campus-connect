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
- Room (local persistence groundwork)
- Paging 3 (large lists)
- WorkManager (background sync)
- Coil (image loading)

### Firebase services
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- Firebase Cloud Messaging (FCM)
- Firebase Analytics
- Firebase Crashlytics
- Firebase Cloud Functions

### Media and backend tooling
- Cloudinary (signed uploads + private PDF URLs)
- Node.js (Cloud Functions + admin scripts)
- Firebase Hosting (static `public/`)

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
- Node.js 18+ (Cloud Functions + scripts)

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
   - Functions
   - Analytics/Crashlytics (optional but recommended)

### `google-services.json` setup
1. Download `google-services.json` from Firebase Console.
2. Place it at:

```text
app/google-services.json
```

### Local configuration (`local.properties`)
Add the following keys (used by `app/build.gradle.kts`):

```text
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CLOUDINARY_UPLOAD_PRESET=your_upload_preset
KEYSTORE_PATH=path\to\keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### Cloud Functions configuration
Set the admin access code and default permissions in Firebase Functions config:

```bash
firebase functions:config:set campus.admin_code="YOUR_ADMIN_CODE"
firebase functions:config:set campus.default_admin_permissions="meetings:manage,notes:manage,placements:manage,society:*:manage"
```

Cloudinary credentials for functions are read from environment variables during deploy/runtime:

```text
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
```

### Cloud Functions setup (optional but recommended)

```bash
cd cloud-functions
npm install
firebase deploy --only functions
```

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
- Module topics used by Cloud Functions: `events`, `placements`, `society_updates`, `notes`

### Firestore-driven triggers
- `notes/{noteId}` -> topic `all_students`
- `events/{eventId}` -> topic `events`
- `meetings/{meetingId}` -> topic `events`
- `announcements/{announcementId}` -> topic `events`
- `placements/{placementId}` -> topic `placements`
- `societies/{societyId}/events/{eventId}` -> topic `society_updates`
- `notification_queue/{queueId}` -> validated topic send

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

### Bootstrap options
- Cloud Function (one-time): `bootstrapSuperAdmin` in `cloud-functions/bootstrap-super-admin.js`
- Manual script (one-time): `scripts/create-super-admin.js`
- Assisted PowerShell flow: `scripts/setup-admin.ps1`

### Admin helper scripts
- `scripts/listUsers.js`: list Firebase Auth users/UIDs
- `scripts/setCustomClaims.js`: update Firestore permissions by UID

> Review and run the provided scripts carefully in a secure environment before production use.

---

## 9) Cloud Functions & Scripts 🧩

### Cloud Functions (`cloud-functions/`)
- `requestAdminAccess`: validates admin code and applies default permissions
- `createCloudinaryUploadSignature`: signed upload params
- `generateSignedPdfUrl`: temporary private PDF URL
- Notification triggers for notes, events, meetings, announcements, placements, society events
- `processNotificationQueue`: validates and dispatches queued notifications
- `sendTopicNotification`: super-admin topic send
- `onUserDeleted`: cleanup Firestore `users/{uid}`

### Local fallback sender
If Cloud Functions deployment is blocked, use the fallback script:

```powershell
Set-Location "D:\AndroidStudioProjects\campus-connect\cloud-functions"
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\path\to\service-account.json"
npm run send:topic:fallback -- --topic all_students --title "New Notes Uploaded" --body "CN Unit 3" --type notes
```

---

## 10) Contributing 🤝

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

## 11) License 📄

This project is licensed under the terms in `LICENSE`.

---

## 12) Contact 📬

- Open an issue in this repository for bug reports and feature requests.
- For maintainership/contact details, add project owner email or profile link here.

---

If you find this project useful, consider starring ⭐ the repository
