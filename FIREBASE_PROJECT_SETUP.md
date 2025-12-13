# Firebase Project Setup Instructions

## Current Situation

Your Android app is configured to use Firebase project: **campusconnect-c2b32**
However, this project is not accessible via your Firebase CLI login.

You have access to these Firebase projects:
- campusconnect-b0fc2
- chatroomapp-dc264

## Option 1: Use Existing Project (Recommended)

Update your Android app to use the **campusconnect-b0fc2** project:

### Step 1: Update .firebaserc
```powershell
cd D:\AndroidStudioProjects\CampusConnect
Set-Content -Path .firebaserc -Value '{"projects":{"default":"campusconnect-b0fc2"}}'
```

### Step 2: Download new google-services.json
1. Go to Firebase Console: https://console.firebase.google.com/project/campusconnect-b0fc2
2. Click on Project Settings (gear icon)
3. Scroll to "Your apps" section
4. If the Android app exists, download `google-services.json`
5. If not, click "Add app" → Android → register with package name: `com.example.campusconnect`
6. Download the `google-services.json` file
7. Replace the file at: `D:\AndroidStudioProjects\CampusConnect\app\google-services.json`

### Step 3: Enable Required Firebase Services
In Firebase Console for campusconnect-b0fc2:
1. **Authentication**: Enable Email/Password sign-in
2. **Firestore Database**: Create database (start in test mode for now)
3. **Cloud Functions**: Will be enabled automatically on first deploy

### Step 4: Deploy Firestore Rules
```powershell
cd D:\AndroidStudioProjects\CampusConnect
firebase deploy --only firestore:rules
```

### Step 5: Set Admin Code Configuration
```powershell
firebase functions:config:set campus.admin_code="YOUR_SECURE_ADMIN_CODE"
firebase functions:config:set campus.default_admin_roles="admin,event:create,notes:upload"
```

### Step 6: Deploy Cloud Functions
```powershell
firebase deploy --only functions
```

## Option 2: Get Access to campusconnect-c2b32

If you want to keep using the current project (campusconnect-c2b32):

### Step 1: Verify Project Ownership
1. Go to: https://console.firebase.google.com/project/campusconnect-c2b32
2. Check if you can access it (you may need to be added as a collaborator)

### Step 2: Add to Firebase CLI
If you have access but CLI doesn't see it:
```powershell
firebase login --reauth
```

Then try again:
```powershell
firebase projects:list
```

### Step 3: If Still Not Listed
The project might belong to a different Google account. You'll need to:
1. Login with the correct Google account, OR
2. Be added as a project member by the project owner

## Quick Start Commands (After Choosing Option 1)

```powershell
# Set to use campusconnect-b0fc2
cd D:\AndroidStudioProjects\CampusConnect
firebase use campusconnect-b0fc2

# Set admin code (replace with your secure code)
firebase functions:config:set campus.admin_code="MySecureAdminCode2025!"
firebase functions:config:set campus.default_admin_roles="admin,event:create,notes:upload"

# Deploy functions
firebase deploy --only functions

# Or deploy everything
firebase deploy
```

## Verification

After deployment, verify:
1. ✅ Functions deployed: `firebase functions:list`
2. ✅ Config set: `firebase functions:config:get`
3. ✅ Firestore rules deployed: Check Firebase Console → Firestore Database → Rules
4. ✅ Test from Android app

## Need Help?

Run from project root:
```powershell
# See current project
firebase use

# List all projects
firebase projects:list

# Get function logs
firebase functions:log

# Check function config
firebase functions:config:get
```

