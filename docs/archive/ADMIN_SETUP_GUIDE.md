# 🔧 Admin Access Setup - Step by Step Guide

## Prerequisites Checklist
- [x] npm installed (you already have this)
- [x] firebase-admin installed (you already have this)
- [ ] Firebase service account key downloaded
- [ ] Environment variable set correctly

---

## 📥 Step 1: Download Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **CampusConnect** project
3. Click the **⚙️ gear icon** → **Project Settings**
4. Go to the **Service Accounts** tab
5. Click **"Generate New Private Key"**
6. Click **"Generate Key"** in the confirmation dialog
7. Save the downloaded JSON file to a location like:
   - `D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json`
   - OR `C:\Firebase\campusconnect-service-account.json`
   - **⚠️ IMPORTANT**: Add this file to `.gitignore` - NEVER commit it to Git!

---

## 🔐 Step 2: Set Environment Variable

Open PowerShell and set the path to your downloaded key:

```powershell
# Replace with YOUR actual path where you saved the JSON file
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"

# Verify it's set correctly
echo $env:GOOGLE_APPLICATION_CREDENTIALS
```

**✅ Expected output**: The path you just set

---

## 👥 Step 3: Find Your User UID

Run the helper script to list all users:

```powershell
node scripts/listUsers.js
```

**Expected output**:
```
✅ Firebase Admin SDK initialized successfully

📋 Fetching all users...

Found 3 user(s):

================================================================================

1. USER DETAILS:
   UID:           abc123xyz789def456
   Email:         student@example.com
   Display Name:  John Student
   Created:       1/15/2024, 10:30:00 AM
   Last Sign In:  11/20/2025, 2:45:00 PM
   Custom Claims: None (Regular user)
--------------------------------------------------------------------------------

2. USER DETAILS:
   UID:           xyz789abc123ghi456
   Email:         admin@example.com
   Display Name:  Admin User
   Created:       1/10/2024, 9:00:00 AM
   Last Sign In:  11/20/2025, 3:00:00 PM
   Custom Claims: {"admin": true, "roles": ["event:create"]}
--------------------------------------------------------------------------------
```

**📝 Copy the UID** of the user you want to make admin

---

## 🎯 Step 4: Grant Admin Access

Now use the UID from Step 3 (remove the `<>` brackets and paste actual UID):

```powershell
# Example with a real UID (replace with your actual UID):
node scripts/setCustomClaims.js "abc123xyz789def456" admin event:create notes:upload

# For full admin with all permissions:
node scripts/setCustomClaims.js "abc123xyz789def456" admin event:create notes:upload senior:update society:manage

# For specific role only (e.g., only event creation):
node scripts/setCustomClaims.js "abc123xyz789def456" event:create
```

**✅ Expected output**:
```
Custom claims set successfully for user: abc123xyz789def456
Claims: {
  "admin": true,
  "roles": ["event:create", "notes:upload"]
}

✅ User now has admin privileges!
   The user must sign out and sign back in for changes to take effect.
```

---

## 🔄 Step 5: Apply Changes in App

The user **MUST** do one of these:

### Option A: Sign Out and Back In (Recommended)
1. Open the app
2. Go to Profile → Sign Out
3. Sign in again with the same credentials
4. Go to Profile → You should now see "Admin Tools" section
5. Click "Open Admin Panel"

### Option B: Use Refresh Button (If Already Admin)
1. Open the app
2. Go to Profile → Admin Tools → Open Admin Panel
3. Click "Refresh Permissions" button
4. Claims will update without re-login

---

## ❓ Troubleshooting

### Error: "The '<' operator is reserved for future use"
**Problem**: You used `<USER_UID>` literally instead of replacing it with actual UID

**Solution**: Replace `<USER_UID>` with the actual UID from Step 3:
```powershell
# ❌ WRONG:
node scripts/setCustomClaims.js <USER_UID> admin

# ✅ CORRECT:
node scripts/setCustomClaims.js "abc123xyz789def456" admin
```

### Error: "GOOGLE_APPLICATION_CREDENTIALS not set"
**Problem**: Environment variable not set or path is wrong

**Solution**: 
```powershell
# Set it again with correct path
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\YOUR\ACTUAL\PATH\serviceAccountKey.json"

# Verify
echo $env:GOOGLE_APPLICATION_CREDENTIALS
```

### Error: "Service account key file not found"
**Problem**: File doesn't exist at the path you specified

**Solution**:
1. Check if file exists: `Test-Path "D:\path\to\serviceAccountKey.json"`
2. If False, re-download from Firebase Console (Step 1)
3. Update environment variable with correct path

### "I don't see Admin Tools section after setting claims"
**Problem**: Claims haven't refreshed in the app

**Solution**:
1. **Force sign out**: Profile → Sign Out
2. **Close the app completely** (swipe away from recent apps)
3. **Reopen the app** and sign in again
4. Claims should now be active

### "listUsers.js shows 'No users found'"
**Problem**: 
- Wrong Firebase project selected
- Service account doesn't have permissions

**Solution**:
1. Verify you're using the correct service account key for CampusConnect project
2. In Firebase Console, check Service Account has "Firebase Admin SDK Administrator Service Agent" role

---

## 📋 Quick Reference Commands

```powershell
# 1. Set environment variable (do this FIRST)
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"

# 2. List all users to find UID
node scripts/listUsers.js

# 3. Set admin claims (replace UID with actual value from step 2)
node scripts/setCustomClaims.js "PASTE_ACTUAL_UID_HERE" admin event:create notes:upload

# 4. Verify claims were set
node scripts/listUsers.js
```

---

## 🎓 Understanding Roles

| Role | Permission | What User Can Do |
|------|-----------|------------------|
| `admin` | Full admin flag | Admin panel shows "Full Admin Access" |
| `event:create` | Create events | Can create and manage events |
| `notes:upload` | Upload notes | Can upload PDF notes |
| `senior:update` | Update seniors | Can edit senior profiles |
| `society:manage` | Manage societies | Can manage society pages |

**You can assign multiple roles at once**:
```powershell
node scripts/setCustomClaims.js "USER_UID" admin event:create notes:upload senior:update society:manage
```

---

## 🔒 Security Notes

1. **⚠️ NEVER commit `serviceAccountKey.json` to Git**
   - Add to `.gitignore` immediately
   - This file gives full admin access to your Firebase project

2. **Store the key securely**
   - Keep it outside your source code directory if possible
   - Use environment variables in production

3. **Limit access**
   - Only set admin claims for trusted users
   - Regularly audit who has admin access

4. **Backup**
   - Keep a backup of the service account key in a secure location
   - If lost, you'll need to generate a new one from Firebase Console

---

## ✅ Success Checklist

After completing all steps, verify:

- [ ] Service account key downloaded and saved securely
- [ ] Environment variable set and verified
- [ ] `listUsers.js` runs successfully and shows your users
- [ ] `setCustomClaims.js` runs without errors
- [ ] User signed out and back in
- [ ] "Admin Tools" section appears in Profile
- [ ] "Open Admin Panel" button works
- [ ] Admin Panel shows correct permissions

---

**Need Help?** Check the [ADMIN_PANEL_GUIDE.md](./ADMIN_PANEL_GUIDE.md) for more details about the admin panel features.

