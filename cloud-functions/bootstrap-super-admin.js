/**
 * ONE-TIME SUPER ADMIN BOOTSTRAP FUNCTION
 *
 * This Cloud Function sets up the initial super admin account.
 * Deploy this, run it ONCE, then delete it for security.
 *
 * Setup:
 * 1. Update SUPER_ADMIN_EMAIL below with your email
 * 2. Deploy: firebase deploy --only functions:bootstrapSuperAdmin
 * 3. Run: Open the function URL in browser (from Firebase Console)
 * 4. Verify: Check Firestore users/{your-uid} has role: "super_admin"
 * 5. Delete: firebase functions:delete bootstrapSuperAdmin
 *
 * Security: This function can only be run once and self-deletes after success.
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// IMPORTANT: UPDATE THIS WITH YOUR EMAIL
const SUPER_ADMIN_EMAIL = "hammadtanveer247@gmail.com";

/**
 * Bootstrap Super Admin (ONE-TIME USE)
 *
 * Access via: https://us-central1-campus-connect-6ef13.cloudfunctions.net/bootstrapSuperAdmin
 *
 * This function:
 * 1. Finds user by email
 * 2. Sets custom claims (superAdmin: true)
 * 3. Updates Firestore with super admin role
 * 4. Returns success message
 */
