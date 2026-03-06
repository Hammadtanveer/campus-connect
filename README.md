# Campus Connect

## Overview
Campus Connect is an Android application built with Kotlin and Firebase to improve communication and engagement across a college campus. It gives students a centralized experience for societies, events, notes, and campus updates.

## Features
- Student login and authentication
- Societies and clubs discovery
- Society-based event browsing
- Role-based event creation (admin/super admin)
- Event image upload using Cloudinary
- Event calendar and reminders
- Notes sharing module
- Real-time updates with Firebase Firestore
- Clean, responsive Android UI

## Tech Stack
- Kotlin
- Android Studio
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Cloudinary (event images)
- MVVM Architecture

## App Architecture
Campus Connect follows MVVM for clear separation of concerns:
- **Model**: data models and repositories
- **ViewModel**: business logic and UI state
- **View**: Jetpack Compose screens/components

### Key Highlights
- Built with MVVM architecture
- Uses Firebase for real-time data
- Cloudinary integration for media storage
- Modular Android architecture

## Project Structure
```text
campus-connect/
|- app/
|  |- src/main/java/com/example/campusconnect/
|  |  |- data/          # Models, repositories, data sources
|  |  |- ui/            # Screens, components, viewmodels
|  |  |- util/          # Helpers and utilities
|  |  |- di/            # Dependency injection modules
|  |- src/main/res/     # Android resources
|- cloud-functions/     # Firebase cloud functions
|- scripts/             # Setup and admin scripts
|- firebase.json
|- firestore.rules
|- README.md
```

## Installation & Setup
### Prerequisites
- Android Studio (latest stable)
- JDK 11+
- Firebase project access
- Android emulator or physical device

### Setup Steps
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd campus-connect
   ```
2. Open the project in Android Studio.
3. Add `google-services.json` inside `app/`.
4. Configure Firebase and Cloudinary credentials as required.
5. Sync Gradle and run the app.

## How the App Works
1. Users sign in through Firebase Authentication.
2. Students browse societies and related events.
3. Admin users create/manage events.
4. Event images are uploaded to Cloudinary and stored as URLs.
5. Event and notes data are stored in Firestore and update in real time.

## Screenshots
> Add screenshots here.

Suggested sections:
- Login Screen
- Societies Screen
- Society Events Screen
- Create Event Screen
- Notes Screen

## Future Improvements
- Push notifications for event reminders
- In-app event registration workflow
- Advanced search and filters
- Offline caching enhancements
- Admin analytics dashboard

## Contribution Guidelines
Contributions are welcome.

1. Fork the repository.
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Commit with clear messages.
4. Push and open a pull request.

Please ensure:
- Changes align with existing architecture
- Code is tested before PR submission
- Relevant documentation is updated

## License
This project is licensed under the terms defined in the repository `LICENSE` file.
