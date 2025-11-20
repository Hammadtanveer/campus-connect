# CampusConnect - Technical Specifications

## API Documentation

### Firebase Firestore Data Schema

#### Collections Structure

```
campusconnect/
├── users/
│   └── {userId}/
│       ├── id: String
│       ├── displayName: String
│       ├── email: String
│       ├── course: String
│       ├── branch: String
│       ├── year: String
│       ├── bio: String
│       ├── profilePictureUrl: String
│       ├── eventCount: Number
│       ├── isMentor: Boolean
│       ├── mentorshipBio: String
│       ├── expertise: Array<String>
│       ├── mentorshipStatus: String  // "available" | "busy" | "unavailable"
│       ├── mentorshipRating: Number (nullable)
│       └── totalConnections: Number
│
├── events/
│   └── {eventId}/
│       ├── id: String
│       ├── title: String
│       ├── description: String
│       ├── dateTime: Timestamp
│       ├── durationMinutes: Number
│       ├── organizerId: String
│       ├── organizerName: String
│       ├── category: String  // EventCategory enum
│       ├── participants: Array<String>  // user IDs
│       ├── maxParticipants: Number (0 = unlimited)
│       ├── meetLink: String
│       ├── imageUrl: String
│       ├── tags: Array<String>
│       └── createdAt: Timestamp
│
├── mentorship_requests/
│   └── {requestId}/
│       ├── id: String
│       ├── senderId: String
│       ├── receiverId: String
│       ├── message: String
│       ├── status: String  // "pending" | "accepted" | "rejected"
│       ├── createdAt: Timestamp
│       └── updatedAt: Timestamp (nullable)
│
└── mentorship_connections/
    └── {connectionId}/
        ├── id: String
        ├── mentorId: String
        ├── menteeId: String
        ├── participants: Array<String>  // [mentorId, menteeId]
        └── connectedAt: Timestamp
```

---

## ViewModel API Reference

### MainViewModel Methods

#### **Authentication**

```kotlin
// Sign in with email and password
fun signInWithEmailPassword(
    email: String,
    password: String,
    onResult: (success: Boolean, error: String?) -> Unit
)

// Register new user
fun registerWithEmailPassword(
    email: String,
    password: String,
    displayName: String,
    course: String,
    branch: String,
    year: String,
    bio: String = "",
    onResult: (success: Boolean, error: String?) -> Unit
)

// Sign out current user
fun signOut()
```

#### **User Profile**

```kotlin
// Update user profile
fun updateUserProfile(
    updatedProfile: UserProfile,
    onResult: (success: Boolean, error: String?) -> Unit
)

// Update mentor-specific profile fields
fun updateMentorProfile(
    bio: String,
    expertise: List<String>,
    status: String,
    onResult: (success: Boolean, error: String?) -> Unit
)
```

#### **Events**

```kotlin
// Load all events (returns Flow)
fun loadEvents(): Flow<Resource<List<OnlineEvent>>>

// Create new event
fun createEvent(
    title: String,
    description: String,
    dateTime: Timestamp,
    durationMinutes: Long,
    category: EventCategory,
    maxParticipants: Int = 0,
    meetLink: String = "",
    onResult: (success: Boolean, error: String?) -> Unit
)

// Suspend wrapper for createEvent
suspend fun createEventAwait(
    title: String,
    description: String,
    dateTime: Timestamp,
    durationMinutes: Long,
    category: EventCategory,
    maxParticipants: Int = 0,
    meetLink: String = ""
)

// Register for event
fun registerForEvent(
    eventId: String,
    onResult: (success: Boolean, error: String?) -> Unit
)
```

#### **Mentorship**

```kotlin
// Load all available mentors
fun loadMentors(): Flow<Resource<List<UserProfile>>>

// Send mentorship request
fun sendMentorshipRequest(
    mentorId: String,
    message: String = ""
): Flow<Resource<Boolean>>

// Get requests sent by current user
fun getMyMentorshipRequests(): Flow<Resource<List<MentorshipRequest>>>

// Get requests received by current user (for mentors)
fun getReceivedRequests(): Flow<Resource<List<MentorshipRequest>>>

// Accept a mentorship request
fun acceptRequest(requestId: String): Flow<Resource<Boolean>>

// Reject a mentorship request
fun rejectRequest(requestId: String): Flow<Resource<Boolean>>

// Get current accepted connections
fun getMyConnections(): Flow<Resource<List<UserProfile>>>

// Remove an existing connection
fun removeConnection(otherUserId: String): Flow<Resource<Boolean>>

// Start listening for pending requests (with optional notifications)
fun startPendingRequestsListener(context: Context? = null)

// Stop listening for pending requests
fun stopPendingRequestsListener()
```

#### **Downloads**

```kotlin
// Add download to list
fun addDownload(title: String, sizeLabel: String)

// Remove download by ID
fun removeDownload(id: String)

// Clear all downloads
fun clearDownloads()
```

