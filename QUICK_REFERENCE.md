# CampusConnect - Quick Reference Guide

## 🎯 Project Overview

**Type:** Android Native Application  
**Language:** Kotlin  
**UI:** Jetpack Compose + Material 3  
**Backend:** Firebase (Auth, Firestore, Storage, FCM)  
**Architecture:** MVVM (Currently monolithic, refactoring recommended)

---

## 📁 Key Files Quick Reference

### Core Application
- `MainActivity.kt` - App entry point
- `MainViewModel.kt` - **Central ViewModel (994 lines)** - Handles all business logic
- `Navigation.kt` - Navigation graph and routes
- `Screen.kt` - Screen definitions

### Authentication
- `AuthGate.kt` - Auth state manager
- `AuthScreen.kt` - Login/Register UI
- `WelcomeLoginScreens.kt` - Onboarding flow

### Features
**Events:**
- `EventsRepository.kt` - Data layer for events
- `EventsModels.kt` - Event data models
- `EventsListScreen.kt` - List view
- `EventDetailScreen.kt` - Detail view
- `CreateEventScreen.kt` - Create event form

**Mentorship:**
- `MentorsListScreen.kt` - Browse mentors
- `MentorProfileScreen.kt` - Mentor details
- `MyMentorshipScreen.kt` - Dashboard
- `RequestDetailScreen.kt` - Request management
- `MentorshipRequest.kt` - Request model
- `MentorshipConnection.kt` - Connection model

**Other Modules:**
- `NotesView.kt` - Notes browser (partial implementation)
- `Societies.kt` - Societies grid (basic implementation)
- `PlacementCareerScreen.kt` - Placeholder
- `AccountView.kt` - User profile
- `DownloadView.kt` - Downloads manager

### Data Models
- `UserProfile.kt` - User data model
- `UserActivity.kt` - Activity tracking
- `EventsModels.kt` - Event categories and models
- `Senior.kt` - Legacy senior profile model

### Utilities
- `NotificationHelper.kt` - Notification scheduling
- `NotificationReceiver.kt` - Broadcast receiver
- `NetworkUtils.kt` - Network connectivity checks
- `Resource.kt` - API response wrapper

---

## 🔑 Important Classes & Methods

### MainViewModel Key Methods

```kotlin
// Authentication
signInWithEmailPassword(email, password, onResult)
registerWithEmailPassword(email, password, displayName, course, branch, year, bio, onResult)
signOut()

// Profile
updateUserProfile(updatedProfile, onResult)
updateMentorProfile(bio, expertise, status, onResult)

// Events
loadEvents(): Flow<Resource<List<OnlineEvent>>>
createEvent(...)
registerForEvent(eventId, onResult)

// Mentorship
loadMentors(): Flow<Resource<List<UserProfile>>>
sendMentorshipRequest(mentorId, message): Flow<Resource<Boolean>>
getMyMentorshipRequests(): Flow<Resource<List<MentorshipRequest>>>
getReceivedRequests(): Flow<Resource<List<MentorshipRequest>>>
acceptRequest(requestId): Flow<Resource<Boolean>>
rejectRequest(requestId): Flow<Resource<Boolean>>
getMyConnections(): Flow<Resource<List<UserProfile>>>
removeConnection(otherUserId): Flow<Resource<Boolean>>

// Listeners
startPendingRequestsListener(context?)
stopPendingRequestsListener()

// Downloads
addDownload(title, sizeLabel)
removeDownload(id)
clearDownloads()
```

---

## 🗂️ Firestore Collections

```
users/                          # User profiles
  {userId}/
    - id, displayName, email, course, branch, year, bio
    - isMentor, mentorshipBio, expertise, mentorshipStatus
    - mentorshipRating, totalConnections

events/                         # Campus events
  {eventId}/
    - id, title, description, dateTime, durationMinutes
    - organizerId, organizerName, category
    - participants[], maxParticipants, meetLink

mentorship_requests/            # Mentorship requests
  {requestId}/
    - id, senderId, receiverId, message
    - status (pending/accepted/rejected)
    - createdAt, updatedAt

mentorship_connections/         # Active mentorship connections
  {connectionId}/
    - id, mentorId, menteeId
    - participants[], connectedAt
```

