# CampusConnect - Visual Architecture Diagrams

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Interface Layer                     │
│                      (Jetpack Compose + Material 3)             │
├─────────────────────────────────────────────────────────────────┤
│  Welcome/    │  Auth      │  Main     │  Events   │  Mentorship │
│  Onboarding  │  Screens   │  View     │  Screens  │  Screens    │
│              │            │           │           │             │
│  - Welcome   │  - Login   │  - Drawer │  - List   │  - List     │
│  - Splash    │  - Register│  - Bottom │  - Detail │  - Profile  │
│              │            │    Nav    │  - Create │  - Requests │
└──────────────┴────────────┴───────────┴───────────┴─────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        Navigation Layer                          │
│                   (Jetpack Navigation Compose)                   │
├─────────────────────────────────────────────────────────────────┤
│  Navigation.kt  │  Screen.kt  │  Route Management               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                         ViewModel Layer                          │
│                    (State Management - MVVM)                     │
├─────────────────────────────────────────────────────────────────┤
│  MainViewModel (994 lines - MONOLITHIC)                         │
│  ├─ Authentication Logic                                         │
│  ├─ User Profile Management                                      │
│  ├─ Events Management                                            │
│  ├─ Mentorship Logic                                             │
│  ├─ Downloads Management                                         │
│  ├─ Activity Tracking                                            │
│  └─ Notification Listeners                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Repository Layer                            │
│                    (Data Access & Business Logic)                │
├─────────────────────────────────────────────────────────────────┤
│  EventsRepository  │  (Other repositories planned)               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                         Data Sources                             │
│                    (Firebase Services)                           │
├─────────────────────────────────────────────────────────────────┤
│  Firebase Auth  │  Firestore  │  Storage  │  FCM  │  Functions  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Architecture

### Authentication Flow

```
┌─────────────┐
│    User     │
│   Input     │
└──────┬──────┘
       │
       ↓
┌──────────────────┐
│   AuthScreen     │ (UI)
│   - Email        │
│   - Password     │
└──────┬───────────┘
       │
       ↓
┌──────────────────────────┐
│    MainViewModel         │
│  signInWithEmail         │
│  Password()              │
└──────┬───────────────────┘
       │
       ↓
┌──────────────────────────┐
│   Firebase Auth          │
│  .signInWith             │
│  EmailAndPassword()      │
└──────┬───────────────────┘
       │
       ↓ (Success)
┌──────────────────────────┐
│   Firestore              │
│  Load User Profile       │
│  /users/{uid}            │
└──────┬───────────────────┘
       │
       ↓
┌──────────────────────────┐
│   MainViewModel          │
│  _userProfile.value =    │
│  profile                 │
└──────┬───────────────────┘
       │
       ↓
┌──────────────────────────┐
│   AuthGate               │
│  Navigate to MainView    │
└──────────────────────────┘
```

---

## Events System Flow

```
┌──────────────────┐
│  EventsList      │
│  Screen          │
└────────┬─────────┘
         │
         ↓ loadEvents()
┌────────────────────────┐
│   MainViewModel        │
│   eventsRepo.observe   │
│   Events()             │
└────────┬───────────────┘
         │
         ↓
┌────────────────────────┐
│  EventsRepository      │
│  observeEvents()       │
└────────┬───────────────┘
         │
         ↓
┌────────────────────────┐
│  Firestore             │
│  /events collection    │
│  .addSnapshotListener  │
└────────┬───────────────┘
         │
         ↓ Real-time updates
┌────────────────────────┐
│  callbackFlow          │
│  Flow<Resource<List>>  │
└────────┬───────────────┘
         │
         ↓
┌────────────────────────┐
│  ViewModel State       │
│  _eventsList.value =   │
│  events                │
└────────┬───────────────┘
         │
         ↓
┌────────────────────────┐
│  UI Recomposes         │
│  Display Events        │
└────────────────────────┘
```

---

## Mentorship System Flow

