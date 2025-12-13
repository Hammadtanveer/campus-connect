# 🚀 Quick Reference - Admin Access Setup

## Common Commands (Copy & Paste)

### From Project Root Directory

```powershell
# Navigate to project root
cd D:\AndroidStudioProjects\CampusConnect

# Check Firebase login
firebase login

# List accessible projects
firebase projects:list

# Set project (use campusconnect-b0fc2 if campusconnect-c2b32 not accessible)
firebase use campusconnect-b0fc2

# Set admin code (CHANGE THIS!)
firebase functions:config:set campus.admin_code="YourSecureCode123!"
firebase functions:config:set campus.default_admin_roles="admin,event:create,notes:upload"

# Verify config
firebase functions:config:get

# Deploy functions
firebase deploy --only functions

# View logs
firebase functions:log

# Run automated deploy script
.\deploy-functions.ps1
```

## Your Firebase Projects

Available to you:
- ✅ **campusconnect-b0fc2** (recommended)
- ✅ **chatroomapp-dc264**

Referenced in google-services.json but not accessible:
- ⚠️ **campusconnect-c2b32** (need access or switch projects)

## Admin Code Best Practices

❌ **Don't Use**:
- CAMPUS_ADMIN_2025 (default, insecure)
- admin
- 12345678
- Anything under 16 characters

✅ **Do Use**:
- Minimum 16 characters
- Mix of uppercase, lowercase, numbers, symbols
- Example: `Adm!n#2025$Campus@Secure`

## Testing Checklist

### After Deployment:
1. ✅ Go to Firebase Console → Functions
2. ✅ Verify `requestAdminAccess` function exists
3. ✅ Run Android app
4. ✅ Register with admin code OR call server function
5. ✅ Check Admin Panel shows admin access
6. ✅ Verify Firestore user doc has `isAdmin: true`

## File Locations

```
D:\AndroidStudioProjects\CampusConnect\
├── ADMIN_ACCESS_IMPLEMENTATION_SUMMARY.md  ← Full overview
├── CLOUD_FUNCTIONS_DEPLOYMENT_GUIDE.md     ← Detailed deploy guide
├── FIREBASE_PROJECT_SETUP.md               ← Project config help
├── deploy-functions.ps1                    ← Automated script
├── firebase.json                           ← Firebase config
├── .firebaserc                             ← Project alias
├── cloud-functions/
│   ├── index.js                            ← Function code
│   ├── package.json                        ← Dependencies
│   └── README.md                           ← Function docs
└── app/
    ├── build.gradle.kts                    ← Added firebase-functions-ktx
    ├── google-services.json                ← May need update
    └��─ src/main/java/.../
        ├── MainViewModel.kt                ← Added requestAdminAccessServer()
        └── ui/screens/AuthScreen.kt        ← Updated signup flow
```

## Troubleshooting Quick Fixes

### "Not in a Firebase app directory"
```powershell
# Run from project root, not cloud-functions folder
cd D:\AndroidStudioProjects\CampusConnect
firebase deploy --only functions
```

### "No currently active project"
```powershell
firebase use campusconnect-b0fc2
```

### npm EBADENGINE warning
- Safe to ignore (local Node 22, deploys to Node 18 runtime)

### Function not found in app
- Verify deployment: `firebase functions:list`
- Check project matches google-services.json
- Rebuild Android app

### Admin access not showing
1. Check Firestore user doc has `isAdmin: true`
2. Refresh claims: Sign out and sign in
3. Or use "Refresh Permissions" button in Admin Panel

## Firebase Console Links

- **Project**: https://console.firebase.google.com/project/campusconnect-b0fc2
- **Functions**: https://console.firebase.google.com/project/campusconnect-b0fc2/functions
- **Firestore**: https://console.firebase.google.com/project/campusconnect-b0fc2/firestore
- **Authentication**: https://console.firebase.google.com/project/campusconnect-b0fc2/authentication

## Need Help?

1. Read: `ADMIN_ACCESS_IMPLEMENTATION_SUMMARY.md`
2. Check: Firebase Console → Functions → Logs
3. Run: `firebase functions:log`
4. Verify: `firebase functions:config:get`

---

**Last Updated**: Implementation complete, ready to deploy!

