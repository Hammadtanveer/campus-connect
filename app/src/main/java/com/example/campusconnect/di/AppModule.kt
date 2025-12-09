package com.example.campusconnect.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cloudinary.android.MediaManager
import com.example.campusconnect.analytics.AnalyticsManager
import com.example.campusconnect.crash.CrashReporter
import com.example.campusconnect.data.local.AppDatabase
import com.example.campusconnect.data.local.EventsDao
import com.example.campusconnect.data.local.NotesDao
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.network.ConnectivityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
/**
 * Hilt module providing app-level dependencies.
 * 
 * This module provides singleton instances of Firebase services,
 * Cloudinary, and other core dependencies used throughout the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /**
     * Provides singleton instance of FirebaseAuth for authentication.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    /**
     * Provides singleton instance of FirebaseFirestore for database operations.
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    /**
     * Provides singleton instance of Cloudinary MediaManager for file uploads.
     * Note: MediaManager.get() returns the initialized singleton instance.
     */
    @Provides
    @Singleton
    fun provideCloudinaryMediaManager(): MediaManager {
        return MediaManager.get()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "campusconnect.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNotesDao(db: AppDatabase): NotesDao = db.notesDao()
    @Provides
    fun provideEventsDao(db: AppDatabase): EventsDao = db.eventsDao()

    @Provides
    @Singleton
    fun provideActivityLogRepository(): ActivityLogRepository = ActivityLogRepository()

    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return ConnectivityManager(context)
    }

    @Provides
    @Singleton
    fun provideAnalyticsManager(@ApplicationContext context: Context): AnalyticsManager {
        return AnalyticsManager(context)
    }

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter {
        return CrashReporter()
    }

    // Database migration from version 1 to 2
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns to notes table
            database.execSQL("ALTER TABLE notes ADD COLUMN cloudinaryPublicId TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE notes ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE notes ADD COLUMN lastSynced INTEGER")
            database.execSQL("ALTER TABLE notes ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE notes ADD COLUMN version INTEGER NOT NULL DEFAULT 1")

            // Add new columns to events table
            database.execSQL("ALTER TABLE events ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE events ADD COLUMN lastSynced INTEGER")
            database.execSQL("ALTER TABLE events ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")

            // Create indices
            database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_uploaderId ON notes(uploaderId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_events_organizerId ON events(organizerId)")
        }
    }
}
