# CampusConnect Cloud Functions

This folder contains a Firebase Cloud Function to securely grant admin access to authenticated users when they provide a valid server-side admin code.

## Function: requestAdminAccess (callable)

- **Path**: `functions.requestAdminAccess`
- **Input**: `{ adminCode: string }`
- **Requirements**: Caller must be authenticated
- **Behavior**:
  - Validates the admin code against `functions.config().campus.admin_code` (falls back to 'CAMPUS_ADMIN_2025')
  - Sets custom claims on the user: `{ admin: true, roles: [ ... ] }`
  - Updates Firestore `users/{uid}` document with `isAdmin: true` and merged `roles`
  - Returns success message; client should refresh token (call `getIdToken(true)` or sign out/in) to pick up claims

## Installation

### Step 1: Install Dependencies

```powershell
cd cloud-functions
npm install
```

**Note about npm warnings**:
- `EBADENGINE` warning is safe to ignore (Node 22 works fine, function will deploy to Node 18 runtime)
- Deprecated packages are transitive dependencies from Firebase SDK (safe to ignore)
- Security vulnerabilities in dev dependencies can be fixed with: `npm audit fix`

### Step 2: Configure Admin Code (IMPORTANT - Security)

From the **project root directory** (not cloud-functions):

```powershell
cd D:\AndroidStudioProjects\CampusConnect

# Set secure admin code (DO NOT USE DEFAULT IN PRODUCTION!)
firebase functions:config:set campus.admin_code="YOUR_REAL_ADMIN_CODE"

# Set default admin roles
firebase functions:config:set campus.default_admin_roles="admin,event:create,notes:upload"

# Verify configuration
firebase functions:config:get
```

### Step 3: Deploy Functions

From the **project root directory**:

```powershell
cd D:\AndroidStudioProjects\CampusConnect
firebase deploy --only functions
```

Or use the automated script:

```powershell
cd D:\AndroidStudioProjects\CampusConnect
.\deploy-functions.ps1
```

## Troubleshooting

### Error: "Not in a Firebase app directory (could not locate firebase.json)"

**Cause**: Running `firebase deploy` from inside `cloud-functions/` folder.

**Solution**: Always run Firebase commands from the **project root**:

```powershell
# Wrong (from cloud-functions folder)
cd cloud-functions
firebase deploy --only functions  # ❌ ERROR

# Correct (from project root)
cd D:\AndroidStudioProjects\CampusConnect
firebase deploy --only functions  # ✅ WORKS
```

### npm EBADENGINE Warning

**Message**: `Unsupported engine { required: { node: '18' }, current: { node: 'v22.20.0' } }`

**Impact**: None - this is just a warning. Node 22 works fine for local development. The deployed function will use Node 18 runtime specified in `firebase.json`.

**To suppress**: Update `cloud-functions/package.json`:
```json
"engines": {
  "node": "18 || 20 || 22"
}
```

### npm Deprecated Packages

**Impact**: These are transitive dependencies from Firebase SDK. Safe to ignore unless they cause actual errors.

**Optional fix**:
```powershell
cd cloud-functions
npm audit fix
```

### Firebase Project Not Found

**Error**: `No currently active project`

**Solution**: See `FIREBASE_PROJECT_SETUP.md` in project root for detailed instructions.

Quick fix:
```powershell
cd D:\AndroidStudioProjects\CampusConnect
firebase use campusconnect-b0fc2  # Or your project ID
```

## Client-side Usage (Android / Kotlin)

### Method 1: Using MainViewModel Helper

```kotlin
// In your UI code (e.g., AdminPanelScreen)
viewModel.requestAdminAccessServer("YOUR_ADMIN_CODE") { success, error ->
    if (success) {
        // Admin access granted!
        // Navigate to admin panel or show success message
    } else {
        // Show error: error
    }
}
```

### Method 2: Direct Firebase Functions Call

```kotlin
val functions = Firebase.functions
val data = hashMapOf("adminCode" to "YOUR_ADMIN_CODE")
functions.getHttpsCallable("requestAdminAccess")
    .call(data)
    .addOnSuccessListener { result ->
        // Admin access granted. Refresh token to pick up claims.
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
            ?.addOnSuccessListener { token ->
                // Now token.claims contains admin:true and roles
            }
    }
    .addOnFailureListener { e ->
        // Handle error
    }
```

## Security Notes

### ⚠️ CRITICAL: Admin Code Security

1. **Never commit admin code to repository**
   - Use `firebase functions:config:set` (stores server-side)
   - Do NOT hardcode in `index.js`

2. **Use strong admin codes**
   - Minimum 16 characters
   - Mix uppercase, lowercase, numbers, symbols
   - Example: `Adm!n#2025$Secure%Code`

3. **Rotate codes periodically**
   ```powershell
   firebase functions:config:set campus.admin_code="NEW_SECURE_CODE"
   firebase deploy --only functions
   ```

4. **Monitor function logs**
   ```powershell
   firebase functions:log
   ```

### Additional Security Recommendations

- **Rate limiting**: Prevent brute force attempts
- **IP whitelisting**: Restrict to known networks
- **Audit logging**: Log all admin access grants
- **Two-factor verification**: Add additional validation step
- **Email domain restriction**: Only allow certain email domains

## Files in This Directory

- **index.js**: Cloud Function implementation
- **package.json**: Node.js dependencies and configuration
- **README.md**: This file
- **node_modules/**: Installed dependencies (not committed to git)

## Deployment Checklist

- [ ] `npm install` completed successfully
- [ ] Admin code configured via `firebase functions:config:set`
- [ ] Running deploy from **project root** directory
- [ ] Firebase project selected (`firebase use`)
- [ ] Deployment successful (`firebase deploy --only functions`)
- [ ] Function visible in Firebase Console
- [ ] Tested from Android app
- [ ] Admin Panel shows admin access

## Support & Documentation

- **Main implementation guide**: See `ADMIN_ACCESS_IMPLEMENTATION_SUMMARY.md` in project root
- **Deployment guide**: See `CLOUD_FUNCTIONS_DEPLOYMENT_GUIDE.md` in project root
- **Firebase setup**: See `FIREBASE_PROJECT_SETUP.md` in project root
- **Firebase Functions docs**: https://firebase.google.com/docs/functions
- **Custom Claims docs**: https://firebase.google.com/docs/auth/admin/custom-claims

