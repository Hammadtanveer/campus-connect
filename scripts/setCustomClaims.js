// Node script to set Firestore permissions for a user document.
// Usage: node scripts/setCustomClaims.js <uid> meetings:manage notes:manage placements:manage
// Requires: npm install firebase-admin; set GOOGLE_APPLICATION_CREDENTIALS to service account json.

const admin = require('firebase-admin');

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to your service account json path');
  process.exit(1);
}

try {
  admin.initializeApp({ credential: admin.credential.applicationDefault() });
} catch (e) {
  // ignore if already initialized
}

const [, , uid, ...permissionArgs] = process.argv;
if (!uid) {
  console.error('UID required');
  process.exit(1);
}

const permissions = Array.from(new Set(permissionArgs.map((entry) => String(entry).trim()).filter(Boolean)));

if (permissions.length === 0) {
  console.error('At least one permission is required');
  process.exit(1);
}

(async () => {
  try {
    const db = admin.firestore();
    await db.collection('users').doc(uid).set({ permissions }, { merge: true });
    console.log('Permissions updated for', uid, permissions);
    process.exit(0);
  } catch (e) {
    console.error('Failed to update permissions', e);
    process.exit(2);
  }
})();
