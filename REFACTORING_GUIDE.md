# CampusConnect - Refactoring Guide

## 🎯 Objective

Transform the current MVP into a production-ready, scalable application by addressing technical debt, improving architecture, and completing placeholder features.

---

## 📊 Current State Analysis

### Strengths ✅
- Modern tech stack (Jetpack Compose, Kotlin, Firebase)
- Working authentication and user management
- Functional events system with real-time updates
- Complete mentorship flow
- Reactive architecture with Flow and State

### Weaknesses ⚠️
- Monolithic ViewModel (994 lines)
- No dependency injection framework
- Missing offline support
- Incomplete features (Notes, Societies, Placement)
- Limited error handling
- No pagination
- Hard-coded data

---

## 🗺️ Refactoring Roadmap

### Phase 1: Architecture Cleanup (2-3 weeks)
**Priority:** HIGH  
**Goal:** Separate concerns, implement DI, create proper layering

### Phase 2: Feature Completion (3-4 weeks)
**Priority:** MEDIUM  
**Goal:** Complete all MVP features from PRD

### Phase 3: Optimization & Polish (2-3 weeks)
**Priority:** MEDIUM  
**Goal:** Add offline support, pagination, testing

### Phase 4: Advanced Features (4-6 weeks)
**Priority:** LOW  
**Goal:** Q&A forums, chat, advanced search

---

## 📋 Phase 1: Architecture Cleanup

### 1.1 Implement Hilt Dependency Injection

**Current:** Manual object creation, no DI  
**Target:** Hilt-based DI with proper scoping

#### Step 1: Add Hilt Dependencies

```kotlin
// build.gradle.kts (Project level)
plugins {
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

// build.gradle.kts (App level)
plugins {
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
}
```

#### Step 2: Create Application Class

```kotlin
// CampusConnectApp.kt
@HiltAndroidApp
class CampusConnectApp : Application()
```

#### Step 3: Update AndroidManifest.xml

```xml
<application
    android:name=".CampusConnectApp"
    ...>
```

#### Step 4: Create DI Modules

```kotlin
// di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideUserRepository(
        auth: FirebaseAuth,
        db: FirebaseFirestore
    ): UserRepository = UserRepositoryImpl(auth, db)
    
    @Provides
    @Singleton
    fun provideEventsRepository(
        db: FirebaseFirestore
    ): EventsRepository = EventsRepository(db)
    
    @Provides
    @Singleton
    fun provideMentorshipRepository(
        auth: FirebaseAuth,
        db: FirebaseFirestore
    ): MentorshipRepository = MentorshipRepositoryImpl(auth, db)
    
    @Provides
    @Singleton
    fun provideNotesRepository(
        db: FirebaseFirestore,
        storage: FirebaseStorage
    ): NotesRepository = NotesRepositoryImpl(db, storage)
}
```

---

### 1.2 Split MainViewModel into Feature ViewModels

#### Create Feature-Specific ViewModels

**File: AuthViewModel.kt**
```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState = _authState.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            userRepository.signIn(email, password)
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _userProfile.value = result.data
                            _authState.value = AuthState.Authenticated
                        }
                        is Resource.Error -> {
                            _authState.value = AuthState.Error(result.message)
                        }
                        is Resource.Loading -> {
                            _authState.value = AuthState.Loading
                        }
                    }
                }
        }
    }
    
    fun register(
        email: String,
        password: String,
        displayName: String,
        course: String,
        branch: String,
        year: String,
        bio: String = ""
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            userRepository.register(email, password, displayName, course, branch, year, bio)
                .collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _userProfile.value = result.data
                            _authState.value = AuthState.Authenticated
                        }
                        is Resource.Error -> {
                            _authState.value = AuthState.Error(result.message)
                        }
                        is Resource.Loading -> {
                            _authState.value = AuthState.Loading
                        }
                    }
                }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
            _userProfile.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String?) : AuthState()
}
```

