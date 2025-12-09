# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*

# ===== Firebase =====
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepnames class com.google.firebase.firestore.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keepnames class com.google.firebase.auth.** { *; }

# ===== Cloudinary =====
-keep class com.cloudinary.** { *; }
-keep interface com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# ===== Data Models =====
# Keep all data model classes
-keep class com.example.campusconnect.data.models.** { *; }
-keepnames class com.example.campusconnect.data.models.** { *; }

# Keep Room entities
-keep class com.example.campusconnect.data.local.** { *; }
-keepnames class com.example.campusconnect.data.local.** { *; }

# ===== Hilt/Dagger =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt generated components
-keep class **_HiltModules** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ===== Kotlin =====
# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations

# Kotlin serialization
-keepattributes InnerClasses
-keep class kotlinx.serialization.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== Compose =====
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# ===== WorkManager =====
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# ===== Paging 3 =====
-keep class androidx.paging.** { *; }
-keep interface androidx.paging.** { *; }

# ===== Room =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract **Dao **Dao();
}

# ===== Gson (if used) =====
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ===== OkHttp & Retrofit (if used) =====
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ===== Navigation =====
-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }

# ===== ViewModels =====
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ===== Crash reporting =====
# Keep custom exception classes
-keep public class * extends java.lang.Exception

# ===== General =====
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== App Specific =====
# Keep ViewModels
-keep class com.example.campusconnect.ui.viewmodels.** { *; }
-keep class com.example.campusconnect.**ViewModel { *; }

# Keep Repositories
-keep class com.example.campusconnect.data.repository.** { *; }

# Keep sync workers
-keep class com.example.campusconnect.sync.** { *; }

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

