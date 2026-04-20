require('dotenv').config();

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize the admin SDK
admin.initializeApp();

// Configuration: set campus.admin_code and default admin permissions in functions config
const EXPECTED_ADMIN_ACCESS_CODE = functions.config().campus?.admin_code || '';
const DEFAULT_ADMIN_PERMISSIONS = (functions.config().campus?.default_admin_permissions && functions.config().campus.default_admin_permissions.split(',')) || [
  'meetings:manage',
  'notes:manage',
  'placements:manage',
  'society:*:manage',
];

/**
 * Callable function: requestAdminAccess
 * Expects: { adminCode: string }
 * Behavior:
 *  - Validates adminCode server-side against functions config
 *  - If valid: updates Firestore users/{uid} document with canonical permissions
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
  if (!EXPECTED_ADMIN_ACCESS_CODE) {
    throw new functions.https.HttpsError('failed-precondition', 'Admin code is not configured on the server');
  }
  if (adminCode !== EXPECTED_ADMIN_ACCESS_CODE) {
    throw new functions.https.HttpsError('permission-denied', 'Invalid admin code');
  }

  const uid = context.auth.uid;
  if (!uid) {
    throw new functions.https.HttpsError('unauthenticated', 'No authenticated user id');
  }

  try {
    // Update Firestore users doc with canonical permissions
    const db = admin.firestore();
    const userRef = db.collection('users').doc(uid);

    // Merge existing doc permissions into canonical permissions
    const snap = await userRef.get();
    const existingPermissions = (snap.exists && snap.data() && Array.isArray(snap.data().permissions)) ? snap.data().permissions : [];
    const mergedPermissions = Array.from(new Set([...(existingPermissions || []), ...DEFAULT_ADMIN_PERMISSIONS]));

    await userRef.set({ permissions: mergedPermissions }, { merge: true });

    return { success: true, message: 'Admin access granted.' };
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

// Configure Cloudinary from environment variables.
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

/**
 * Callable function: createCloudinaryUploadSignature
 * Expects: { folder: string, publicId: string, resourceType?: string, uploadType?: string }
 * Returns signed parameters required by Cloudinary upload API.
 */
exports.createCloudinaryUploadSignature = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated.');
    }

    const folder = data && data.folder ? String(data.folder).trim() : '';
    const publicId = data && data.publicId ? String(data.publicId).trim() : '';
    const resourceType = data && data.resourceType ? String(data.resourceType).trim() : 'auto';
    const uploadType = data && data.uploadType ? String(data.uploadType).trim() : 'authenticated';

    if (!folder || !publicId) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing folder or publicId');
    }

    const apiKey = process.env.CLOUDINARY_API_KEY;
    const apiSecret = process.env.CLOUDINARY_API_SECRET;
    const cloudName = process.env.CLOUDINARY_CLOUD_NAME;

    if (!apiKey || !apiSecret || !cloudName) {
        throw new functions.https.HttpsError('failed-precondition', 'Cloudinary credentials are not configured on server');
    }

    const timestamp = Math.floor(Date.now() / 1000);
    const signedParams = {
        folder,
        public_id: publicId,
        resource_type: resourceType,
        timestamp,
        type: uploadType,
        overwrite: false,
    };

    const signature = cloudinary.utils.api_sign_request(signedParams, apiSecret);

    return {
        cloudName,
        apiKey,
        timestamp,
        signature,
        folder,
        publicId,
        resourceType,
        uploadType,
        overwrite: false,
        secure: true,
    };
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

// Notify students when new notes are uploaded
exports.notifyStudentsOnNewNote = functions.firestore
  .document('notes/{noteId}')
  .onCreate(async (snapshot) => {
    const noteData = snapshot.data() || {};
    const noteTitle = noteData.title ? String(noteData.title) : '';
    const uploaderName = noteData.uploaderName ? String(noteData.uploaderName) : '';

    // uploaderName is read for future personalization/debug while keeping payload minimal.
    console.log('notifyStudentsOnNewNote', {
      noteId: snapshot.id,
      title: noteTitle,
      uploaderName,
    });

    const message = {
      topic: 'all_students',
      data: {
        title: 'New Notes Uploaded',
        body: noteTitle,
        type: 'notes',
      },
      android: {
        priority: 'high',
      },
    };

    return admin.messaging().send(message);
  });

