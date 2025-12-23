# Fix for 401 Unauthorized Error

## Issue
Users reported `HTTP ERROR 401` (Unauthorized) when trying to access uploaded PDFs.
This indicates that the files were being uploaded to a restricted bucket (likely `raw` files are restricted on this Cloudinary account) or the access mode was not correctly set to public.

## Fix Applied
Modified `app/src/main/java/com/example/campusconnect/data/repository/NotesRepository.kt`:
1.  **Changed `resource_type` to `"image"`**:
    *   Previously set to `"auto"`, which was defaulting to `"raw"` for PDFs.
    *   The `raw` bucket appears to be restricted/private on this account.
    *   The `image` bucket is confirmed public (sample image works).
    *   Cloudinary supports storing PDFs as "images" (it allows generating thumbnails, but the original file is preserved).
2.  **Explicitly set `"type"` to `"upload"`**:
    *   This ensures the file is marked as a public upload, not `private` or `authenticated`.

## Action Required
*   **Delete the broken note**: The note giving the 401 error cannot be fixed remotely. Please delete it.
*   **Re-upload**: Upload the file again. It will now be stored in the public `image` bucket and should open correctly without authentication errors.