**File: EventsViewModel.kt**
```kotlin
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val authRepository: UserRepository
) : ViewModel() {
    
    private val _eventsState = MutableStateFlow<EventsState>(EventsState.Loading)
    val eventsState = _eventsState.asStateFlow()
    
    private val _selectedEvent = MutableStateFlow<OnlineEvent?>(null)
    val selectedEvent = _selectedEvent.asStateFlow()
    
    init {
        loadEvents()
    }
    
    private fun loadEvents() {
        viewModelScope.launch {
            eventsRepository.observeEvents()
                .collect { result ->
                    _eventsState.value = when (result) {
                        is Resource.Success -> EventsState.Success(result.data)
                        is Resource.Error -> EventsState.Error(result.message ?: "Unknown error")
                        is Resource.Loading -> EventsState.Loading
                    }
                }
        }
    }
    
    fun createEvent(
        title: String,
        description: String,
        dateTime: Timestamp,
        durationMinutes: Long,
        category: EventCategory,
        maxParticipants: Int = 0,
        meetLink: String = ""
    ) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val userName = authRepository.getCurrentUserName() ?: ""
            
            eventsRepository.createEvent(
                title, description, dateTime, durationMinutes,
                userId, userName, category, maxParticipants, meetLink
            ).collect { /* Handle result */ }
        }
    }
    
    fun registerForEvent(eventId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            eventsRepository.registerForEvent(userId, eventId)
                .collect { /* Handle result */ }
        }
    }
    
    fun loadEventById(eventId: String) {
        viewModelScope.launch {
            eventsRepository.getEventById(eventId)
                .collect { result ->
                    if (result is Resource.Success) {
                        _selectedEvent.value = result.data
                    }
                }
        }
    }
}

sealed class EventsState {
    object Loading : EventsState()
    data class Success(val events: List<OnlineEvent>) : EventsState()
    data class Error(val message: String) : EventsState()
}
```

**File: MentorshipViewModel.kt**
```kotlin
@HiltViewModel
class MentorshipViewModel @Inject constructor(
    private val mentorshipRepository: MentorshipRepository,
    private val authRepository: UserRepository
) : ViewModel() {
    
    private val _mentorsState = MutableStateFlow<MentorsState>(MentorsState.Loading)
    val mentorsState = _mentorsState.asStateFlow()
    
    private val _myRequestsState = MutableStateFlow<RequestsState>(RequestsState.Loading)
    val myRequestsState = _myRequestsState.asStateFlow()
    
    private val _receivedRequestsState = MutableStateFlow<RequestsState>(RequestsState.Loading)
    val receivedRequestsState = _receivedRequestsState.asStateFlow()
    
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount = _pendingCount.asStateFlow()
    
    fun loadMentors() {
        viewModelScope.launch {
            mentorshipRepository.loadMentors()
                .collect { result ->
                    _mentorsState.value = when (result) {
                        is Resource.Success -> MentorsState.Success(result.data)
                        is Resource.Error -> MentorsState.Error(result.message ?: "Unknown error")
                        is Resource.Loading -> MentorsState.Loading
                    }
                }
        }
    }
    
    fun sendRequest(mentorId: String, message: String) {
        viewModelScope.launch {
            mentorshipRepository.sendRequest(mentorId, message)
                .collect { /* Handle result */ }
        }
    }
    
    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            mentorshipRepository.acceptRequest(requestId)
                .collect { /* Handle result */ }
        }
    }
    
    fun loadReceivedRequests() {
        viewModelScope.launch {
            mentorshipRepository.getReceivedRequests()
                .collect { result ->
                    if (result is Resource.Success) {
                        _pendingCount.value = result.data.count { it.status == "pending" }
                        _receivedRequestsState.value = RequestsState.Success(result.data)
                    }
                }
        }
    }
}

sealed class MentorsState {
    object Loading : MentorsState()
    data class Success(val mentors: List<UserProfile>) : MentorsState()
    data class Error(val message: String) : MentorsState()
}

sealed class RequestsState {
    object Loading : RequestsState()
    data class Success(val requests: List<MentorshipRequest>) : RequestsState()
    data class Error(val message: String) : RequestsState()
}
```

**File: ProfileViewModel.kt**
```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState = _profileState.asStateFlow()
    
    private val _userActivities = MutableStateFlow<List<UserActivity>>(emptyList())
    val userActivities = _userActivities.asStateFlow()
    
    fun loadProfile() {
        viewModelScope.launch {
            userRepository.getCurrentUserProfile()
                .collect { result ->
                    _profileState.value = when (result) {
                        is Resource.Success -> ProfileState.Success(result.data)
                        is Resource.Error -> ProfileState.Error(result.message ?: "Unknown error")
                        is Resource.Loading -> ProfileState.Loading
                    }
                }
        }
    }
    
    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            userRepository.updateProfile(updatedProfile)
                .collect { /* Handle result */ }
        }
    }
    
    fun updateMentorProfile(bio: String, expertise: List<String>, status: String) {
        viewModelScope.launch {
            userRepository.updateMentorProfile(bio, expertise, status)
                .collect { /* Handle result */ }
        }
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}
```