```
┌─────────────────────────────────────────────────────────┐
│                    Mentorship Flow                       │
└─────────────────────────────────────────────────────────┘

Student Side:                      Mentor Side:
┌──────────────┐                  ┌──────────────┐
│ Browse       │                  │ Receive      │
│ Mentors      │                  │ Notification │
└──────┬───────┘                  └──────▲───────┘
       │                                  │
       ↓                                  │
┌──────────────┐                         │
│ Select       │                         │
│ Mentor       │                         │
└──────┬───────┘                         │
       │                                  │
       ↓                                  │
┌──────────────┐                         │
│ Send Request │─────────────────────────┤
│ with Message │                         │
└──────┬───────┘                         │
       │                                  │
       │         Firestore               │
       │         /mentorship_requests    │
       │         {                        │
       │           senderId: student     │
       │           receiverId: mentor    │
       │           status: "pending"     │
       │         }                        │
       │                                  │
       ↓                                  │
┌──────────────┐                  ┌──────────────┐
│ Wait for     │                  │ Review       │
│ Response     │                  │ Request      │
└──────┬───────┘                  └──────┬───────┘
       │                                  │
       │                                  ↓
       │                          ┌──────────────┐
       │                          │ Accept/      │
       │                          │ Reject       │
       │                          └──────┬───────┘
       │                                  │
       │◄─────────────────────────────────┤
       │         Status Update            │
       │                                  │
       ↓                                  ↓
┌──────────────┐                  ┌──────────────┐
│ Connection   │◄────────────────►│ Connection   │
│ Established  │                  │ Established  │
└──────────────┘                  └──────────────┘
       │                                  │
       │    /mentorship_connections       │
       │    {                             │
       │      mentorId, menteeId,         │
       │      participants[]              │
       │    }                             │
       │                                  │
       ↓                                  ↓
┌──────────────┐                  ┌──────────────┐
│ View in      │                  │ View in      │
│ Connections  │                  │ Connections  │
└──────────────┘                  └──────────────┘
```

---

## State Management Pattern

```
┌─────────────────────────────────────────────────────────┐
│                   ViewModel State                        │
└─────────────────────────────────────────────────────────┘

Private Mutable State:
┌──────────────────────────────────┐
│  private val _state =            │
│    mutableStateOf<T>(initial)    │
└──────────────────────────────────┘
                │
                ↓
Public Read-Only Accessor:
┌──────────────────────────────────┐
│  val state: T                    │
│    get() = _state.value          │
└──────────────────────────────────┘
                │
                ↓
UI Observes State:
┌──────────────────────────────────┐
│  @Composable                     │
│  fun MyScreen(viewModel: VM) {   │
│    val state = viewModel.state   │
│    // Use state                  │
│  }                               │
└──────────────────────────────────┘
                │
                ↓
Recomposition on State Change:
┌──────────────────────────────────┐
│  State changes trigger           │
│  automatic UI recomposition      │
└──────────────────────────────────┘
```

---

## Resource Wrapper Pattern

```
┌────────────────────────────────────────────┐
│         sealed class Resource<T>           │
├────────────────────────────────────────────┤
│                                            │
│  ┌──────────────┐                         │
│  │   Loading    │  No data yet            │
│  └──────────────┘                         │
│         │                                  │
│         ↓                                  │
│  ┌──────────────┐                         │
│  │  Success<T>  │  data: T                │
│  └──────────────┘                         │
│         │                                  │
│         ↓                                  │
│  ┌──────────────┐                         │
│  │    Error     │  message: String?       │
│  └──────────────┘                         │
└────────────────────────────────────────────┘

UI Handling:
┌────────────────────────────────────────────┐
│  when (resource) {                         │
│    is Resource.Loading -> {                │
│      CircularProgressIndicator()           │
│    }                                       │
│    is Resource.Success -> {                │
│      DisplayData(resource.data)            │
│    }                                       │
│    is Resource.Error -> {                  │
│      ErrorMessage(resource.message)        │
│    }                                       │
│  }                                         │
└────────────────────────────────────────────┘
```

---

## Navigation Structure

