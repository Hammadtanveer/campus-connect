const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize the admin SDK
admin.initializeApp();

// Configuration: set ADMIN_CODE and DEFAULT_ADMIN_ROLES in functions config (or fallback here)
const DEFAULT_ADMIN_CODE = functions.config().campus?.admin_code || 'CAMPUS_ADMIN_2025';
const DEFAULT_ADMIN_ROLES = (functions.config().campus?.default_admin_roles && functions.config().campus.default_admin_roles.split(',')) || ['admin','event:create','notes:upload'];

/**
 * Callable function: requestAdminAccess
 * Expects: { adminCode: string }
 * Behavior:
 *  - Validates adminCode server-side against functions config
 *  - If valid: sets custom claims on the requesting user (admin=true, roles=[...])
 *  - Updates Firestore users/{uid} document with isAdmin=true and roles
 *  - Returns success or error
 * Security:
 *  - Only callable by authenticated users (context.auth.required)
 *  - The authoritative admin code is kept in functions config (not repo)
 */
exports.requestAdminAccess = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'The function must be called while authenticated.');
  }

  const adminCode = (data && data.adminCode) ? String(data.adminCode).trim() : '';
  if (!adminCode) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing adminCode');
  }

  // Validate admin code
  if (adminCode !== DEFAULT_ADMIN_CODE) {
    throw new functions.https.HttpsError('permission-denied', 'Invalid admin code');
  }

  const uid = context.auth.uid;
  if (!uid) {
    throw new functions.https.HttpsError('unauthenticated', 'No authenticated user id');
  }

  try {
    // Set custom claims
    const claims = { admin: true, roles: DEFAULT_ADMIN_ROLES };
    await admin.auth().setCustomUserClaims(uid, claims);

    // Update Firestore users doc to reflect isAdmin and roles
    const db = admin.firestore();
    const userRef = db.collection('users').doc(uid);

    // Merge existing roles with defaults and set isAdmin
    const snap = await userRef.get();
    const existingRoles = (snap.exists && snap.data() && snap.data().roles) ? snap.data().roles : [];
    const mergedRoles = Array.from(new Set([...(existingRoles || []), ...DEFAULT_ADMIN_ROLES]));

    await userRef.set({ isAdmin: true, roles: mergedRoles }, { merge: true });

    return { success: true, message: 'Admin access granted. Please refresh token (sign out/in or call getIdToken(true)).' };
  } catch (err) {
    console.error('requestAdminAccess error', err);
    throw new functions.https.HttpsError('internal', err.message || 'Internal error');
  }
});

// Bootstrap Super Admin (ONE-TIME USE)
// Import and export the bootstrap function
const { bootstrapSuperAdmin } = require('./bootstrap-super-admin');
exports.bootstrapSuperAdmin = bootstrapSuperAdmin;

const cloudinary = require('cloudinary').v2;

// Configure Cloudinary (Best practice: use functions.config().cloudinary)
// For now, using provided credentials for immediate fix
cloudinary.config({
  cloud_name: 'dkxunmucg',
  api_key: '492784632542267',
  api_secret: '3CSXo-IjIxXX6qy-CTo-9bBSunU'
});

/**
 * Callable function: generateSignedPdfUrl
 * Expects: { publicId: string }
 * Returns: { url: string }
 */
exports.generateSignedPdfUrl = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated.');
    }

    const publicId = data.publicId;
    if (!publicId) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing publicId');
    }

    try {
        // Generate the API Download URL (which works for authenticated PDFs)
        const url = cloudinary.utils.private_download_url(publicId, 'pdf', {
            resource_type: 'image', // PDFs are often stored as 'image' in Cloudinary unless 'raw' was specified
            type: 'authenticated',
            attachment: false, // Allow viewing
            expires_at: Math.floor(Date.now() / 1000) + 3600 // 1 hour
        });

        return { url: url };
    } catch (error) {
        console.error("Error generating signed URL:", error);
        throw new functions.https.HttpsError('internal', 'Failed to generate URL');
    }
});