---

### 1.3 Create Repository Layer

**File: repository/UserRepository.kt**
```kotlin
interface UserRepository {
    suspend fun signIn(email: String, password: String): Flow<Resource<UserProfile>>
    suspend fun register(
        email: String, password: String, displayName: String,
        course: String, branch: String, year: String, bio: String
    ): Flow<Resource<UserProfile>>
    suspend fun signOut()
    suspend fun getCurrentUserProfile(): Flow<Resource<UserProfile>>
    suspend fun updateProfile(profile: UserProfile): Flow<Resource<Unit>>
    suspend fun updateMentorProfile(
        bio: String, expertise: List<String>, status: String
    ): Flow<Resource<Unit>>
    fun getCurrentUserId(): String?
    fun getCurrentUserName(): String?
}

class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : UserRepository {
    
    override suspend fun signIn(email: String, password: String) = callbackFlow {
        trySend(Resource.Loading)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val profile = doc.toObject(UserProfile::class.java)
                            if (profile != null) {
                                trySend(Resource.Success(profile))
                            } else {
                                trySend(Resource.Error("Profile not found"))
                            }
                            close()
                        }
                        .addOnFailureListener { e ->
                            trySend(Resource.Error(e.message))
                            close()
                        }
                } else {
                    trySend(Resource.Error("No user ID"))
                    close()
                }
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        awaitClose { }
    }
    
    override suspend fun register(
        email: String, password: String, displayName: String,
        course: String, branch: String, year: String, bio: String
    ) = callbackFlow {
        trySend(Resource.Loading)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    val profile = UserProfile(
                        id = uid,
                        displayName = displayName,
                        email = email,
                        course = course,
                        branch = branch,
                        year = year,
                        bio = bio
                    )
                    db.collection("users").document(uid).set(profile)
                        .addOnSuccessListener {
                            trySend(Resource.Success(profile))
                            close()
                        }
                        .addOnFailureListener { e ->
                            trySend(Resource.Error(e.message))
                            close()
                        }
                } else {
                    trySend(Resource.Error("No user ID"))
                    close()
                }
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        awaitClose { }
    }
    
    override suspend fun signOut() {
        auth.signOut()
    }
    
    override suspend fun getCurrentUserProfile() = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose { }
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        trySend(Resource.Success(profile))
                    }
                }
            }
        awaitClose { registration.remove() }
    }
    
    override suspend fun updateProfile(profile: UserProfile) = callbackFlow {
        trySend(Resource.Loading)
        db.collection("users").document(profile.id).set(profile)
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        awaitClose { }
    }
    
    override suspend fun updateMentorProfile(
        bio: String, expertise: List<String>, status: String
    ) = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose { }
            return@callbackFlow
        }
        val updates = mapOf(
            "isMentor" to true,
            "mentorshipBio" to bio,
            "expertise" to expertise,
            "mentorshipStatus" to status
        )
        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        awaitClose { }
    }
    
    override fun getCurrentUserId(): String? = auth.currentUser?.uid
    override fun getCurrentUserName(): String? = auth.currentUser?.displayName
}
```

**File: repository/MentorshipRepository.kt**
```kotlin
interface MentorshipRepository {
    suspend fun loadMentors(): Flow<Resource<List<UserProfile>>>
    suspend fun sendRequest(mentorId: String, message: String): Flow<Resource<Unit>>
    suspend fun getMyRequests(): Flow<Resource<List<MentorshipRequest>>>
    suspend fun getReceivedRequests(): Flow<Resource<List<MentorshipRequest>>>
    suspend fun acceptRequest(requestId: String): Flow<Resource<Unit>>
    suspend fun rejectRequest(requestId: String): Flow<Resource<Unit>>
    suspend fun getMyConnections(): Flow<Resource<List<UserProfile>>>
    suspend fun removeConnection(otherUserId: String): Flow<Resource<Unit>>
}

class MentorshipRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : MentorshipRepository {
    // Implementation similar to methods in MainViewModel
    // Extract mentorship-related methods from MainViewModel
}
```

