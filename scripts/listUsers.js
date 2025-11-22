/**
 * Helper script to list all Firebase users and their UIDs
 * Run this to find your user UID before setting custom claims
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
try {
  admin.initializeApp({
    credential: admin.credential.applicationDefault()
  });
  console.log('✅ Firebase Admin SDK initialized successfully');
} catch (error) {
  console.error('❌ Error initializing Firebase Admin SDK:');
  console.error('Make sure you have set GOOGLE_APPLICATION_CREDENTIALS environment variable');
  console.error('Example: $env:GOOGLE_APPLICATION_CREDENTIALS="C:/path/to/your-service-account-key.json"');
  console.error('\nError details:', error.message);
  process.exit(1);
}

// List all users
async function listAllUsers() {
  try {
    console.log('\n📋 Fetching all users...\n');
    const listUsersResult = await admin.auth().listUsers(1000);

    if (listUsersResult.users.length === 0) {
      console.log('No users found in your Firebase project.');
      return;
    }

    console.log(`Found ${listUsersResult.users.length} user(s):\n`);
    console.log('=' .repeat(80));

    listUsersResult.users.forEach((userRecord, index) => {
      console.log(`\n${index + 1}. USER DETAILS:`);
      console.log(`   UID:           ${userRecord.uid}`);
      console.log(`   Email:         ${userRecord.email || 'N/A'}`);
      console.log(`   Display Name:  ${userRecord.displayName || 'N/A'}`);
      console.log(`   Created:       ${new Date(userRecord.metadata.creationTime).toLocaleString()}`);
      console.log(`   Last Sign In:  ${userRecord.metadata.lastSignInTime ? new Date(userRecord.metadata.lastSignInTime).toLocaleString() : 'Never'}`);

      // Check if user has custom claims
      if (userRecord.customClaims) {
        console.log(`   Custom Claims: ${JSON.stringify(userRecord.customClaims, null, 2)}`);
      } else {
        console.log(`   Custom Claims: None (Regular user)`);
      }
      console.log('-'.repeat(80));
    });

    console.log('\n\n📝 To set admin access for a user, copy their UID and run:');
    console.log('   node scripts/setCustomClaims.js "PASTE_UID_HERE" admin event:create notes:upload\n');

  } catch (error) {
    console.error('❌ Error listing users:', error.message);
    process.exit(1);
  }
}

// Run the function
listAllUsers()
  .then(() => {
    console.log('✅ Done!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  });