```
┌─────────────────────────────────────────────────────────┐
│                    App Navigation                        │
└─────────────────────────────────────────────────────────┘

MainActivity
    │
    ↓
AuthGate (Auth State Manager)
    │
    ├─── Not Authenticated ──→ WelcomeHost
    │                              │
    │                              ├─ WelcomeScreen
    │                              └─ AuthScreen
    │                                    ├─ LoginScreen
    │                                    └─ RegisterScreen
    │
    └─── Authenticated ──→ MainView
                              │
                              ├─ TopAppBar
                              ├─ NavigationDrawer
                              │     ├─ Profile
                              │     ├─ Notes
                              │     ├─ Seniors
                              │     ├─ Societies
                              │     ├─ Events
                              │     ├─ Mentors
                              │     ├─ Mentorship
                              │     ├─ Placement & Career
                              │     └─ Downloads
                              │
                              └─ NavHost
                                    │
                                    ├─ profile
                                    ├─ notes
                                    ├─ seniors
                                    ├─ societies
                                    ├─ download
                                    ├─ placement_career
                                    │
                                    ├─ events
                                    ├─ event/{eventId}
                                    ├─ events/create
                                    │
                                    ├─ mentors
                                    ├─ mentor/{mentorId}
                                    ├─ mentorship
                                    └─ mentorship/request/{requestId}
```

---

## Firestore Database Schema

```
Firebase Firestore
│
├─ users/
│  └─ {userId}/
│     ├─ id: String
│     ├─ displayName: String
│     ├─ email: String
│     ├─ course: String
│     ├─ branch: String
│     ├─ year: String
│     ├─ bio: String
│     ├─ profilePictureUrl: String
│     ├─ eventCount: Number
│     ├─ isMentor: Boolean
│     ├─ mentorshipBio: String
│     ├─ expertise: Array<String>
│     ├─ mentorshipStatus: String
│     ├─ mentorshipRating: Number?
│     └─ totalConnections: Number
│
├─ events/
│  └─ {eventId}/
│     ├─ id: String
│     ├─ title: String
│     ├─ description: String
│     ├─ dateTime: Timestamp
│     ├─ durationMinutes: Number
│     ├─ organizerId: String
│     ├─ organizerName: String
│     ├─ category: String (EventCategory)
│     ├─ participants: Array<String>
│     ├─ maxParticipants: Number
│     ├─ meetLink: String
│     ├─ imageUrl: String
│     ├─ tags: Array<String>
│     └─ createdAt: Timestamp
│
├─ mentorship_requests/
│  └─ {requestId}/
│     ├─ id: String
│     ├─ senderId: String
│     ├─ receiverId: String
│     ├─ message: String
│     ├─ status: String (pending/accepted/rejected)
│     ├─ createdAt: Timestamp
│     └─ updatedAt: Timestamp?
│
└─ mentorship_connections/
   └─ {connectionId}/
      ├─ id: String
      ├─ mentorId: String
      ├─ menteeId: String
      ├─ participants: Array<String>
      └─ connectedAt: Timestamp
```

---

## Event Categories Enum

```
┌─────────────────────────────────────┐
│      enum class EventCategory       │
├─────────────────────────────────────┤
│  WORKSHOP        Tech workshops     │
│  SEMINAR         Guest lectures     │
│  COMPETITION     Hackathons, etc.   │
│  SOCIAL          Social events      │
│  ACADEMIC        Academic events    │
│  CAREER          Placement/Career   │
│  OTHER           Miscellaneous      │
└─────────────────────────────────────┘
```

---

## User Activity Types

```
┌─────────────────────────────────────┐
│      enum class ActivityType        │
├─────────────────────────────────────┤
│  NOTE_UPLOAD     Uploaded notes     │
│  EVENT_JOINED    Joined event       │
│  NOTE_DOWNLOAD   Downloaded notes   │
│  PROFILE_UPDATE  Updated profile    │
└─────────────────────────────────────┘

Additional tracked activities (string-based):
- MENTORSHIP_REQUEST
- MENTORSHIP_ACCEPTED
- MENTORSHIP_REJECTED
- MENTORSHIP_REMOVED
- MENTORSHIP_NOTIFICATION
```

---

## Notification System

