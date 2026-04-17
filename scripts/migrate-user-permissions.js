/**
 * One-time migration: normalize users/{uid}.permissions to canonical keys.
 *
 * Usage:
 *   node scripts/migrate-user-permissions.js --dry-run
 *   node scripts/migrate-user-permissions.js --apply
 *
 * Canonical keys:
 *   meetings:manage
 *   notes:manage
 *   placements:manage
 *   seniors:manage
 *   society:<id>:manage
 *   society:*:manage
 *   admin:access
 *   *:*:*
 */

const admin = require('firebase-admin');

const isApply = process.argv.includes('--apply');
const isDryRun = !isApply;

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to your service account json path');
  process.exit(1);
}

try {
  admin.initializeApp({ credential: admin.credential.applicationDefault() });
} catch (_) {
  // Already initialized.
}

const db = admin.firestore();

function normalize(value) {
  return String(value || '').trim().toLowerCase();
}

function normalizeSocietyPermission(raw) {
  const normalized = normalize(raw);
  if (normalized.startsWith('can_manage_society_')) {
    const id = normalize(normalized.replace('can_manage_society_', ''));
    return id ? `society:${id}:manage` : '';
  }
  if (normalized.startsWith('society:') && normalized.endsWith(':manage')) {
    const id = normalized.replace('society:', '').replace(':manage', '').trim();
    if (!id) return '';
    return id === '*' ? 'society:*:manage' : `society:${normalize(id)}:manage`;
  }
  return '';
}

function toCanonical(raw) {
  const normalized = normalize(raw);
  if (!normalized) return '';

  if (normalized === '*:*:*') return '*:*:*';

  const societyPermission = normalizeSocietyPermission(normalized);
  if (societyPermission) return societyPermission;

  switch (normalized) {
    case 'meetings:manage':
    case 'events:create:own':
    case 'events:create:all':
    case 'events:edit:own':
    case 'events:edit:all':
    case 'events:delete:own':
    case 'events:delete:all':
    case 'events:create':
    case 'events:edit':
    case 'events:delete':
    case 'event:create':
    case 'can_manage_events':
      return 'meetings:manage';

    case 'notes:manage':
    case 'notes:upload:own':
    case 'notes:upload:all':
    case 'notes:edit:own':
    case 'notes:delete:all':
    case 'notes:moderate:all':
    case 'notes:upload':
    case 'notes:edit':
    case 'notes:delete':
    case 'can_manage_notes':
      return 'notes:manage';

    case 'placements:manage':
    case 'placements:add:all':
    case 'placements:edit:all':
    case 'placements:delete:all':
    case 'placements:create':
    case 'placements:edit':
    case 'placements:delete':
    case 'can_manage_placements':
    case 'manage_placement':
    case 'manage_placements':
      return 'placements:manage';

    case 'seniors:manage':
    case 'seniors:add:all':
    case 'seniors:edit:all':
    case 'seniors:delete:all':
    case 'seniors:verify:all':
    case 'senior:manage':
    case 'manage_senior':
    case 'manage_seniors':
      return 'seniors:manage';

    case 'admin:access':
    case 'is_admin':
    case 'admin':
      return 'admin:access';

    default:
      return '';
  }
}

function extractRawPermissions(userData) {
  const result = [];

  const permissions = userData.permissions;
  if (Array.isArray(permissions)) {
    result.push(...permissions);
  } else if (permissions && typeof permissions === 'object') {
    Object.entries(permissions).forEach(([key, value]) => {
      if (value === true) result.push(key);
    });
  }

  if (Array.isArray(userData.roles)) {
    result.push(...userData.roles);
  }

  if (userData && userData.isAdmin === true) {
    result.push('admin:access');
  }

  return result;
}

async function run() {
  console.log(isDryRun ? 'Running DRY RUN migration...' : 'Running APPLY migration...');

  const snapshot = await db.collection('users').get();
  let changed = 0;

  for (const doc of snapshot.docs) {
    const data = doc.data() || {};
    const rawPermissions = extractRawPermissions(data);

    const canonicalPermissions = Array.from(
      new Set(rawPermissions.map(toCanonical).filter(Boolean))
    ).sort();

    const previousPermissions = Array.isArray(data.permissions)
      ? data.permissions.map((p) => String(p))
      : [];

    const previousSorted = [...previousPermissions].sort();
    const samePermissions = JSON.stringify(previousSorted) === JSON.stringify(canonicalPermissions);
    const hasLegacyFields = Object.prototype.hasOwnProperty.call(data, 'roles') || Object.prototype.hasOwnProperty.call(data, 'isAdmin');

    if (samePermissions && !hasLegacyFields) {
      continue;
    }

    changed += 1;
    console.log(`- ${doc.id}`);
    console.log(`  old: ${JSON.stringify(previousPermissions)}`);
    console.log(`  new: ${JSON.stringify(canonicalPermissions)}`);

    if (!isDryRun) {
      await doc.ref.set(
        {
          permissions: canonicalPermissions,
          roles: admin.firestore.FieldValue.delete(),
          isAdmin: admin.firestore.FieldValue.delete(),
        },
        { merge: true }
      );
    }
  }

  console.log(`\nDone. ${changed} user document(s) ${isDryRun ? 'would be' : 'were'} updated.`);
  if (isDryRun) {
    console.log('Run with --apply to persist changes.');
  }
}

run()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error('Migration failed:', error);
    process.exit(1);
  });