function pickFirstString(data, keys, fallback = '') {
  for (const key of keys) {
    const value = data && data[key];
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }
  return fallback;
}

async function sendTopicDataMessage({ topic, title, body, type, targetId = '' }) {
  const payload = {
    topic,
    data: {
      title: title || 'CampusConnect',
      body: body || '',
      type: type || 'general',
      targetId,
    },
    android: { priority: 'high' },
  };
  return admin.messaging().send(payload);
}

exports.notifyOnNewEvent = functions.firestore
  .document('events/{eventId}')
  .onCreate(async (snapshot) => {
    const data = snapshot.data() || {};
    const title = pickFirstString(data, ['title', 'name'], 'New Event Created');
    const body = pickFirstString(data, ['description', 'details'], 'Tap to open Meetings & Announcements');

    return sendTopicDataMessage({
      topic: 'events',
      title,
      body,
      type: 'events',
      targetId: snapshot.id,
    });
  });

exports.notifyOnNewMeeting = functions.firestore
  .document('meetings/{meetingId}')
  .onCreate(async (snapshot) => {
    const data = snapshot.data() || {};
    const title = pickFirstString(data, ['title', 'name'], 'New Meeting Created');
    const body = pickFirstString(data, ['description', 'details'], 'Tap to open Meetings & Announcements');

    return sendTopicDataMessage({
      topic: 'events',
      title,
      body,
      type: 'meetings',
      targetId: snapshot.id,
    });
  });

exports.notifyOnNewAnnouncement = functions.firestore
  .document('announcements/{announcementId}')
  .onCreate(async (snapshot) => {
    const data = snapshot.data() || {};
    const title = pickFirstString(data, ['title', 'name'], 'New Announcement');
    const body = pickFirstString(data, ['description', 'details'], 'Tap to open Meetings & Announcements');

    return sendTopicDataMessage({
      topic: 'events',
      title,
      body,
      type: 'announcements',
      targetId: snapshot.id,
    });
  });

exports.notifyOnNewPlacement = functions.firestore
  .document('placements/{placementId}')
  .onCreate(async (snapshot) => {
    const data = snapshot.data() || {};
    const role = pickFirstString(data, ['role', 'jobTitle', 'title'], 'New Placement Added');
    const company = pickFirstString(data, ['companyName', 'company', 'organization'], 'Campus Placement Cell');

    return sendTopicDataMessage({
      topic: 'placements',
      title: role,
      body: company,
      type: 'placements',
      targetId: snapshot.id,
    });
  });

exports.notifyOnNewSocietyEvent = functions.firestore
  .document('societies/{societyId}/events/{eventId}')
  .onCreate(async (snapshot, context) => {
    const data = snapshot.data() || {};
    const title = pickFirstString(data, ['name', 'eventTitle', 'title'], 'New Society Event');
    const body = 'Tap to open society updates';

    const payload = {
      topic: 'society_updates',
      data: {
        title,
        body,
        type: 'society',
        targetId: context.params.eventId,
        parentId: context.params.societyId,
      },
      android: { priority: 'high' },
    };

    return admin.messaging().send(payload);
  });