---

## 🎨 Navigation Routes

```kotlin
// Main Drawer Screens
"profile"           // User profile/account
"notes"             // Notes browser
"seniors"           // Seniors listing
"societies"         // Societies/clubs
"download"          // Downloads manager
"placement_career"  // Placement & Career
"events"            // Events listing
"mentors"           // Find mentors

// Detail Screens
"event/{eventId}"                   // Event details
"events/create"                     // Create new event
"mentor/{mentorId}"                 // Mentor profile
"mentorship/request/{requestId}"    // Request details
```

---

## 🔧 Build Configuration

### Gradle Files
- `build.gradle.kts` (app) - Main build config
- `libs.versions.toml` - Version catalog
- `settings.gradle.kts` - Project settings

### Key Build Settings
```kotlin
minSdk = 29            // Android 10+
targetSdk = 36
compileSdk = 36
kotlin = "2.0.21"
agp = "8.12.1"
```

### Build Types
```kotlin
debug {
    buildConfigField("boolean", "FORCE_WELCOME", "true")
}
release {
    isMinifyEnabled = true
    isShrinkResources = true
}
```

---

## 📦 Dependencies Summary

### Core
```kotlin
androidx.core:core-ktx:1.16.0
androidx.lifecycle:lifecycle-runtime-ktx:2.9.2
androidx.activity:activity-compose:1.10.1
```

### Compose
```kotlin
androidx.compose.bom:2024.09.00
androidx.compose.material3:1.3.2
androidx.navigation:navigation-compose:2.7.5
io.coil-kt:coil-compose:2.4.0
```

### Firebase
```kotlin
firebase-bom:32.7.0
firebase-auth-ktx
firebase-firestore-ktx
kotlinx-coroutines-play-services:1.8.1
```

---

## 🚨 Known Issues & Limitations

### Current Issues
1. **Monolithic ViewModel** - MainViewModel is 994 lines (should be split)
2. **No Dependency Injection** - Manual object creation
3. **No Offline Support** - No local database (Room)
4. **No Pagination** - Loads all data at once
5. **Hard-coded Data** - Societies and courses are hard-coded
6. **Incomplete Features** - Notes, Societies, Placement are placeholders

### Missing Features
- Q&A Forums
- Real-time chat/messaging
- File uploads (notes)
- Advanced search
- User ratings/reviews
- Push notifications (FCM not fully integrated)
- Analytics

---

## 🔄 State Management Pattern

### Current Pattern
```kotlin
// ViewModel
private val _state = mutableStateOf<T>(initialValue)
val state: T get() = _state.value

// UI
val state = viewModel.state
// Use state in Compose
```

### Resource Wrapper
```kotlin
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String?) : Resource<Nothing>()
}

// Usage in UI
when (resource) {
    is Resource.Loading -> LoadingView()
    is Resource.Success -> SuccessView(resource.data)
    is Resource.Error -> ErrorView(resource.message)
}
```

---

## 🎯 Common Tasks

### Add New Screen
1. Define route in `Screen.kt`
2. Create Composable screen file
3. Add to `Navigation.kt` NavHost
4. Add to drawer menu if needed

### Add New Feature
1. Create data model (e.g., `MyFeature.kt`)
2. Create repository (e.g., `MyFeatureRepository.kt`)
3. Add methods to ViewModel (or create new ViewModel)
4. Create UI screens
5. Wire up in Navigation

### Add Firebase Collection
1. Create data class with Firestore annotations
2. Add CRUD methods to repository
3. Create Flow-based observers for real-time updates
4. Update ViewModel to expose data
5. Create UI to display/edit data

---

## 🧪 Testing

### Test Files
- `ExampleUnitTest.kt` - Unit test example
- `ProcessEventInputTest.kt` - Event processing test
- `ExampleInstrumentedTest.kt` - Instrumented test

### Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## 🚀 Build & Run

### Debug Build
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### Release Build
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Clean Build
```bash
./gradlew clean
# Or use provided scripts
.\scripts\clean.ps1      # Windows
./scripts/clean.sh       # Unix/Mac
```

---

## 🔐 Firebase Configuration

### Setup Required
1. Create Firebase project
2. Add Android app to Firebase
3. Download `google-services.json`
4. Place in `app/` directory
5. Enable Authentication (Email/Password)
6. Create Firestore database
7. Set up security rules

### Security Rules (Recommended)
See `TECHNICAL_SPECIFICATIONS.md` for detailed security rules

---

## 📊 Performance Tips

### Current Optimizations
- LazyColumn/LazyRow for lists
- Coil for image loading
- Flow for reactive data
- StateFlow for ViewModel state

### Recommended Optimizations
- Add Room for local caching
- Implement pagination
- Add image size constraints
- Use WorkManager for background tasks
- Implement request debouncing
- Add query result limits

---

## 🐛 Debugging

### Logcat Tags
```kotlin
Log.i("MainViewModel", "message")
Log.e("EventsRepository", "error", exception)
```

### Common Debug Points
- Auth state changes in `AuthGate.kt`
- Network requests in repositories
- State updates in ViewModels
- Navigation in `Navigation.kt`

### Useful ADB Commands
```bash
# Clear app data
adb shell pm clear com.example.campusconnect

# View logs
adb logcat | grep "MainViewModel"

# Check network
adb shell dumpsys connectivity
```

---

## 📝 Code Style

### Naming Conventions
- **Files:** PascalCase (e.g., `EventsListScreen.kt`)
- **Classes:** PascalCase (e.g., `MainViewModel`)
- **Functions:** camelCase (e.g., `loadEvents()`)
- **Variables:** camelCase (e.g., `eventsList`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `EVENTS_CHANNEL_ID`)

### Composable Functions
```kotlin
@Composable
fun ScreenName(
    viewModel: MyViewModel = viewModel(),
    navController: NavController
) {
    // Implementation
}
```

---

## 🔗 Useful Links

### Documentation
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Firebase Android: https://firebase.google.com/docs/android/setup
- Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
- Material 3: https://m3.material.io/

### Project Files
- `README.md` - Project overview
- `ARCHITECTURE_ANALYSIS.md` - Complete architecture analysis
- `TECHNICAL_SPECIFICATIONS.md` - API documentation
- `REFACTORING_GUIDE.md` - Refactoring roadmap
- `local_only/CampusConnect_PRD.md` - Product requirements

---

## 🎓 Learning Resources

### For New Developers
1. Start with `README.md` for project overview
2. Read `ARCHITECTURE_ANALYSIS.md` for architecture understanding
3. Check `TECHNICAL_SPECIFICATIONS.md` for API reference
4. Review `MainViewModel.kt` for business logic
5. Explore UI screens in `ui/theme/` directory

### Key Concepts to Understand
- MVVM architecture pattern
- Jetpack Compose declarative UI
- Kotlin Coroutines and Flow
- Firebase Firestore real-time updates
- State management in Compose
- Navigation in Compose

---

## 🔮 Future Roadmap

### Phase 1: Refactoring (High Priority)
- Implement Hilt dependency injection
- Split MainViewModel into feature ViewModels
- Create proper repository layer
- Add Room for offline support

### Phase 2: Feature Completion (Medium Priority)
- Complete Notes module with uploads
- Implement Q&A forums
- Complete Societies module
- Build out Placement module

### Phase 3: Enhancements (Low Priority)
- Add real-time chat
- Implement advanced search
- Add analytics
- Support multiple campuses
- Add video calling for mentorship

---

## 📞 Quick Contact

For questions or contributions:
- Check `CONTRIBUTING.md` for contribution guidelines
- Review `CODE_OF_CONDUCT.md` for community standards
- See `SECURITY.md` for security reporting

---

**Document Version:** 1.0  
**Last Updated:** November 19, 2025  
**For:** Quick reference and onboarding

