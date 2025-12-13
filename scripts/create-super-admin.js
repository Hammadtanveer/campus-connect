/**
 * ONE-TIME SCRIPT: Create Super Admin User Document
 *
 * This script creates the users collection and your super admin document
 * in Firestore directly.
 *
 * Run this ONCE after creating your Firestore database.
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
const serviceAccount = require('./path-to-service-account-key.json'); // Update this path

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function createSuperAdminUser() {
  try {
    console.log('🔍 Looking for user with email: hammadtanveer247@gmail.com...');

    // Find user by email in Firebase Auth
    let user;
    try {
      user = await admin.auth().getUserByEmail('hammadtanveer247@gmail.com');
      console.log(`✅ Found user: ${user.uid}`);
    } catch (error) {
      console.error('❌ User not found in Firebase Authentication!');
      console.log('\n📋 INSTRUCTIONS:');
      console.log('1. Sign in to your CampusConnect app at least once');
      console.log('2. This will create your user in Firebase Authentication');
      console.log('3. Then run this script again');
      process.exit(1);
    }

    const uid = user.uid;

    // Create user document in Firestore
    console.log('📝 Creating super admin document in Firestore...');

    await db.collection('users').doc(uid).set({
      id: uid,
      email: 'hammadtanveer247@gmail.com',
      displayName: user.displayName || 'Super Admin',
      role: 'super_admin',
      isAdmin: true,
      permissions: ['*:*:*'],
      status: 'active',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      // Other default fields
      course: '',
      branch: '',
      year: '',
      bio: '',
      profilePictureUrl: '',
      eventCount: 0,
      isMentor: false,
      mentorshipBio: '',
      expertise: [],
      mentorshipStatus: 'available',
      mentorshipRating: null,
      totalConnections: 0,
      roles: ['admin', 'event:create', 'notes:upload'], // Legacy compatibility
      department: null,
      createdBy: 'SYSTEM_BOOTSTRAP',
      expiresAt: null,
      lastModifiedBy: null,
      lastModifiedAt: null,
      roleTemplate: null,
      suspendedBy: null,
      suspendedAt: null,
      suspendedUntil: null,
      suspensionReason: null,
      revokedBy: null,
      revokedAt: null,
      revocationReason: null
    }, { merge: true });

    // Set custom claims
    console.log('🔐 Setting custom claims...');
    await admin.auth().setCustomUserClaims(uid, {
      superAdmin: true,
      role: 'super_admin',
      admin: true,
      permissions: ['*:*:*']
    });

    // Create audit log entry
    console.log('📋 Creating audit log...');
    await db.collection('admin_audit_log').add({
      adminId: 'SYSTEM',
      action: 'super_admin:created',
      details: {
        targetUserId: uid,
        targetEmail: 'hammadtanveer247@gmail.com',
        method: 'manual_script'
      },
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log('\n✅ SUCCESS! Super Admin created!');
    console.log('\n📊 Details:');
    console.log(`   Email: hammadtanveer247@gmail.com`);
    console.log(`   UID: ${uid}`);
    console.log(`   Role: super_admin`);
    console.log(`   Permissions: *:*:* (all permissions)`);
    console.log(`   Status: active`);

    console.log('\n🎯 Next Steps:');
    console.log('1. Sign out of your app (if signed in)');
    console.log('2. Sign back in to refresh your token');
    console.log('3. You should now have super admin access!');
    console.log('\n✅ You can now delete this script for security.');

    process.exit(0);

  } catch (error) {
    console.error('❌ Error creating super admin:', error);
    process.exit(1);
  }
}

// Run the script
createSuperAdminUser();