exports.processNotificationQueue = functions.firestore
  .document('notification_queue/{queueId}')
  .onCreate(async (snapshot, context) => {
    const data = snapshot.data() || {};
    const topic = typeof data.topic === 'string' ? data.topic.trim() : '';
    const title = typeof data.title === 'string' ? data.title.trim() : '';
    const body = typeof data.body === 'string' ? data.body.trim() : '';
    const type = typeof data.type === 'string' ? data.type.trim() : 'general';
    const targetId = typeof data.targetId === 'string' ? data.targetId.trim() : '';

    const allowedTopics = ['all_students', 'events', 'placements', 'society_updates', 'notes'];
    if (!allowedTopics.includes(topic) || !title || !body) {
      console.error('processNotificationQueue invalid payload', { queueId: context.params.queueId, topic, titlePresent: Boolean(title), bodyPresent: Boolean(body) });
      await snapshot.ref.set(
        {
          status: 'failed',
          error: 'Invalid topic/title/body payload',
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      return null;
    }

    try {
      const messageId = await admin.messaging().send({
        topic,
        data: {
          title,
          body,
          type,
          targetId,
        },
        android: { priority: 'high' },
      });

      await snapshot.ref.set(
        {
          status: 'sent',
          messageId,
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    } catch (error) {
      console.error('processNotificationQueue send failed', error);
      await snapshot.ref.set(
        {
          status: 'failed',
          error: error && error.message ? error.message : 'Unknown send error',
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    }

    return null;
  });

// Send custom topic notification from admin panel
exports.sendTopicNotification = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required.');
  }

  const db = admin.firestore();
  const userDoc = await db.collection('users').doc(context.auth.uid).get();
  const permissions = userDoc.exists && Array.isArray(userDoc.data().permissions)
    ? userDoc.data().permissions
    : [];
  const canSend = permissions.includes('*:*:*') || permissions.includes('admin:access');
  if (!canSend) {
    throw new functions.https.HttpsError('permission-denied', 'Only super admin can send notifications.');
  }

  const topic = data && data.topic ? String(data.topic).trim() : '';
  const title = data && data.title ? String(data.title).trim() : '';
  const body = data && data.body ? String(data.body).trim() : '';
  const type = data && data.type ? String(data.type).trim() : 'general';

  const allowedTopics = ['all_students', 'events', 'placements', 'society_updates', 'notes'];
  if (!allowedTopics.includes(topic)) {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid topic.');
  }
  if (!title || !body) {
    throw new functions.https.HttpsError('invalid-argument', 'Title and body are required.');
  }

  const message = {
    topic,
    data: {
      title,
      body,
      type,
    },
    android: {
      priority: 'high',
    },
  };

  await admin.messaging().send(message);
  return { success: true };
});

// Auto-delete Firestore user document when Firebase Auth user is deleted
exports.onUserDeleted = functions.auth.user().onDelete(async (user) => {
  const uid = user.uid;
  console.log('onUserDeleted triggered for uid:', uid);

  try {
    const db = admin.firestore();
    await db.collection('users').document(uid).delete();
    console.log('Firestore user document deleted for uid:', uid);
  } catch (error) {
    console.error('Failed to delete Firestore document for uid:', uid, error);
  }
});

exports.createCloudinaryUploadSignature = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated.');
    }
    const folder = data?.folder ? String(data.folder).trim() : '';
    const publicId = data?.publicId ? String(data.publicId).trim() : '';
    const resourceType = data?.resourceType ? String(data.resourceType).trim() : 'auto';
    const uploadType = data?.uploadType ? String(data.uploadType).trim() : 'authenticated';
    if (!folder || !publicId) {
        throw new functions.https.HttpsError('invalid-argument', 'Missing folder or publicId');
    }
    const apiKey = process.env.CLOUDINARY_API_KEY;
    const apiSecret = process.env.CLOUDINARY_API_SECRET;
    const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
    if (!apiKey || !apiSecret || !cloudName) {
        throw new functions.https.HttpsError('failed-precondition', 'Cloudinary credentials not configured');
    }
    const timestamp = Math.floor(Date.now() / 1000);
    const signedParams = { folder, public_id: publicId, resource_type: resourceType,
        timestamp, type: uploadType, overwrite: false };
    const signature = cloudinary.utils.api_sign_request(signedParams, apiSecret);
    return { cloudName, apiKey, timestamp, signature, folder, publicId,
        resourceType, uploadType, overwrite: false, secure: true };
});

