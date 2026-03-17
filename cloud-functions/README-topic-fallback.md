# FCM Topic Fallback Sender

This script sends a topic notification using Firebase Admin SDK from your local machine.
It is useful when Cloud Functions deployment is blocked (for example billing setup issues).

## Prerequisites

- A Firebase service account JSON file with messaging permissions.
- `GOOGLE_APPLICATION_CREDENTIALS` set to that JSON file path.
- Dependencies installed in `cloud-functions`.

## Install

```powershell
Set-Location "D:\AndroidStudioProjects\campus-connect\cloud-functions"
npm install
```

## Usage

```powershell
Set-Location "D:\AndroidStudioProjects\campus-connect\cloud-functions"
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\path\to\service-account.json"
npm run send:topic:fallback -- --topic all_students --title "New Notes Uploaded" --body "CN Unit 3" --type notes
```

## Dry run (validation only)

```powershell
Set-Location "D:\AndroidStudioProjects\campus-connect\cloud-functions"
$env:GOOGLE_APPLICATION_CREDENTIALS="D:\path\to\service-account.json"
npm run send:topic:fallback -- --topic all_students --title "Test" --dry-run
```

## Payload format

The script sends data payload compatible with the app notification handler:

- `title`
- `body`
- `type`

