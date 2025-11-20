# CampusConnect  

CampusConnect is a comprehensive Android application designed to enhance the college experience by creating a unified platform for academic collaboration, campus engagement, student networking, and career support.  

The app facilitates **note sharing, event discovery, mentorship connections, placement support, and online events** — making campus life smarter and more connected.  

---

## 🚀 Features  

### 📚 Academic Resource Sharing  
- Smart Notes Repository organized by course, semester, and topic  
- Collaborative study materials with annotation capabilities  
- Version control for tracking document changes  
- Advanced search and filter for academic content  

### 🏫 Campus Life Hub  
- Comprehensive event calendar for all campus activities  
- Society profiles with information and updates  
- Announcement system for important campus updates  
- RSVP system for event attendance tracking  

### 🤝 Mentorship Network  
- Senior-Junior connection platform with profile matching  
- Topic-specific Q&A forums  
- Office hours scheduling for mentorship sessions  
- Showcase of success stories and testimonials  

### 💼 Placement Support *(New)*  
- Centralized placement cell updates and notifications  
- Company profiles with eligibility criteria and recruitment process  
- Mock interview scheduling and preparation resources  
- Internship & job application tracking system  

### 🌐 Online Events *(New)*  
- Virtual seminar and workshop hosting  
- In-app event live streaming integration  
- Chat and Q&A during live sessions  
- Event recordings and resources available post-session  

---

## 🛠 Tech Stack  

- **Language:** Kotlin  
- **UI Framework:** Jetpack Compose  
- **Architecture:** MVVM with Clean Architecture  
- **Backend:** Firebase (Authentication, Firestore, Storage)  
- **Dependency Injection:** Hilt  
- **Asynchronous Operations:** Coroutines & Flow  
- **Navigation:** Jetpack Navigation Component  
- **Image Loading:** Coil  
- **Testing:** JUnit5, Mockito, Espresso  

---

## 📲 Installation  

### Prerequisites  
- Android Studio Arctic Fox (2020.3.1) or newer  
- Kotlin 1.5.0 or newer  
- JDK 11  
- Android SDK 21+  

### Setup  
```bash
# Clone the repository
git clone https://github.com/Hammadtanveer/campus-connect-android.git

# Open the project in Android Studio

# Create a Firebase project and add the google-services.json file to the app module

# Build the project
./gradlew build

# Run on an emulator or physical device
./gradlew installDebug

## 📂 Project Structure
app/
├── src/
│ ├── main/
│ │ ├── java/com/campusconnect/
│ │ │ ├── data/ # Data layer: repositories, data sources
│ │ │ ├── di/ # Dependency injection modules
│ │ │ ├── domain/ # Domain layer: use cases, models
│ │ │ ├── presentation/ # UI layer: screens, viewmodels
│ │ │ ├── util/ # Utility classes
│ │ │ └── CampusConnectApp.kt
│ │ ├── res/ # Resources
│ │ └── AndroidManifest.xml
│ ├── test/ # Unit tests
│ └── androidTest/ # Instrumentation tests
├── build.gradle
└── proguard-rules.pro

---

## 📸 Screenshots  
Coming soon!  

---

## 🛤 Roadmap  

- [x] Project initialization  
- [x] User authentication system  
- [x] Profile management  
- [ ] Notes repository implementation  
- [ ] Event calendar integration  
- [ ] Mentorship matching algorithm  
- [ ] Placement support module  
- [ ] Online events integration  
- [ ] Offline caching  
- [ ] Push notifications  
- [ ] Performance optimization  
- [ ] Beta testing  

---
## 🤝 Contributing  

Contributions are welcome!  
- Fork the repository  
- Create a new branch (`git checkout -b feature-name`)  
- Commit your changes (`git commit -m "Add feature"`)  
- Push to the branch (`git push origin feature-name`)  
- Open a Pull Request  
---
## 📚 Documentation

**NEW!** Comprehensive project documentation is now available:

- **[📖 INDEX.md](INDEX.md)** - Start here! Complete documentation index
- **[📋 PROJECT_ANALYSIS_SUMMARY.md](PROJECT_ANALYSIS_SUMMARY.md)** - Project overview and analysis
- **[🏗️ ARCHITECTURE_ANALYSIS.md](ARCHITECTURE_ANALYSIS.md)** - Complete architecture deep dive
- **[📘 TECHNICAL_SPECIFICATIONS.md](TECHNICAL_SPECIFICATIONS.md)** - API reference and technical specs
- **[🔧 REFACTORING_GUIDE.md](REFACTORING_GUIDE.md)** - Step-by-step improvement roadmap
- **[⚡ QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Quick lookup for developers
- **[📊 ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md)** - Visual architecture diagrams

### Quick Start with Documentation

**New to the project?**  
1. Read [INDEX.md](INDEX.md) for navigation guide
2. Check [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for key concepts
3. Follow setup instructions above

**Want to understand the architecture?**  
1. Read [ARCHITECTURE_ANALYSIS.md](ARCHITECTURE_ANALYSIS.md)
2. View [ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md)

**Planning improvements?**  
1. Review [PROJECT_ANALYSIS_SUMMARY.md](PROJECT_ANALYSIS_SUMMARY.md)
2. Follow [REFACTORING_GUIDE.md](REFACTORING_GUIDE.md)

---

## 📜 License  

This project is licensed under the **MIT License**. 
