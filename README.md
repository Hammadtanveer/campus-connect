# CampusConnect

<p align="center">
  <img src="assets/campus_connect_logo.png" alt="CampusConnect Logo" width="200"/>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#installation">Installation</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#roadmap">Roadmap</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a>
</p>

## Overview

CampusConnect is a comprehensive Android application designed to enhance the college experience by creating a unified platform for academic collaboration, campus engagement, and student networking. The app facilitates note sharing, event discovery, and mentorship connections between juniors and seniors.

## Features

### Academic Resource Sharing
- 📚 Smart Notes Repository organized by course, semester, and topic
- 📝 Collaborative study materials with annotation capabilities
- 🔄 Version control for tracking document changes
- 🔍 Advanced search and filter for academic content

### Campus Life Hub
- 📅 Comprehensive event calendar for all campus activities
- 🏛 Society profiles with information and updates
- 📢 Announcement system for important campus updates
- ✅ RSVP system for event attendance tracking

### Mentorship Network
- 🤝 Senior-Junior connection platform with profile matching
- 💬 Topic-specific Q&A forums
- 📊 Office hours scheduling for mentorship sessions
- 🌟 Showcase of success stories and testimonials

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Backend**: Firebase (Authentication, Firestore, Storage)
- **Dependency Injection**: Hilt
- **Asynchronous Operations**: Coroutines & Flow
- **Navigation**: Jetpack Navigation Component
- **Image Loading**: Coil
- **Testing**: JUnit5, Mockito, Espresso

## Installation

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- Kotlin 1.5.0 or newer
- JDK 11
- Android SDK 21+

### Setup
1. Clone the repository
```bash
git clone https://github.com/Hammadtanveer/campus-connect-android.git
```

2. Open the project in Android Studio

3. Create a Firebase project and add the `google-services.json` file to the app module

4. Build the project
```bash
./gradlew build
```

5. Run on an emulator or physical device
```bash
./gradlew installDebug
```

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/campusconnect/
│   │   │   ├── data/            # Data layer: repositories, data sources
│   │   │   ├── di/              # Dependency injection modules
│   │   │   ├── domain/          # Domain layer: use cases, models
│   │   │   ├── presentation/    # UI layer: screens, viewmodels
│   │   │   ├── util/            # Utility classes
│   │   │   └── CampusConnectApp.kt
│   │   ├── res/                 # Resources
│   │   └── AndroidManifest.xml
│   ├── test/                    # Unit tests
│   └── androidTest/             # Instrumentation tests
├── build.gradle
└── proguard-rules.pro
```

## Screenshots

*Coming soon!*

## Roadmap

- [x] Project initialization
- [ ] User authentication system
- [ ] Profile management
- [ ] Notes repository implementation
- [ ] Event calendar integration
- [ ] Mentorship matching algorithm
- [ ] Offline caching
- [ ] Push notifications
- [ ] Performance optimization
- [ ] Beta testing

## Contributing

We welcome contributions to the CampusConnect project! Please follow these steps:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Created with ❤️ by <a href="https://github.com/Hammadtanveer">Hammadtanveer</a>
</p>