#### **Navigation**

```kotlin
// Navigate to screen by route
fun setCurrentScreenByRoute(route: String)
```

#### **Notifications**

```kotlin
// Show mentorship notification
fun notifyMentorship(
    context: Context,
    title: String,
    message: String,
    id: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
)
```

---

## Repository API Reference

### EventsRepository

```kotlin
class EventsRepository(private val db: FirebaseFirestore)

// Observe all events in real-time
fun observeEvents(): Flow<Resource<List<OnlineEvent>>>

// Create new event
fun createEvent(
    title: String,
    description: String,
    dateTime: Timestamp,
    durationMinutes: Long,
    organizerId: String,
    organizerName: String,
    category: EventCategory,
    maxParticipants: Int,
    meetLink: String,
    onResult: (success: Boolean, error: String?) -> Unit
)

// Get single event by ID
fun getEventById(eventId: String): Flow<Resource<OnlineEvent?>>

// Register user for event
fun registerForEvent(
    userId: String,
    eventId: String,
    onResult: (success: Boolean, error: String?) -> Unit
)

// Unregister user from event
fun unregisterFromEvent(
    userId: String,
    eventId: String,
    onResult: (success: Boolean, error: String?) -> Unit
)

// Delete event (organizer only)
fun deleteEvent(
    eventId: String,
    onResult: (success: Boolean, error: String?) -> Unit
)
```

---

## UI Component API

### Navigation Routes

```kotlin
// Drawer Screens
"profile"              // Profile/Account screen
"notes"                // Notes browser
"seniors"              // Seniors listing
"societies"            // Societies/clubs
"download"             // Downloads manager
"placement_career"     // Placement & Career
"events"               // Events listing
"mentors"              // Mentors listing

// Detail Screens
"event/{eventId}"                    // Event detail
"events/create"                      // Create event
"mentor/{mentorId}"                  // Mentor profile
"mentorship/request/{requestId}"     // Request detail
```

### Screen Parameters

```kotlin
// EventDetailScreen
eventId: String          // Required

// MentorProfileScreen
mentorId: String         // Required

// RequestDetailScreen
requestId: String        // Required
```

---

## Data Classes

### UserProfile

```kotlin
data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val course: String = "",
    val branch: String = "",
    val year: String = "",
    val bio: String = "",
    val profilePictureUrl: String = "",
    val eventCount: Int = 0,
    val isMentor: Boolean = false,
    val mentorshipBio: String = "",
    val expertise: List<String> = emptyList(),
    val mentorshipStatus: String = "available",
    val mentorshipRating: Double? = null,
    val totalConnections: Int = 0
)
```

### OnlineEvent

```kotlin
data class OnlineEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp? = null,
    val durationMinutes: Long = 60,
    val organizerId: String = "",
    val organizerName: String = "",
    val category: EventCategory = EventCategory.OTHER,
    val participants: List<String> = emptyList(),
    val maxParticipants: Int = 0,
    val meetLink: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)

enum class EventCategory {
    WORKSHOP, SEMINAR, COMPETITION, SOCIAL, ACADEMIC, CAREER, OTHER
}
```

### MentorshipRequest

```kotlin
data class MentorshipRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val status: String = "pending",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

// Status values: "pending", "accepted", "rejected"
```

### MentorshipConnection

```kotlin
data class MentorshipConnection(
    val id: String = "",
    val mentorId: String = "",
    val menteeId: String = "",
    val participants: List<String> = emptyList(),
    val connectedAt: Timestamp? = null
)
```

### UserActivity

```kotlin
data class UserActivity(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val iconResId: Int
)

enum class ActivityType {
    NOTE_UPLOAD, EVENT_JOINED, NOTE_DOWNLOAD, PROFILE_UPDATE
}
```

### Resource (Wrapper)

```kotlin
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String?) : Resource<Nothing>()
}
```

---

## Notification System

### NotificationHelper

```kotlin
object NotificationHelper {
    // Schedule event reminder (30 min before)
    fun scheduleEventReminder(context: Context, event: OnlineEvent)
    
    // Cancel scheduled reminder
    fun cancelEventReminder(context: Context, eventId: String)
    
    // Show simple notification
    fun showSimpleNotification(
        context: Context,
        notifId: Int,
        title: String,
        text: String
    )
}
```

### Notification Channels

```kotlin
// Events channel
const val EVENTS_CHANNEL_ID = "events_channel"
Priority: HIGH
Description: "Event reminders and updates"

// Mentorship channel
const val MENTORSHIP_CHANNEL_ID = "mentorship_channel"
Priority: HIGH
Description: "Notifications for mentorship requests and updates"
```

---

## Network Utilities

### NetworkUtils

```kotlin
object NetworkUtils {
    // Check network availability
    fun isNetworkAvailable(context: Context): Boolean
}
```

---

## Error Handling

