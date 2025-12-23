# Cloudinary vs Firebase Storage Analysis

## Why Cloudinary is Failing (401 Unauthorized)
The persistent `401 Unauthorized` error, even with signed URLs, indicates a fundamental mismatch between the Cloudinary account's security settings and the way the app is trying to access files.
*   **Strict Security**: Your Cloudinary account likely has "Strict Transformations" or "Authenticated Delivery" enabled globally. This means *every* access requires a specific signature or token that matches the exact transformation requested.
*   **PDF Complexity**: Cloudinary treats PDFs sometimes as images (for thumbnails) and sometimes as raw files. This duality is causing issues where the app uploads as one type but tries to read as another, or the signature doesn't match the specific "image-based" delivery of a PDF.
*   **Reliability**: While Cloudinary is excellent for image manipulation, using it for simple file storage (PDFs) in a secure Android app is proving brittle due to these complex access rules.

## Recommendation: Switch to Firebase Storage
Since your app is already deeply integrated with Firebase (Authentication, Firestore, Crashlytics), switching to **Firebase Storage** is the most logical and reliable solution.

### Benefits of Firebase Storage
1.  **Native Integration**: Works seamlessly with Firebase Auth. You can set rules like `allow read, write: if request.auth != null;` directly in the Firebase Console. No complex signature generation code needed in the app.
2.  **Simplicity**: It treats files as files. No "image vs raw" confusion. A PDF is just a blob of data.
3.  **Reliability**: It is the standard for Android apps. It just works.
4.  **Cost**: Generous free tier (5GB) similar to Cloudinary, but often cheaper at scale for simple file hosting.

## Migration Plan
1.  **Add Dependency**: Add `implementation("com.google.firebase:firebase-storage-ktx")` to `app/build.gradle.kts`.
2.  **Update Repository**: Rewrite `NotesRepository` to use `FirebaseStorage` instead of `MediaManager`.
3.  **Update UI**: The UI just needs a URL. Firebase Storage provides a simple `getDownloadUrl()` which returns a permanent, public (but token-protected) URL that works everywhere.

**I strongly recommend we make this switch now to permanently resolve the PDF access issues.**

