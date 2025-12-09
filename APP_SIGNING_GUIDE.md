# App Signing Configuration Guide

## Overview
This guide explains how to configure app signing for release builds.

## Generate Release Keystore

### Using Command Line:
```bash
keytool -genkey -v -keystore campus-connect-release.keystore -alias campusconnect -keyalg RSA -keysize 2048 -validity 10000
```

### Information to Provide:
- **Password**: [Your secure password]
- **First and Last Name**: CampusConnect Team
- **Organizational Unit**: Development
- **Organization**: CampusConnect
- **City**: [Your city]
- **State**: [Your state]
- **Country Code**: [Your country]

## Keystore Location
Store the keystore file securely:
- **Development**: `local_only/campus-connect-release.keystore`
- **CI/CD**: Encrypted in repository secrets

## Gradle Configuration

### Create keystore.properties
Create a file `keystore.properties` in the project root:

```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=campusconnect
storeFile=../local_only/campus-connect-release.keystore
```

### Add to .gitignore
```
keystore.properties
local_only/*.keystore
```

### Update app/build.gradle.kts

```kotlin
// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ... existing config ...

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## Google Play App Signing

### Recommended: Use Play App Signing
1. Enable Play App Signing in Google Play Console
2. Upload your keystore or let Google generate one
3. Download the upload certificate
4. Use upload certificate for builds

### Benefits:
- Google manages your app signing key
- Lost keystore recovery possible
- Automatic key rotation
- Enhanced security

## Build Release APK

### Command Line:
```bash
./gradlew assembleRelease
```

### Output:
```
app/build/outputs/apk/release/app-release.apk
```

## Build AAB (Android App Bundle)

### Recommended for Play Store:
```bash
./gradlew bundleRelease
```

### Output:
```
app/build/outputs/bundle/release/app-release.aab
```

## Verify Signature

### Check APK signature:
```bash
jarsigner -verify -verbose -certs app-release.apk
```

### View certificate details:
```bash
keytool -list -v -keystore campus-connect-release.keystore
```

## Security Best Practices

1. **Never commit keystore** to version control
2. **Use different keystores** for debug and release
3. **Backup keystore securely** (encrypted cloud storage)
4. **Document keystore password** in secure location
5. **Enable Play App Signing** for production
6. **Rotate keys** if compromised

## CI/CD Integration

### GitHub Actions Example:
```yaml
- name: Decode keystore
  run: |
    echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks

- name: Build release
  run: ./gradlew assembleRelease
  env:
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

## Troubleshooting

### Issue: Keystore not found
**Solution**: Check path in keystore.properties

### Issue: Wrong password
**Solution**: Verify password in keystore.properties

### Issue: Signature verification failed
**Solution**: Rebuild with clean: `./gradlew clean assembleRelease`

## Version Management

Update version code and name in build.gradle.kts:
```kotlin
defaultConfig {
    versionCode = 1
    versionName = "1.0.0"
}
```

**Version Code**: Increment for every release  
**Version Name**: Semantic versioning (MAJOR.MINOR.PATCH)

