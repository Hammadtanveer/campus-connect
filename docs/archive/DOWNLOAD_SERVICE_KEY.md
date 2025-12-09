# 🔐 How to Download Firebase Service Account Key - Visual Guide

## ❌ Current Issue

You're seeing this error:
```
The file at D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json does not exist
```

**Why?** You haven't downloaded the Firebase service account key yet!

---

## 📥 Step-by-Step Download Instructions

### Step 1: Open Firebase Console

🌐 **Go to**: https://console.firebase.google.com/

You should see a list of your Firebase projects.

---

### Step 2: Select Your Project

Click on your **CampusConnect** project (or whatever you named your Firebase project)

---

### Step 3: Open Project Settings

1. Look at the **top-left** of the screen
2. Click the **⚙️ gear icon** (Settings)
3. Select **"Project settings"** from the dropdown menu

---

### Step 4: Go to Service Accounts Tab

1. You'll see several tabs at the top: **General**, **Usage**, **Integrations**, etc.
2. Click on the **"Service accounts"** tab
3. You should see: "Firebase Admin SDK" section

---

### Step 5: Generate New Private Key

1. Scroll down to the **"Firebase Admin SDK"** section
2. You'll see a button that says **"Generate new private key"**
3. Click that button

---

### Step 6: Confirm Download

1. A warning dialog will appear saying:
   > "This key should be kept confidential and never checked into version control."
2. Click the **"Generate key"** button to confirm

---

### Step 7: Save the Downloaded File

1. Your browser will download a JSON file
2. The file will have a name like:
   ```
   campusconnect-firebase-adminsdk-abc12-xyz789.json
   ```
3. **Important**: The name will be different for everyone!

---

### Step 8: Rename and Move the File

#### Option A: Using File Explorer (Easier)

1. Open **File Explorer**
2. Go to your **Downloads** folder
3. Find the JSON file you just downloaded
4. **Right-click** → **Rename**
5. Rename it to exactly: `serviceAccountKey.json`
6. **Cut** the file (Ctrl+X)
7. Navigate to: `D:\AndroidStudioProjects\CampusConnect\`
8. **Paste** the file there (Ctrl+V)

#### Option B: Using PowerShell

```powershell
# Find the downloaded file (replace the name with your actual file name)
Move-Item "$env:USERPROFILE\Downloads\campusconnect-*-firebase-adminsdk-*.json" "D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"
```

---

### Step 9: Verify the File is in Place

Run this command:

```powershell
Test-Path "D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"
```

**Expected output**: `True`

If you see `False`, the file is not in the right place.

---

### Step 10: Verify File Contents (Optional)

```powershell
Get-Content "D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json" | Select-Object -First 5
```

You should see JSON content starting with:
```json
{
  "type": "service_account",
  "project_id": "campusconnect-xxxxx",
  ...
```

---

## ✅ After You Have the File

Now you can proceed with the admin setup:

```powershell
# 1. Set the environment variable
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"

# 2. List users
node scripts/listUsers.js

# 3. Set admin claims (replace with real UID)
node scripts/setCustomClaims.js "PASTE_UID_HERE" admin event:create notes:upload
```

---

## 🆘 Troubleshooting

### "I can't find the downloaded file"

Check your browser's download folder:
```powershell
# Open Downloads folder
explorer "$env:USERPROFILE\Downloads"

# List JSON files in Downloads
Get-ChildItem "$env:USERPROFILE\Downloads\*.json" | Select-Object Name
```

### "The file is in Downloads but I can't move it"

Use PowerShell to move it:
```powershell
# List all JSON files in Downloads (to see the exact name)
Get-ChildItem "$env:USERPROFILE\Downloads\*.json"

# Copy the name you see and replace EXACT_NAME_HERE:
Move-Item "$env:USERPROFILE\Downloads\EXACT_NAME_HERE.json" "D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"
```

### "I accidentally downloaded it twice"

Delete all and start over:
```powershell
# Delete from CampusConnect folder
Remove-Item "D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json" -ErrorAction SilentlyContinue

# Delete from Downloads
Remove-Item "$env:USERPROFILE\Downloads\*firebase-adminsdk*.json"

# Then download again from Firebase Console
```

### "Test-Path still returns False"

Check for typos:
```powershell
# Check if directory exists
Test-Path "D:\AndroidStudioProjects\CampusConnect"

# List all files in the directory
Get-ChildItem "D:\AndroidStudioProjects\CampusConnect" -Filter "*.json"
```

---

## 🔒 Security Reminder

### ⚠️ CRITICAL: Never Share This File!

This file contains credentials that give **full admin access** to your Firebase project. 

**Do NOT**:
- ❌ Commit it to Git (it's in .gitignore)
- ❌ Share it on Discord/Slack/Teams
- ❌ Email it to anyone
- ❌ Upload it to Google Drive/Dropbox public folders
- ❌ Include it in screenshots

**If you accidentally expose it**:
1. Go back to Firebase Console → Service Accounts
2. Click the "..." menu next to the key
3. Select "Delete key"
4. Generate a new one

---

## 📍 Exact File Location

The file MUST be at exactly this location:
```
D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json
```

**Common mistakes**:
- ❌ `serviceaccountkey.json` (wrong case)
- ❌ `serviceAccountKey.JSON` (wrong extension case)
- ❌ `serviceAccountKey (1).json` (duplicate download)
- ❌ In Downloads folder instead of CampusConnect folder

---

## ✅ Success Checklist

After downloading, verify:

- [ ] File exists at: `D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json`
- [ ] File is named exactly: `serviceAccountKey.json` (case-sensitive)
- [ ] File contains JSON starting with `{"type": "service_account"`
- [ ] `Test-Path` command returns `True`
- [ ] File is in .gitignore (already done)

Once all checked, run:
```powershell
.\scripts\check-service-key.ps1
```

It should show ✅ and tell you the next steps!

---

## 🎯 Quick Commands to Run

```powershell
# 1. Run the helper script
.\scripts\check-service-key.ps1

# 2. If it says file is missing, it will open Firebase Console for you
# Download the key and place it as instructed

# 3. Run the helper script again to verify
.\scripts\check-service-key.ps1

# 4. Once verified, proceed with admin setup
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"
node scripts/listUsers.js
```

