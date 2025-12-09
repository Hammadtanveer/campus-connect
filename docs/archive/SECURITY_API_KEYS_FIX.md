# 🔐 Security Fix: Move API Keys from Code

## ⚠️ CRITICAL: Before Pushing to GitHub

Your Cloudinary API keys are **hardcoded in source code**. This is a **security risk** and must be fixed before pushing to GitHub!

---

## 🚨 Current Problem

**File:** `app/src/main/java/com/example/campusconnect/util/CloudinaryConfig.kt`

```kotlin
// ❌ SECURITY RISK - These will be visible on GitHub!
private const val CLOUD_NAME = "dkxunmucg"
private const val API_KEY = "xxxxx"
private const val API_SECRET = "xxxxx"
```

**File:** `app/src/main/java/com/example/campusconnect/util/CloudinaryTransformations.kt`

```kotlin
// Also needs updating
private const val CLOUD_NAME = "your-cloud-name"
```

---

## ✅ Solution: Use BuildConfig (Recommended)

### Step 1: Add to gradle.properties (Local Only)

1. Open `gradle.properties` in project root
2. Add these lines:

```properties
# Cloudinary Configuration (DO NOT COMMIT REAL VALUES)
CLOUDINARY_CLOUD_NAME=dkxunmucg
CLOUDINARY_API_KEY=your_actual_api_key
CLOUDINARY_API_SECRET=your_actual_api_secret
```

3. Ensure `gradle.properties` is in `.gitignore` ✅ (Already done!)

### Step 2: Update .gitignore

Already done! ✅ The `.gitignore` file now excludes:
```
gradle.properties
local.properties
```

### Step 3: Update app/build.gradle.kts

Add this to `android { defaultConfig { } }` block:

```kotlin
android {
    defaultConfig {
        // ... existing code ...
        
        // Cloudinary configuration from gradle.properties
        val cloudinaryCloudName: String = project.findProperty("CLOUDINARY_CLOUD_NAME") as String? ?: "default-cloud"
        val cloudinaryApiKey: String = project.findProperty("CLOUDINARY_API_KEY") as String? ?: "default-key"
        val cloudinaryApiSecret: String = project.findProperty("CLOUDINARY_API_SECRET") as String? ?: "default-secret"
        
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"$cloudinaryApiKey\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"$cloudinaryApiSecret\"")
    }
}
```

### Step 4: Update CloudinaryConfig.kt

Replace the hardcoded values:

```kotlin
package com.example.campusconnect.util

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.example.campusconnect.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudinaryConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ✅ SECURE: Read from BuildConfig instead of hardcoding
    private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val API_KEY = BuildConfig.CLOUDINARY_API_KEY
    private val API_SECRET = BuildConfig.CLOUDINARY_API_SECRET

    private val TAG = "CloudinaryConfig"

    init {
        initializeCloudinary()
    }

    private fun initializeCloudinary() {
        try {
            val config = hashMapOf(
                "cloud_name" to CLOUD_NAME,
                "api_key" to API_KEY,
                "api_secret" to API_SECRET,
                "secure" to true
            )

            MediaManager.init(context, config)
            Log.d(TAG, "Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Cloudinary", e)
        }
    }

    fun isConfigured(): Boolean {
        return CLOUD_NAME != "default-cloud" &&
               API_KEY != "default-key" &&
               API_SECRET != "default-secret"
    }
}
```

### Step 5: Update CloudinaryTransformations.kt

```kotlin
package com.example.campusconnect.util

import com.example.campusconnect.BuildConfig

object CloudinaryTransformations {
    // ✅ SECURE: Use BuildConfig
    private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME

    fun getProfileImageUrl(
        publicId: String,
        width: Int = 300,
        height: Int = 300,
        crop: String = "fill",
        gravity: String = "face"
    ): String {
        return "https://res.cloudinary.com/$CLOUD_NAME/image/upload/" +
                "w_$width,h_$height,c_$crop,g_$gravity/$publicId"
    }

    // ... rest of the methods stay the same ...
}
```

### Step 6: Create gradle.properties.example (Template)

Create `gradle.properties.example` (safe to commit):

```properties
# Cloudinary Configuration
# Copy this file to gradle.properties and fill in your actual values
# DO NOT commit gradle.properties with real credentials!

CLOUDINARY_CLOUD_NAME=your_cloud_name_here
CLOUDINARY_API_KEY=your_api_key_here
CLOUDINARY_API_SECRET=your_api_secret_here
```

---

## 🧪 Test the Changes

```powershell
# Clean build
./gradlew clean

# Build
./gradlew build

# Verify BuildConfig was generated
# Check: app/build/generated/source/buildConfig/debug/com/example/campusconnect/BuildConfig.java
```

---

## ✅ Verification Checklist

Before pushing to GitHub:

- [ ] gradle.properties created with real credentials
- [ ] gradle.properties added to .gitignore ✅
- [ ] app/build.gradle.kts updated with buildConfigField
- [ ] CloudinaryConfig.kt updated to use BuildConfig
- [ ] CloudinaryTransformations.kt updated to use BuildConfig
- [ ] gradle.properties.example created (template)
- [ ] Build successful
- [ ] App runs and uploads work
- [ ] Search code for hardcoded credentials:
  ```powershell
  git grep -i "dkxunmucg"  # Should return 0 results
  git grep -i "api_key"    # Only in comments/examples
  ```

---

## 🚀 For GitHub Collaborators

Add to README.md:

```markdown
## 🔐 Setup Cloudinary

1. Copy `gradle.properties.example` to `gradle.properties`
2. Get Cloudinary credentials from project admin
3. Fill in the values in `gradle.properties`
4. Never commit `gradle.properties`!
```

---

## 🎯 Summary

### Before:
```kotlin
❌ private const val CLOUD_NAME = "dkxunmucg"  // Exposed!
❌ private const val API_KEY = "xxxxx"         // Exposed!
❌ private const val API_SECRET = "xxxxx"      // Exposed!
```

### After:
```kotlin
✅ private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
✅ private val API_KEY = BuildConfig.CLOUDINARY_API_KEY
✅ private val API_SECRET = BuildConfig.CLOUDINARY_API_SECRET
```

### Result:
- ✅ Credentials stored in local `gradle.properties` (gitignored)
- ✅ Safe to push to GitHub
- ✅ Easy for team to set up (copy .example file)
- ✅ No secrets in git history

---

## 🔄 Next Steps

1. **Fix Now:** Follow steps 1-6 above
2. **Test:** Verify build and app functionality
3. **Push:** Once verified, safe to push to GitHub
4. **Team:** Share credentials securely (not via Git)

---

**Estimated Time:** 15-20 minutes  
**Priority:** CRITICAL - Do this before first Git push!