---

### 1.4 Update Compose Screens to Use New ViewModels

**Example: Update EventsListScreen.kt**
```kotlin
@Composable
fun EventsListScreen(
    viewModel: EventsViewModel = hiltViewModel(),
    navController: NavController
) {
    val eventsState by viewModel.eventsState.collectAsState()
    
    when (eventsState) {
        is EventsState.Loading -> {
            LoadingIndicator()
        }
        is EventsState.Success -> {
            val events = (eventsState as EventsState.Success).events
            EventsList(events = events, onEventClick = { eventId ->
                navController.navigate("event/$eventId")
            })
        }
        is EventsState.Error -> {
            ErrorView(message = (eventsState as EventsState.Error).message)
        }
    }
}
```

**Example: Update AuthScreen.kt**
```kotlin
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    startInRegister: Boolean = false
) {
    val authState by viewModel.authState.collectAsState()
    
    when (authState) {
        is AuthState.Authenticated -> {
            // Navigate to main app
        }
        is AuthState.Unauthenticated -> {
            // Show login/register UI
        }
        is AuthState.Loading -> {
            LoadingIndicator()
        }
        is AuthState.Error -> {
            val error = (authState as AuthState.Error).message
            // Show error
        }
    }
}
```

---

## 📋 Phase 2: Feature Completion

### 2.1 Complete Notes Module

#### Create Data Models

```kotlin
data class Note(
    val id: String = "",
    val title: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val uploaderId: String = "",
    val uploaderName: String = "",
    val fileUrl: String = "",
    val fileType: String = "", // pdf, image, doc
    val fileSizeBytes: Long = 0,
    val semester: String = "",
    val branch: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val upvotes: Int = 0,
    val downloads: Int = 0,
    val createdAt: Timestamp? = null,
    val thumbnailUrl: String = ""
)

data class Course(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val semester: String = "",
    val branch: String = "",
    val department: String = ""
)
```

#### Create NotesRepository

```kotlin
interface NotesRepository {
    suspend fun uploadNote(
        note: Note,
        fileUri: Uri,
        onProgress: (Float) -> Unit
    ): Flow<Resource<String>> // returns download URL
    
    suspend fun getNotesByCourse(courseId: String): Flow<Resource<List<Note>>>
    suspend fun getNotesBySemester(semester: String): Flow<Resource<List<Note>>>
    suspend fun searchNotes(query: String): Flow<Resource<List<Note>>>
    suspend fun upvoteNote(noteId: String): Flow<Resource<Unit>>
    suspend fun downloadNote(noteId: String, downloadUrl: String): Flow<Resource<Uri>>
    suspend fun deleteNote(noteId: String): Flow<Resource<Unit>>
}
```

#### Create NotesViewModel

```kotlin
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val authRepository: UserRepository
) : ViewModel() {
    
    private val _notesState = MutableStateFlow<NotesState>(NotesState.Loading)
    val notesState = _notesState.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress = _uploadProgress.asStateFlow()
    
    fun loadNotesBySemester(semester: String) {
        viewModelScope.launch {
            notesRepository.getNotesBySemester(semester)
                .collect { result ->
                    _notesState.value = when (result) {
                        is Resource.Success -> NotesState.Success(result.data)
                        is Resource.Error -> NotesState.Error(result.message ?: "Unknown error")
                        is Resource.Loading -> NotesState.Loading
                    }
                }
        }
    }
    
    fun uploadNote(note: Note, fileUri: Uri) {
        viewModelScope.launch {
            notesRepository.uploadNote(note, fileUri) { progress ->
                _uploadProgress.value = progress
            }.collect { /* Handle result */ }
        }
    }
    
    fun searchNotes(query: String) {
        viewModelScope.launch {
            notesRepository.searchNotes(query)
                .collect { result ->
                    _notesState.value = when (result) {
                        is Resource.Success -> NotesState.Success(result.data)
                        is Resource.Error -> NotesState.Error(result.message ?: "Unknown error")
                        is Resource.Loading -> NotesState.Loading
                    }
                }
        }
    }
}

sealed class NotesState {
    object Loading : NotesState()
    data class Success(val notes: List<Note>) : NotesState()
    data class Error(val message: String) : NotesState()
}
```

