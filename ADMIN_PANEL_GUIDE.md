# Admin Login Panel - Implementation Guide

## Overview
I've successfully implemented an Admin Panel feature for your CampusConnect app. The system uses Role-Based Access Control (RBAC) with Firebase custom claims to distinguish between regular users and admins.

## What Was Implemented

### 1. Admin Panel Screen (`AdminPanelScreen.kt`)
A comprehensive admin panel that shows:
- **Admin Status**: Displays whether the user has admin access
- **User Information**: Shows display name and email
- **Permissions List**: Visual list of all granted permissions:
  - Full Admin Access
  - Event Management (create events)
  - Notes Upload
  - Senior Management
  - Society Management
- **Quick Actions**: Buttons to:
  - Create Event
  - Upload Notes
  - Refresh Permissions (refreshes Firebase custom claims)
- **Instructions**: How to get admin access
- **Technical Details**: Shows admin flag and assigned roles for debugging

### 2. Navigation Integration
- Added route `"admin/panel"` to Navigation.kt
- Updated AccountView to accept NavController parameter
- Added "Open Admin Panel" button in AccountView's Admin Tools section

### 3. Access Requirements
The admin panel can be accessed by users who have:
- `isAdmin = true` flag in their user profile, OR
- Any elevated roles in their `roles` array (e.g., "event:create", "notes:upload", etc.)

## How to Access the Admin Panel

### For Regular Users:
1. Sign in to the app
2. Go to Profile screen (AccountView)
3. **IF you have admin privileges**, you'll see an "Admin Tools" section
4. Click "Open Admin Panel" button

### For Users Without Admin Access:
The "Admin Tools" section and "Open Admin Panel" button will NOT appear if you don't have any admin permissions.

## How to Grant Admin Access

You need to use the Firebase Admin SDK to set custom claims. Here's how:

### Step 1: Set Up Firebase Admin SDK

1. Download your service account key from Firebase Console:
   - Go to Project Settings → Service Accounts
   - Click "Generate New Private Key"
   - Save the JSON file securely

2. Set environment variable (PowerShell):
   ```powershell
   $env:GOOGLE_APPLICATION_CREDENTIALS="C:/path/to/serviceAccountKey.json"
   ```

### Step 2: Use the setCustomClaims.js Script

The script is already in your project at `scripts/setCustomClaims.js`. Run it:

```powershell
# Install Firebase Admin SDK if not already installed
npm install firebase-admin

# Grant full admin access
node scripts/setCustomClaims.js <USER_UID> admin

# Grant specific roles
node scripts/setCustomClaims.js <USER_UID> event:create notes:upload

# Grant all permissions
node scripts/setCustomClaims.js <USER_UID> admin event:create notes:upload senior:update society:manage
```

### Step 3: User Must Sign Out and Sign Back In

After setting custom claims:
1. The user must sign out of the app
2. Sign back in
3. OR use the "Refresh Permissions" button in the Admin Panel

## Testing the Admin Panel

### Test Scenario 1: Regular User (No Admin Access)
1. Sign in with a regular account
2. Go to Profile screen
3. **Expected**: No "Admin Tools" section visible
4. **Expected**: Cannot navigate to admin panel

### Test Scenario 2: Admin User
1. Grant admin access using setCustomClaims.js
2. Sign out and sign back in
3. Go to Profile screen
4. **Expected**: "Admin Tools" section appears with roles listed
5. Click "Open Admin Panel"
6. **Expected**: Full admin panel showing all permissions and quick actions

### Test Scenario 3: User with Specific Roles
1. Grant specific role: `node scripts/setCustomClaims.js USER_UID event:create`
2. Sign out and sign back in
3. Go to Profile → Open Admin Panel
4. **Expected**: Only "Event Management" permission shows
5. **Expected**: Only "Create Event" quick action appears

## File Changes Made

### New Files:
1. **`AdminPanelScreen.kt`**: Complete admin panel UI

### Modified Files:
1. **`Navigation.kt`**: 
   - Added import for AdminPanelScreen
   - Added route `"admin/panel"`
   - Updated AccountView call to pass NavController

2. **`AccountView.kt`**:
   - Added NavController parameter
   - Added "Open Admin Panel" button in Admin Tools section

## Features Available in Admin Panel

### Current Features:
✅ View admin status and permissions  
✅ List all assigned roles  
✅ Quick action: Create Event  
✅ Quick action: Upload Notes  
✅ Refresh permissions without re-login  
✅ Instructions for getting admin access  
✅ Technical details for debugging  

### Recommended Next Steps:
- Add admin-specific analytics/dashboard
- Add user management (view all users, assign roles from UI)
- Add content moderation tools
- Add system settings configuration
- Add audit log viewing

## Security Notes

1. **Firebase Rules**: Your existing `firestore.rules` already protect admin operations
2. **Client-Side Checks**: The UI hides admin features, but server-side validation is still required
3. **Custom Claims**: These are stored in Firebase Auth tokens and can't be tampered with by clients
4. **Refresh Strategy**: Claims are refreshed on app launch and via "Refresh Permissions" button

## Troubleshooting

### "I don't see the Admin Tools section"
- Check if custom claims are set: Use Firebase Console → Authentication → Select user → Check custom claims
- Sign out and back in after setting claims
- Or use "Refresh Permissions" if already in app

### "Admin Panel shows 'No special permissions assigned'"
- Claims might not be synced yet
- Click "Refresh Permissions" button
- Check Firebase console to verify claims are set

### "Button is greyed out/not working"
- Ensure you're on a screen that has NavController passed (Profile screen should work)
- Check app logs for navigation errors

## Summary

You now have a fully functional admin login panel that:
- Shows only to users with admin privileges
- Displays all granted permissions clearly
- Provides quick access to admin features
- Integrates seamlessly with your existing RBAC system

The admin panel is accessible from the Profile screen when logged in with an account that has admin permissions. Regular users won't see any admin UI elements, maintaining a clean experience for non-admin users.

