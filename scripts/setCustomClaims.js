// Node script to set custom claims for a user.
// Usage: node setCustomClaims.js <uid> admin event:create notes:upload senior:update society:manage
// Requires: npm install firebase-admin; set GOOGLE_APPLICATION_CREDENTIALS to service account json.

import admin from 'firebase-admin';

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to your service account json path');
  process.exit(1);
}

try {
  admin.initializeApp({ credential: admin.credential.applicationDefault() });
} catch (e) {
  // ignore if already initialized
}

const [, , uid, ...roleArgs] = process.argv;
if (!uid) {
  console.error('UID required');
  process.exit(1);
}

const roles = roleArgs.filter(r => r !== 'admin');
const adminFlag = roleArgs.includes('admin');

(async () => {
  try {
    const claims = { roles, admin: adminFlag };
    await admin.auth().setCustomUserClaims(uid, claims);
    console.log('Claims set for', uid, claims);
    console.log('Advise user to re-authenticate to refresh token.');
    process.exit(0);
  } catch (e) {
    console.error('Failed to set claims', e);
    process.exit(2);
  }
})();
