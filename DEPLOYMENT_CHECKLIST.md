# Deployment Checklist for CampusConnect

## Pre-Deployment Verification ✅

### 1. Build Status
- [x] Build compiles successfully
- [x] No compilation errors
- [x] All dependencies resolved
- [x] Hilt code generation complete

### 2. Testing
- [x] Unit tests passing (AuthViewModelTest)
- [ ] Integration tests (TODO: Add more tests)
- [ ] UI tests (TODO: Add Espresso tests)
- [ ] Manual testing on emulator/device

### 3. Configuration Files
- [x] `google-services.json` present in app/
- [x] Cloudinary credentials configured in `CloudinaryConfig.kt`
- [ ] `serviceAccountKey.json` placed for Firestore deployment
- [x] `firestore.rules` ready for deployment

### 4. Security
- [x] Firestore rules with RBAC
- [x] Admin role checks in code
- [x] User data protection (users can only edit own data)
- [x] Event/Note ownership validation
- [ ] TODO: Move Cloudinary credentials to env vars or Remote Config

### 5. Architecture
- [x] Dependency Injection (Hilt)
- [x] Repository pattern
- [x] Room database (offline-first)
- [x] UiState for error handling
- [x] ViewModels with proper scoping

---

## Deployment Steps

### Step 1: Deploy Firestore Rules

```powershell
# Ensure you have Firebase CLI installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Deploy rules
firebase deploy --only firestore:rules
```

### Step 2: Set Up Admin User

```powershell
# Navigate to scripts directory
cd scripts

# Run admin setup script with a user email
node setCustomClaims.js user@example.com --admin
```

### Step 3: Build Release APK

```powershell
# Build release APK (requires signing config)
./gradlew :app:assembleRelease

# Or debug APK for testing
./gradlew :app:assembleDebug
```

### Step 4: Install on Device

```powershell
# Connect device or start emulator
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.example.campusconnect/.MainActivity
```

### Step 5: Verify Core Features

1. **Authentication**
   - [ ] Sign up new user
   - [ ] Sign in existing user
   - [ ] Profile creation flow
   - [ ] Session persistence

2. **Events**
   - [ ] View events list
   - [ ] Create event (admin only)
   - [ ] Register for event
   - [ ] View event details

3. **Notes**
   - [ ] Browse notes by semester
   - [ ] Filter by subject
   - [ ] Upload PDF note (with role)
   - [ ] Download note
   - [ ] Delete own note

4. **Profile**
   - [ ] View profile
   - [ ] Edit profile
   - [ ] Update avatar
   - [ ] View activity log

5. **Offline Mode**
   - [ ] View cached notes offline
   - [ ] View cached events offline
   - [ ] Sync when back online

---

## Production Readiness Checklist

### High Priority
- [ ] **ProGuard/R8** - Enable code minification and obfuscation
- [ ] **Signing Config** - Configure release signing
- [ ] **Version Code/Name** - Set proper versioning
- [ ] **Crash Reporting** - Add Firebase Crashlytics
- [ ] **Analytics** - Add Firebase Analytics
- [ ] **Performance Monitoring** - Add Firebase Performance

### Medium Priority
- [ ] **API Keys Security** - Move to BuildConfig or Remote Config
- [ ] **Network Security Config** - Add certificate pinning
- [ ] **App Size Optimization** - Enable resource shrinking
- [ ] **Multi-language Support** - Add string resources
- [ ] **Dark Mode** - Implement theme switching
- [ ] **Backup** - Configure Android Auto Backup

### Low Priority
- [ ] **App Shortcuts** - Add launcher shortcuts
- [ ] **Widgets** - Add home screen widgets
- [ ] **Notifications** - Push notifications for events
- [ ] **Deep Links** - Handle deep linking
- [ ] **App Indexing** - Firebase App Indexing

---

## Performance Optimization

### Current Status
- Build time: ~13-30s (incremental)
- APK size: Not optimized
- Memory usage: Not profiled
- Network calls: Not optimized (no pagination)

### TODO
1. **Enable R8**
   ```gradle
   buildTypes {
       release {
           minifyEnabled true
           proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
       }
   }
   ```

2. **Add Pagination**
   - Implement Paging 3 for notes list
   - Lazy loading for events
   - Cursor-based pagination for Firestore

3. **Image Optimization**
   - Use Coil or Glide for image loading
   - Configure Cloudinary transformations
   - Cache transformed images

4. **Database Optimization**
   - Add indices on frequently queried fields
   - Implement database migrations properly
   - Monitor query performance

---

## Monitoring Setup

### Firebase Console
1. Navigate to: https://console.firebase.google.com
2. Select your project
3. Enable:
   - Authentication (Email/Password provider)
   - Firestore Database
   - Storage (for future file uploads)
   - Crashlytics
   - Analytics
   - Performance Monitoring

### Firestore Indexes
Check if composite indexes are needed:
```
firestore.indexes.json
```

---

## Post-Deployment Verification

### Smoke Tests
1. Install fresh APK on clean device
2. Sign up new account
3. Create profile
4. Upload a note (if admin)
5. Register for event
6. Check offline functionality
7. Verify sync after reconnect

### Monitoring
- Check Firebase Console for:
  - User authentication events
  - Firestore read/write counts
  - Crashlytics for any crashes
  - Performance metrics

---

## Rollback Plan

If critical issues are found:

1. **Disable features via Remote Config**
   ```kotlin
   // Add Remote Config checks before feature access
   if (remoteConfig.getBoolean("notes_upload_enabled")) {
       // Show upload button
   }
   ```

2. **Revert Firestore Rules**
   ```powershell
   firebase deploy --only firestore:rules --config firebase-backup.json
   ```

3. **Push hotfix APK**
   - Increment version code
   - Deploy critical fix
   - Notify users to update

---

## Contact & Support

- **Developer:** Your Name
- **Firebase Project:** campus-connect-xxxxx
- **GitHub Repo:** (if applicable)
- **Issue Tracker:** (if applicable)

---

## Notes

- Last deployment: [DATE]
- Version: 1.0.0 (Phase 1 Complete)
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)

---

**Status:** ✅ Ready for deployment (internal testing)  
**Production Ready:** ⚠️ Pending additional configuration (ProGuard, signing, analytics)

