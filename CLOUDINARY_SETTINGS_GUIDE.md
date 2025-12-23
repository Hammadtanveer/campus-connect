# Cloudinary Configuration Guide

## Overview
We have updated the app to use **Authenticated Uploads** and **Signed URLs**. This ensures your files are secure (private) and can only be accessed by your app users.

## Website Changes
**Good News:** You generally **do not** need to make any changes to your Cloudinary website settings for this to work. The code now handles the security explicitly.

However, you can verify a few settings to ensure maximum security and compatibility:

### 1. Verify "Strict Transformations" (Optional)
*   Go to **Settings** (gear icon) > **Security**.
*   Look for **Strict Transformations**.
*   **Recommended:** You can leave this **Disabled** (default) or **Enabled**.
    *   If **Enabled**: It prevents anyone from generating new image versions on the fly without a signature. Our code now signs URLs, so it will work even if this is enabled.
    *   If **Disabled**: It's easier for testing.

### 2. Check "PDF/Document" Settings
*   Go to **Settings** > **Upload**.
*   Ensure there are no specific restrictions on PDF files.
*   Our code now uploads PDFs as `authenticated` resources, which bypasses most public restrictions.

### 3. Access Control (The "401" Fix)
*   The "401 Unauthorized" error happened because we were trying to access a **Private** file with a **Public** link.
*   **The Fix:** We now generate a **Signed Link** (which looks like `.../s--SIGNATURE--/...`).
*   This signature tells Cloudinary "This user is allowed to see this file".
*   **No website change is needed** for this; it's all handled by the API Key and Secret in the app.

## Summary of App Changes
1.  **Uploads**: Now marked as `type: "authenticated"`. This makes them private.
2.  **Downloads**: Now use a generated `signed_url` that includes a security token.
3.  **Storage**: You can continue using your free Cloudinary storage (25GB) securely.

## Action Required
1.  **Delete Broken Notes**: Any notes that currently give a 401 error must be deleted from the app. They cannot be fixed.
2.  **Upload New Notes**: Try uploading a new PDF. It will use the new secure system and should open correctly.