#### Update NotesView UI

```kotlin
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = hiltViewModel(),
    navController: NavController
) {
    var selectedSemester by remember { mutableStateOf("8th Sem") }
    val notesState by viewModel.notesState.collectAsState()
    
    LaunchedEffect(selectedSemester) {
        viewModel.loadNotesBySemester(selectedSemester)
    }
    
    Column {
        SemesterSelector(
            selectedSemester = selectedSemester,
            onSemesterSelected = { selectedSemester = it }
        )
        
        when (notesState) {
            is NotesState.Loading -> LoadingIndicator()
            is NotesState.Success -> {
                NotesList(notes = (notesState as NotesState.Success).notes)
            }
            is NotesState.Error -> {
                ErrorView(message = (notesState as NotesState.Error).message)
            }
        }
        
        FloatingActionButton(
            onClick = { navController.navigate("notes/upload") }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload Note")
        }
    }
}
```

---

### 2.2 Complete Societies Module

#### Create Data Models

```kotlin
data class Society(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "", // technical, cultural, sports, etc.
    val logoUrl: String = "",
    val coverImageUrl: String = "",
    val admins: List<String> = emptyList(), // user IDs
    val members: List<String> = emptyList(),
    val followersCount: Int = 0,
    val eventsCount: Int = 0,
    val createdAt: Timestamp? = null,
    val socialLinks: Map<String, String> = emptyMap() // instagram, linkedin, etc.
)

data class SocietyPost(
    val id: String = "",
    val societyId: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Timestamp? = null,
    val likes: Int = 0,
    val comments: Int = 0
)
```

#### Create SocietiesRepository

```kotlin
interface SocietiesRepository {
    suspend fun getAllSocieties(): Flow<Resource<List<Society>>>
    suspend fun getSocietyById(id: String): Flow<Resource<Society>>
    suspend fun followSociety(societyId: String): Flow<Resource<Unit>>
    suspend fun unfollowSociety(societyId: String): Flow<Resource<Unit>>
    suspend fun getMyFollowedSocieties(): Flow<Resource<List<Society>>>
    suspend fun createPost(post: SocietyPost): Flow<Resource<Unit>>
    suspend fun getSocietyPosts(societyId: String): Flow<Resource<List<SocietyPost>>>
}
```

---

### 2.3 Implement Placement Module

#### Create Data Models

```kotlin
data class JobPosting(
    val id: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val companyLogo: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "", // internship, full-time, part-time
    val location: String = "",
    val salary: String = "",
    val eligibilityCriteria: Map<String, String> = emptyMap(),
    val skillsRequired: List<String> = emptyList(),
    val applicationDeadline: Timestamp? = null,
    val postedAt: Timestamp? = null,
    val applicationLink: String = ""
)

data class Company(
    val id: String = "",
    val name: String = "",
    val logo: String = "",
    val description: String = "",
    val industry: String = "",
    val website: String = "",
    val linkedIn: String = ""
)

data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val userId: String = "",
    val status: String = "", // applied, shortlisted, rejected, selected
    val appliedAt: Timestamp? = null,
    val resumeUrl: String = ""
)
```

---

## 📋 Phase 3: Optimization & Polish

### 3.1 Add Room Database for Offline Support

#### Add Dependencies

```kotlin
dependencies {
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
}
```

#### Create Database Entities

```kotlin
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dateTime: Long,
    val organizerId: String,
    val category: String,
    val isSynced: Boolean = false
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val isSynced: Boolean = false
)
```

#### Create DAOs

```kotlin
@Dao
interface EventsDao {
    @Query("SELECT * FROM events ORDER BY dateTime DESC")
    fun getAllEvents(): Flow<List<EventEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
    
    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: String)
}
```

#### Update Repositories to Use Room

```kotlin
class EventsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val eventsDao: EventsDao,
    private val networkUtils: NetworkUtils
) : EventsRepository {
    
    override suspend fun observeEvents() = callbackFlow {
        // Try to emit cached data first
        eventsDao.getAllEvents().collect { cachedEvents ->
            if (cachedEvents.isNotEmpty()) {
                trySend(Resource.Success(cachedEvents.map { it.toOnlineEvent() }))
            }
        }
        
        // Then fetch from network if available
        if (networkUtils.isNetworkAvailable()) {
            val registration = db.collection("events")
                .addSnapshotListener { snapshot, error ->
                    // ... Firestore logic
                    // Also cache to Room
                    viewModelScope.launch {
                        snapshot?.documents?.forEach { doc ->
                            val event = doc.toObject(OnlineEvent::class.java)
                            event?.let {
                                eventsDao.insertEvent(it.toEntity())
                            }
                        }
                    }
                }
            awaitClose { registration.remove() }
        } else {
            awaitClose { }
        }
    }
}
```