### Common Error Messages

```kotlin
// Authentication Errors
"No user found with this email. Please sign up first."
"Incorrect password. Please try again."
"This user account has been disabled."
"Network error. Check your internet connection and try again."
"Password is too weak. Please use at least 6 characters."
"This email is already in use. Try signing in or use a different email."
"The email address is invalid. Check for typos."

// Generic Errors
"Not authenticated"
"Request not found"
"Malformed request"
"Connection not found"
```

### Resource Error Handling

```kotlin
when (resource) {
    is Resource.Loading -> { /* Show loading indicator */ }
    is Resource.Success -> { /* Handle data: resource.data */ }
    is Resource.Error -> { /* Show error: resource.message */ }
}
```

---

## Performance Optimizations

### Current Implementations

1. **Lazy Loading:** LazyColumn/LazyRow for lists
2. **State Hoisting:** Compose best practices
3. **Flow Operators:** Efficient reactive streams
4. **Coil Image Loading:** Async image loading with caching

### Recommended Optimizations

1. **Pagination:** Implement for large lists
2. **Local Caching:** Room database for offline support
3. **Image Optimization:** Set size constraints on Coil
4. **Background Work:** WorkManager for uploads
5. **Query Limits:** Firestore query limits to reduce bandwidth

---

## Security Best Practices

### Implemented

- Firebase Authentication
- HTTPS-only communication (Firebase default)
- ProGuard for code obfuscation (release builds)

### Recommended

```kotlin
// Firestore Security Rules (example)
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only read/write their own profile
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    
    // Events are readable by all, writable by creator
    match /events/{eventId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth.uid == resource.data.organizerId;
    }
    
    // Mentorship requests
    match /mentorship_requests/{requestId} {
      allow read: if request.auth != null && 
                     (request.auth.uid == resource.data.senderId || 
                      request.auth.uid == resource.data.receiverId);
      allow create: if request.auth != null && 
                       request.auth.uid == request.resource.data.senderId;
      allow update: if request.auth != null && 
                       request.auth.uid == resource.data.receiverId;
    }
    
    // Mentorship connections
    match /mentorship_connections/{connectionId} {
      allow read: if request.auth != null && 
                     request.auth.uid in resource.data.participants;
      allow create: if request.auth != null;
      allow delete: if request.auth != null && 
                       request.auth.uid in resource.data.participants;
    }
  }
}
```

---

## Testing Guidelines

### Unit Testing

```kotlin
// Example: Testing ViewModel
@Test
fun `signIn with valid credentials should succeed`() {
    // Given
    val email = "test@example.com"
    val password = "password123"
    
    // When
    viewModel.signInWithEmailPassword(email, password) { success, error ->
        // Then
        assertTrue(success)
        assertNull(error)
    }
}
```

### UI Testing

```kotlin
// Example: Testing Compose UI
@Test
fun eventsList_displayedCorrectly() {
    composeTestRule.setContent {
        EventsListScreen(viewModel = mockViewModel, navController = mockNavController)
    }
    
    composeTestRule.onNodeWithText("Events").assertIsDisplayed()
}
```

---

## Deployment Checklist

### Pre-Release

- [ ] Update version code and version name
- [ ] Enable ProGuard
- [ ] Configure Firebase security rules
- [ ] Test on multiple Android versions (min API 29)
- [ ] Test dark/light themes
- [ ] Test offline functionality
- [ ] Run unit and instrumentation tests
- [ ] Check memory leaks
- [ ] Optimize images and resources
- [ ] Review permissions in manifest

### Release Build

```bash
# Generate signed APK/Bundle
./gradlew bundleRelease

# Or
./gradlew assembleRelease
```

### Post-Release

- [ ] Monitor Firebase Crashlytics (if integrated)
- [ ] Monitor Firebase Analytics
- [ ] Review user feedback
- [ ] Plan next iteration based on metrics

---

## Troubleshooting Guide

### Common Issues

**Issue:** App crashes on launch  
**Solution:** Check Firebase configuration, ensure google-services.json is present

**Issue:** Authentication fails  
**Solution:** Verify Firebase Auth is enabled, check network connectivity

**Issue:** Events not loading  
**Solution:** Check Firestore permissions, verify collection names match

**Issue:** Notifications not showing  
**Solution:** Check notification permissions (Android 13+), verify channel creation

**Issue:** Theme not applying  
**Solution:** Check SharedPreferences for theme setting, verify dark mode logic

---

## Version History

- **v1.0** - Initial MVP release
  - Authentication (email/password)
  - Events management
  - Mentorship system
  - Basic UI with Material 3
  - Dark/light theme support

---

## Contact & Support

For technical questions or contributions, refer to:
- `CONTRIBUTING.md`
- `CODE_OF_CONDUCT.md`
- `SECURITY.md`

---

**Last Updated:** November 19, 2025  
**Document Version:** 1.0

