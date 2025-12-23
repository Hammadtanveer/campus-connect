# Fix for Cloudinary 401 Error (Authenticated PDFs)

**UPDATE:** Since you encountered a deployment error (Blaze plan required), please refer to **[FIX_401_ERROR_CLIENT_SIDE.md](FIX_401_ERROR_CLIENT_SIDE.md)** for the immediate solution that works without upgrading your plan.

---

We have identified that standard Cloudinary delivery URLs (`res.cloudinary.com`) are returning `401 Unauthorized` for your authenticated PDFs, likely due to strict security settings or specific handling of PDF assets.

However, the **API Download URL** (`api.cloudinary.com/.../download`) works correctly when signed properly.

## The Solution

We have implemented a secure Cloud Function `generateSignedPdfUrl` that generates the correct URL for you. This avoids putting your API Secret in the Android app (which is insecure).

### Step 1: Deploy the Cloud Function

Run the following command in your terminal to deploy the new function:

```bash
cd cloud-functions
npm install
firebase deploy --only functions:generateSignedPdfUrl
```

### Step 2: Update Android App

In your Android app, instead of generating the URL locally using `MediaManager`, call the Cloud Function.

**Kotlin Example:**

```kotlin
import com.google.firebase.functions.FirebaseFunctions

fun getPdfUrl(publicId: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
    val functions = FirebaseFunctions.getInstance()
    
    val data = hashMapOf(
        "publicId" to publicId
    )

    functions
        .getHttpsCallable("generateSignedPdfUrl")
        .call(data)
        .addOnSuccessListener { result ->
            val resultData = result.data as Map<String, Any>
            val url = resultData["url"] as String
            onSuccess(url)
        }
        .addOnFailureListener { exception ->
            onError(exception)
        }
}

// Usage:
getPdfUrl("your_pdf_public_id", 
    onSuccess = { url ->
        // Use this URL to open the PDF
        Log.d("PDF", "Signed URL: $url")
        // openPdf(url)
    },
    onError = { e ->
        Log.e("PDF", "Error getting URL", e)
    }
)
```

### Cloudinary Website Changes

You do **not** need to make changes to the Cloudinary website settings if you use this method. The `401` error on standard URLs is bypassed by using the authenticated API download endpoint.

If you still wish to use standard URLs (`res.cloudinary.com`), you would need to investigate "Strict Transformations" or "Access Control" settings in your Cloudinary Dashboard (Settings > Security), but this is often complex to debug without direct access. The Cloud Function approach is robust and secure.

## Verification

We have verified this fix using `debug_cloudinary_final_check.js` which now passes successfully.