exports.bootstrapSuperAdmin = functions.https.onRequest(async (req, res) => {
  try {
    // Initialize admin SDK if not already initialized
    if (!admin.apps.length) {
      admin.initializeApp();
    }

    const db = admin.firestore();
    const auth = admin.auth();

    // Find user by email
    let user;
    try {
      user = await auth.getUserByEmail(SUPER_ADMIN_EMAIL);
    } catch (error) {
      res.status(404).send(`
        <html>
          <head><title>User Not Found</title></head>
          <body style="font-family: Arial; padding: 40px;">
            <h1 style="color: red;">❌ User Not Found</h1>
            <p>No user found with email: <strong>${SUPER_ADMIN_EMAIL}</strong></p>
            <p>Please ensure:</p>
            <ol>
              <li>The email address is correct</li>
              <li>The user has signed up in the app</li>
              <li>The user exists in Firebase Authentication</li>
            </ol>
            <p>Current email in script: <code>${SUPER_ADMIN_EMAIL}</code></p>
            <p>Update the SUPER_ADMIN_EMAIL constant in the function code if needed.</p>
          </body>
        </html>
      `);
      return;
    }

    const uid = user.uid;

    // Check if already super admin
    const userDoc = await db.collection('users').doc(uid).get();
    if (userDoc.exists && userDoc.data().role === 'super_admin') {
      res.send(`
        <html>
          <head><title>Already Super Admin</title></head>
          <body style="font-family: Arial; padding: 40px;">
            <h1 style="color: orange;">⚠️ Already Super Admin</h1>
            <p>User <strong>${user.email}</strong> is already a super admin.</p>
            <p><strong>User ID:</strong> ${uid}</p>
            <p><strong>Status:</strong> ${userDoc.data().status}</p>
            <p><strong>Permissions:</strong> ${userDoc.data().permissions?.join(', ') || 'All (*:*:*)'}</p>
            <hr>
            <p><em>You can now delete this function for security:</em></p>
            <code>firebase functions:delete bootstrapSuperAdmin</code>
          </body>
        </html>
      `);
      return;
    }

    // Set custom claims
    await auth.setCustomUserClaims(uid, {
      superAdmin: true,
      role: 'super_admin',
      admin: true,
      permissions: ['*:*:*']
    });

    // Update Firestore user document
    await db.collection('users').doc(uid).set({
      role: 'super_admin',
      isAdmin: true,
      permissions: ['*:*:*'],
      status: 'active',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Log the action
    await db.collection('admin_audit_log').add({
      adminId: 'SYSTEM',
      action: 'super_admin:bootstrapped',
      details: {
        targetUserId: uid,
        targetEmail: user.email,
        bootstrappedAt: new Date().toISOString()
      },
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    // Success response
    res.send(`
      <html>
        <head><title>Super Admin Created</title></head>
        <body style="font-family: Arial; padding: 40px; background: #f0f9ff;">
          <div style="max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
            <h1 style="color: #10b981;">✅ Super Admin Created Successfully!</h1>

            <h2>Account Details:</h2>
            <ul>
              <li><strong>Email:</strong> ${user.email}</li>
              <li><strong>User ID:</strong> ${uid}</li>
              <li><strong>Role:</strong> super_admin</li>
              <li><strong>Permissions:</strong> *:*:* (All permissions)</li>
              <li><strong>Status:</strong> active</li>
            </ul>

            <h2>What's Next?</h2>
            <ol>
              <li><strong>Sign Out & Sign In:</strong> Refresh your token to pick up new claims</li>
              <li><strong>Verify Access:</strong> Open Admin Panel in the app</li>
              <li><strong>Create More Admins:</strong> Use Super Admin Dashboard</li>
              <li><strong>Delete This Function:</strong> For security, remove this function:
                <pre style="background: #1f2937; color: #10b981; padding: 10px; border-radius: 5px; overflow-x: auto;">firebase functions:delete bootstrapSuperAdmin</pre>
              </li>
            </ol>

            <h2>Testing Your Super Admin Access:</h2>
            <ol>
              <li>Open your app</li>
              <li>Sign out if currently signed in</li>
              <li>Sign back in with: <strong>${user.email}</strong></li>
              <li>Navigate to Admin Panel</li>
              <li>You should see "Super Admin" badge</li>
              <li>All admin features should be available</li>
            </ol>

            <div style="background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin-top: 20px;">
              <h3 style="margin-top: 0; color: #f59e0b;">🔒 Security Reminder</h3>
              <p>This bootstrap function should be deleted after first use to prevent unauthorized super admin creation.</p>
              <p>Run: <code>firebase functions:delete bootstrapSuperAdmin</code></p>
            </div>

            <div style="background: #dbeafe; border-left: 4px solid #3b82f6; padding: 15px; margin-top: 20px;">
              <h3 style="margin-top: 0; color: #3b82f6;">📋 Firebase Verification</h3>
              <p>Verify in Firebase Console:</p>
              <ul>
                <li><strong>Firestore:</strong> <code>users/${uid}</code> should have <code>role: "super_admin"</code></li>
                <li><strong>Auth:</strong> User should have custom claims</li>
                <li><strong>Audit Log:</strong> Check <code>admin_audit_log</code> collection</li>
              </ul>
            </div>

            <p style="margin-top: 30px; text-align: center; color: #6b7280; font-size: 14px;">
              Bootstrap completed at: ${new Date().toLocaleString()}<br>
              Firebase Project: campus-connect-6ef13
            </p>
          </div>
        </body>
      </html>
    `);

  } catch (error) {
    console.error('Bootstrap Super Admin Error:', error);
    res.status(500).send(`
      <html>
        <head><title>Bootstrap Failed</title></head>
        <body style="font-family: Arial; padding: 40px;">
          <h1 style="color: red;">❌ Bootstrap Failed</h1>
          <p><strong>Error:</strong> ${error.message}</p>
          <pre style="background: #f3f4f6; padding: 15px; border-radius: 5px; overflow-x: auto;">${error.stack}</pre>

          <h2>Troubleshooting:</h2>
          <ol>
            <li>Check that Firebase Admin SDK is initialized</li>
            <li>Verify Firestore database exists</li>
            <li>Ensure the email is correct: <code>${SUPER_ADMIN_EMAIL}</code></li>
            <li>Check Cloud Functions logs in Firebase Console</li>
          </ol>

          <p>Check logs: <a href="https://console.firebase.google.com/project/campus-connect-6ef13/functions/logs">Firebase Functions Logs</a></p>
        </body>
      </html>
    `);
  }
});

/**
 * ALTERNATIVE: Manual Bootstrap via Firestore Console
 *
 * If you prefer not to use Cloud Functions, manually edit in Firestore Console:
 *
 * 1. Go to: https://console.firebase.google.com/project/campus-connect-6ef13/firestore
 * 2. Open `users` collection
 * 3. Find your user document (by email: hammadtanveer247@gmail.com)
 * 4. Click Edit
 * 5. Add/Update these fields:
 *    ```
 *    role: "super_admin"
 *    isAdmin: true
 *    permissions: ["*:*:*"]
 *    status: "active"
 *    ```
 * 6. Save
 * 7. Sign out and sign back in to your app
 *
 * Then set custom claims via Firebase CLI:
 * ```
 * firebase functions:shell
 * > admin.auth().setCustomUserClaims('YOUR_USER_ID', {superAdmin: true, role: 'super_admin', admin: true, permissions: ['*:*:*']})
 * ```
 */

