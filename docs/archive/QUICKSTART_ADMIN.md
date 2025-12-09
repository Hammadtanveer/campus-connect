# 🚀 Quick Start: Grant Admin Access

## Option 1: Automated Script (Easiest) ⭐

Just run this single command and follow the prompts:

```powershell
# First, set your service account key path:
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"

# Then run the setup script:
.\scripts\setup-admin.ps1
```

The script will:
1. ✅ Check if everything is set up correctly
2. ✅ List all your Firebase users
3. ✅ Ask you to pick a user (paste their UID)
4. ✅ Let you choose permission level
5. ✅ Set the admin claims automatically

---

## Option 2: Manual Commands

### Step 1: Set Environment Variable
```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"
```

### Step 2: List Users
```powershell
node scripts/listUsers.js
```

Copy the UID you see (looks like `K9mxPqR7sVNtXyZ2aB4cD6fG`)

### Step 3: Set Admin Claims
```powershell
# Replace PASTE_UID_HERE with the actual UID from step 2
node scripts/setCustomClaims.js "PASTE_UID_HERE" admin event:create notes:upload
```

---

## ⚠️ Common Mistake You Made

❌ **WRONG** (what you typed):
```powershell
node scripts/setCustomClaims.js <USER_UID> admin event:create notes:upload
```

✅ **CORRECT** (with real UID):
```powershell
node scripts/setCustomClaims.js "K9mxPqR7sVNtXyZ2aB4cD6fG" admin event:create notes:upload
```

**Key differences:**
- Use **quotes** around the UID
- Use the **actual UID string**, not the placeholder `<USER_UID>`
- The `<USER_UID>` is just documentation notation meaning "replace this with your user's UID"

---

## 📥 Don't Have Service Account Key?

Download it from Firebase Console:
1. Go to https://console.firebase.google.com/
2. Select your project
3. ⚙️ Settings → Project Settings → Service Accounts
4. Click "Generate New Private Key"
5. Save to: `D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json`

---

## ✅ After Setting Admin Claims

The user **must** sign out and back in:
1. Open app → Profile → Sign Out
2. Sign in again
3. Go to Profile → "Open Admin Panel" button will appear!

---

## 📚 Full Guides Available

- **ADMIN_SETUP_GUIDE.md** - Detailed step-by-step setup
- **ADMIN_PANEL_GUIDE.md** - How to use the admin panel
- **scripts/setup-admin.ps1** - Automated setup script
- **scripts/listUsers.js** - List all users and their UIDs
- **scripts/setCustomClaims.js** - Set admin claims manually

