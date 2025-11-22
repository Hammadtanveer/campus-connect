# Admin Access Guide - CampusConnect

## 🔐 How to Access Admin Features

### Current Admin System

The app uses **Firebase Custom Claims** to grant admin access. There are **two methods** to become an admin:

---

## Method 1: Admin Code During Registration (NEW - RECOMMENDED)

### Step 1: Use the Admin Code
When creating a new account, there's an optional "Admin Code" field.

**Default Admin Code:** `CAMPUS_ADMIN_2025`

### Step 2: Register with Admin Code
1. Open the app
2. Tap "Create account"
3. Fill in:
   - Display name
   - Email
   - Password
   - **Admin Code**: `CAMPUS_ADMIN_2025`
4. Tap "Sign Up"

### Step 3: Verify Admin Access
1. After registration, sign in
2. Go to **Profile** tab
3. You should see:
   - "You have admin privileges"
   - "Open Admin Panel" button

---

## Method 2: Using Node.js Scripts (Advanced)

If you already have an account or need to grant admin to existing users:

### Prerequisites
- Node.js installed
- Firebase service account key (`serviceAccountKey.json`)

### Step 1: Download Service Account Key
```powershell
# Follow DOWNLOAD_SERVICE_KEY.md instructions
# Place file at: D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json
```

### Step 2: Install Dependencies
```powershell
cd D:\AndroidStudioProjects\CampusConnect
npm install
```

### Step 3: Set Environment Variable
```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"
```

### Step 4: Run Setup Script
```powershell
.\scripts\setup-admin.ps1
```

The script will:
1. List all registered users
2. Let you select a user
3. Grant admin claims
4. Confirm success

### Step 5: Refresh Token
After granting admin claims:
1. **Sign out** from the app
2. **Sign in** again
3. Go to Profile → "Open Admin Panel" button appears

---

## Changing the Admin Code

### In Constants.kt
```kotlin
// File: app/src/main/java/com/example/campusconnect/util/Constants.kt

object Constants {
    // Change this to your custom code
    const val ADMIN_CODE = "YOUR_CUSTOM_CODE_HERE"
}
```

### Security Note
⚠️ The admin code in the app is **client-side only** and should be considered a convenience feature, not a security measure. For production:
- Use Firebase Cloud Functions to verify admin codes server-side
- Store admin codes in Firebase Remote Config
- Implement IP restrictions and rate limiting

---

## Admin Features

Once you have admin access:

### 📊 Admin Panel
- View all users
- Manage user roles
- View system statistics
- Moderate content

### 📝 Create Events
- Admins can create campus events
- Manage event details
- View RSVP lists

### 📚 Manage Notes
- View all uploaded notes
- Delete inappropriate content
- Monitor uploads

---

## Troubleshooting

### "Open Admin Panel" button not showing

**Solution:**
1. Sign out completely
2. Sign in again
3. Admin claims refresh on sign-in

### Admin code not working

**Check:**
1. Code is exactly `CAMPUS_ADMIN_2025` (case-sensitive)
2. No extra spaces
3. Re-build the app after changing Constants.kt

### Can't run Node.js scripts

**Check:**
1. Node.js is installed: `node --version`
2. Dependencies installed: `npm install`
3. Service account key exists at correct path
4. Environment variable is set correctly

---

## Security Best Practices

### For Development
✅ Use the default admin code
✅ Test with dummy accounts
✅ Don't share credentials

### For Production
❌ Never hardcode admin codes in client
✅ Use Firebase Cloud Functions for server-side verification
✅ Implement proper authentication flows
✅ Use Firebase Remote Config for dynamic admin codes
✅ Add audit logging for admin actions
✅ Implement role-based access control (RBAC)

---

## Quick Reference

| Method | Ease | Security | Best For |
|--------|------|----------|----------|
| Admin Code | ⭐⭐⭐⭐⭐ Easy | ⭐⭐ Low | Development/Testing |
| Node.js Scripts | ⭐⭐⭐ Medium | ⭐⭐⭐⭐ High | Production |
| Cloud Functions | ⭐⭐ Hard | ⭐⭐⭐⭐⭐ Highest | Enterprise |

---

## Next Steps

After getting admin access:
1. Read **ADMIN_PANEL_GUIDE.md** for usage instructions
2. Check **RBAC_GUIDE.md** for role management
3. Review **SECURITY.md** for security considerations

---

**Need Help?** Check the other documentation files or create an issue in the repository.