```
┌─────────────────────────────────────────────────────────┐
│              Notification Architecture                   │
└─────────────────────────────────────────────────────────┘

NotificationHelper (Utility)
    │
    ├─ scheduleEventReminder()
    │     │
    │     ↓
    │  AlarmManager
    │     │
    │     ↓ (Trigger at reminder time)
    │  NotificationReceiver (BroadcastReceiver)
    │     │
    │     ↓
    │  Display Notification
    │
    └─ showSimpleNotification()
          │
          ↓
       NotificationManager
          │
          ↓
       Display Notification

Channels:
┌──────────────────────┐
│  events_channel      │  Priority: HIGH
│  For event reminders │
└──────────────────────┘

┌──────────────────────┐
│  mentorship_channel  │  Priority: HIGH
│  For mentorship      │
└──────────────────────┘
```

---

## Proposed Architecture (Post-Refactoring)

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Feature ViewModels (Hilt)                   │
├─────────────────────────────────────────────────────────────┤
│  AuthViewModel  │  EventsViewModel  │  MentorshipViewModel  │
│  ProfileViewModel │ NotesViewModel  │  SocietiesViewModel   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer (Hilt)                    │
├─────────────────────────────────────────────────────────────┤
│  UserRepository  │  EventsRepository  │  MentorshipRepo     │
│  NotesRepository │  SocietiesRepo     │  PlacementRepo      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ├──────────────┬────────────────┐
                              ↓              ↓                ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Firebase    │    │  Room        │    │  Network     │
│  Services    │    │  (Local DB)  │    │  Utils       │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## Dependency Injection (Proposed with Hilt)

```
┌─────────────────────────────────────────────────────────┐
│                     @HiltAndroidApp                      │
│                  CampusConnectApp                        │
└─────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────┐
│                      DI Modules                          │
├─────────────────────────────────────────────────────────┤
│  AppModule:                                             │
│    - FirebaseAuth                                        │
│    - FirebaseFirestore                                   │
│    - FirebaseStorage                                     │
│                                                          │
│  RepositoryModule:                                       │
│    - UserRepository                                      │
│    - EventsRepository                                    │
│    - MentorshipRepository                                │
│    - NotesRepository                                     │
│                                                          │
│  DatabaseModule:                                         │
│    - Room Database                                       │
│    - DAOs                                                │
└─────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────┐
│                  @HiltViewModel                          │
│              Feature ViewModels                          │
│        (Auto-injected dependencies)                      │
└─────────────────────────────────────────────────────────┘
```

---

## Error Handling Flow

```
Repository/Firebase Call
    │
    ↓
Try-Catch or Firebase Callback
    │
    ├─ Success ──→ Resource.Success(data)
    │                  │
    │                  ↓
    │              Flow/Callback
    │                  │
    │                  ↓
    │              ViewModel
    │                  │
    │                  ↓
    │              Update State
    │                  │
    │                  ↓
    │              UI Displays Data
    │
    └─ Error ──→ Resource.Error(message)
                   │
                   ↓
               Flow/Callback
                   │
                   ↓
               ViewModel
                   │
                   ↓
               Update State
                   │
                   ↓
               UI Displays Error
```

---

## Build Process Flow

```
Source Code (.kt, .xml, resources)
    │
    ↓
Gradle Build System
    │
    ├─ Kotlin Compiler
    │     │
    │     ↓ .class files
    │
    ├─ Resource Compiler (AAPT2)
    │     │
    │     ↓ Compiled resources
    │
    ├─ Firebase Google Services Plugin
    │     │
    │     ↓ Process google-services.json
    │
    └─ Compose Compiler
          │
          ↓ Compose bytecode
    │
    ↓
DEX Compiler (D8/R8)
    │
    ↓ .dex files
    │
    ↓
APK/AAB Packager
    │
    ├─ Debug Build → .apk (unoptimized)
    │
    └─ Release Build → .aab (optimized, ProGuard)
          │
          ↓
       App Bundle (.aab)
```

---

**Document Version:** 1.0  
**Last Updated:** November 19, 2025  
**Purpose:** Visual reference for architecture and data flows

