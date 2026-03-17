#!/usr/bin/env node

const admin = require('firebase-admin');

function printHelp() {
  console.log([
    'Usage:',
    '  node send-topic-fallback.js --topic <topic> --title <title> [--body <body>] [--type <type>] [--dry-run]',
    '',
    'Examples:',
    '  node send-topic-fallback.js --topic all_students --title "New Notes Uploaded" --body "DSA Unit 1" --type notes',
    '  node send-topic-fallback.js --topic events --title "New Event Created" --body "Hackathon 2026" --type events --dry-run',
    '',
    'Notes:',
    '  - Requires GOOGLE_APPLICATION_CREDENTIALS to point to a Firebase service account JSON.',
    '  - Sends both notification and data payloads for foreground/background/killed-app delivery.',
  ].join('\n'));
}

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith('--')) {
      continue;
    }

    const key = token.slice(2);
    if (key === 'help' || key === 'dry-run') {
      args[key] = true;
      continue;
    }

    const value = argv[i + 1];
    if (typeof value === 'undefined' || value.startsWith('--')) {
      throw new Error(`Missing value for --${key}`);
    }
    args[key] = value;
    i += 1;
  }
  return args;
}

async function main() {
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    console.error(`Argument error: ${error.message}`);
    printHelp();
    process.exit(1);
  }

  if (args.help) {
    printHelp();
    return;
  }

  const topic = args.topic;
  const title = args.title;
  const body = args.body || '';
  const type = args.type || 'general';
  const dryRun = Boolean(args['dry-run']);

  if (!topic || !title) {
    console.error('Both --topic and --title are required.');
    printHelp();
    process.exit(1);
  }

  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });

  const message = {
    topic,
    notification: {
      title,
      body,
    },
    data: {
      title,
      body,
      type,
    },
    android: {
      priority: 'high',
      notification: {
        title,
        body,
        channelId: 'events',
      },
    },
    apns: {
      headers: {
        'apns-priority': '10',
      },
      payload: {
        aps: {
          alert: {
            title,
            body,
          },
          sound: 'default',
          contentAvailable: true,
        },
      },
    },
  };

  try {
    const messageId = await admin.messaging().send(message, dryRun);
    console.log(`FCM send success: ${messageId}`);
  } catch (error) {
    console.error('FCM send failed:', error.message || error);
    process.exit(1);
  }
}

main();

