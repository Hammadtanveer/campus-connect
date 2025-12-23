# Fix for Cloudinary 401 Error (Client-Side Solution)

Since deploying the Cloud Function failed (due to the Firebase Blaze plan requirement), we have implemented a **client-side solution** to generate the signed URLs directly in your Android app.

This method works immediately without needing to upgrade your Firebase plan.

## How it Works

We added a helper function `getSignedPdfUrl` to your `CloudinaryConfig` object. This function:
1.  Takes the `public_id` of the PDF.
2.  Generates a secure signature using your API Secret (which is already in `CloudinaryConfig`).
3.  Returns a special `api.cloudinary.com` download URL that bypasses the 401 error.

## How to Use It

In your code where you are trying to open the PDF (e.g., in your `NoteDetailScreen` or `PdfViewer`), replace the old URL generation with this:

```kotlin
import com.example.campusconnect.util.CloudinaryConfig

// ... inside your function ...

val publicId = "your_pdf_public_id" // e.g. from note.pdfUrl or note.publicId

// Generate the working URL
val signedUrl = CloudinaryConfig.getSignedPdfUrl(publicId)

Log.d("PDF", "Opening Signed URL: $signedUrl")

// Now use this 'signedUrl' to open the PDF
// e.g. intent to open browser, or PDF viewer
val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(signedUrl))
context.startActivity(browserIntent)
```

## Important Note on Security

This method requires your **Cloudinary API Secret** to be stored in the Android app code (`CloudinaryConfig.kt`).
-   **Risk:** If someone decompiles your app, they could find your API Secret.
-   **Mitigation:** For a student project or internal app, this is often acceptable. For a production app on the Play Store, you should eventually upgrade to the Blaze plan and move this logic to the Cloud Function (as originally planned).

## Verification

You can verify this works by running the app. The `getSignedPdfUrl` function implements the exact same logic that we verified with the `debug_cloudinary_final_check.js` script.