---

### 3.2 Implement Pagination

#### Add Paging Dependencies

```kotlin
dependencies {
    implementation("androidx.paging:paging-runtime:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")
}
```

#### Create Paging Source

```kotlin
class EventsPagingSource(
    private val db: FirebaseFirestore
) : PagingSource<QuerySnapshot, OnlineEvent>() {
    
    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, OnlineEvent> {
        return try {
            val currentPage = params.key ?: db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())
                .get()
                .await()
            
            val lastVisible = currentPage.documents.lastOrNull()
            val nextPage = if (lastVisible != null) {
                db.collection("events")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(params.loadSize.toLong())
                    .get()
                    .await()
            } else null
            
            LoadResult.Page(
                data = currentPage.toObjects(OnlineEvent::class.java),
                prevKey = null,
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<QuerySnapshot, OnlineEvent>): QuerySnapshot? = null
}
```

#### Use in ViewModel

```kotlin
val eventsPagingFlow: Flow<PagingData<OnlineEvent>> = Pager(
    PagingConfig(pageSize = 20)
) {
    EventsPagingSource(db)
}.flow.cachedIn(viewModelScope)
```

#### Use in UI

```kotlin
@Composable
fun EventsListScreen(viewModel: EventsViewModel = hiltViewModel()) {
    val eventsFlow = viewModel.eventsPagingFlow.collectAsLazyPagingItems()
    
    LazyColumn {
        items(eventsFlow) { event ->
            event?.let { EventCard(event = it) }
        }
    }
}
```

---

### 3.3 Add Comprehensive Testing

#### Unit Tests Example

```kotlin
@HiltAndroidTest
class EventsViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var eventsRepository: EventsRepository
    
    private lateinit var viewModel: EventsViewModel
    
    @Before
    fun setup() {
        hiltRule.inject()
        viewModel = EventsViewModel(eventsRepository, mockAuthRepository)
    }
    
    @Test
    fun `loadEvents should update state to Success`() = runTest {
        // Given
        val mockEvents = listOf(
            OnlineEvent(id = "1", title = "Test Event")
        )
        coEvery { eventsRepository.observeEvents() } returns flowOf(Resource.Success(mockEvents))
        
        // When
        viewModel.loadEvents()
        
        // Then
        val state = viewModel.eventsState.value
        assertTrue(state is EventsState.Success)
        assertEquals(1, (state as EventsState.Success).events.size)
    }
}
```

---

## 🎯 Migration Checklist

### Pre-Migration
- [ ] Create feature branch for refactoring
- [ ] Document current functionality
- [ ] Create backup of working code
- [ ] Set up test environment

### Migration Steps
- [ ] Add Hilt dependencies
- [ ] Create Application class with @HiltAndroidApp
- [ ] Create DI modules
- [ ] Create repository interfaces
- [ ] Implement repositories
- [ ] Create feature ViewModels
- [ ] Update UI screens to use new ViewModels
- [ ] Test each feature after migration
- [ ] Remove old MainViewModel code
- [ ] Update navigation
- [ ] Test integration

### Post-Migration
- [ ] Run all tests
- [ ] Test on multiple devices
- [ ] Performance testing
- [ ] Code review
- [ ] Merge to main branch

---

## 📈 Expected Outcomes

### Code Quality
- **Reduced complexity:** ViewModels < 200 lines each
- **Testability:** 80%+ code coverage
- **Maintainability:** Clear separation of concerns

### Performance
- **Faster load times:** Cached data loads instantly
- **Reduced bandwidth:** Offline-first architecture
- **Better UX:** Pagination prevents loading huge lists

### Scalability
- **Easy to extend:** Add new features without modifying existing code
- **Team-friendly:** Multiple developers can work on different modules
- **Production-ready:** Proper error handling, testing, and offline support

---

**Document Version:** 1.0  
**Last Updated:** November 19, 2025

