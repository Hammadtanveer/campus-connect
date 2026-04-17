package com.example.campusconnect.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirestoreAutoNotificationManager {
    private const val TAG = "FirestoreAutoNotif"
    private const val PREFS_NAME = "firestore_auto_notifications"
    private const val KEY_INIT_PREFIX = "initialized_"
    private const val KEY_IDS_PREFIX = "known_ids_"

    private val knownSocieties = listOf(
        "csss" to "CSSS",
        "hobbies_club" to "Hobbies Club",
        "tech_club" to "Tech Club",
        "sports_club" to "Sports Club",
        "cultural_society" to "Cultural Society",
        "literary_society" to "Literary Society"
    )

    private val registrations = mutableListOf<ListenerRegistration>()
    private val societySubcollections = listOf("events")
    private val societyPostRegistrations = mutableMapOf<String, ListenerRegistration>()
    private val societyNames = mutableMapOf<String, String>()
    private var started = false
    private var listenersAttached = false
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private data class Source(
        val collection: String,
        val type: String,
        val defaultTitle: String,
        val titleFields: List<String>,
        val bodyFields: List<String>
    )

    fun start(context: Context) {
        if (started) {
            Log.d(TAG, "Already started")
            return
        }
        started = true

        val appContext = context.applicationContext
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "Auth unavailable, stopping Firestore notification listeners")
                stopListeners()
            } else if (!listenersAttached) {
                Log.d(TAG, "Auth ready (uid=${currentUser.uid}), attaching Firestore notification listeners")
                attachListeners(appContext, firestore)
            }
        }
        authStateListener?.let { auth.addAuthStateListener(it) }

        if (auth.currentUser != null) {
            attachListeners(appContext, firestore)
        } else {
            Log.d(TAG, "start deferred until authenticated user is available")
        }
    }

    private fun attachListeners(appContext: Context, firestore: FirebaseFirestore) {
        if (listenersAttached) return
        listenersAttached = true

        val sources = listOf(
            Source(
                collection = "events",
                type = "events",
                defaultTitle = "New Event",
                titleFields = listOf("title", "name"),
                bodyFields = listOf("description", "details")
            ),
            Source(
                collection = "meetings",
                type = "events",
                defaultTitle = "New Meeting",
                titleFields = listOf("title", "name"),
                bodyFields = listOf("description", "details")
            ),
            Source(
                collection = "announcements",
                type = "events",
                defaultTitle = "New Announcement",
                titleFields = listOf("title", "name"),
                bodyFields = listOf("description", "details")
            ),
            Source(
                collection = "placements",
                type = "placements",
                defaultTitle = "New Placement Update",
                titleFields = listOf("role", "jobTitle", "title"),
                bodyFields = listOf("companyName", "company", "description")
            ),
            Source(
                collection = "notes",
                type = "notes",
                defaultTitle = "New Notes Uploaded",
                titleFields = listOf("title", "subject"),
                bodyFields = listOf("description", "uploaderName")
            )
        )

        sources.forEach { source ->
            Log.d(TAG, "Starting listener for collection=${source.collection} type=${source.type}")
            registrations += firestore.collection(source.collection)
                .addSnapshotListener { snapshot, error ->
                    try {
                        if (error != null) {
                            Log.e(TAG, "Listener error for ${source.collection}", error)
                            return@addSnapshotListener
                        }

                        if (snapshot == null) return@addSnapshotListener

                        handleSnapshot(appContext, source, snapshot.documents)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Listener crash prevented for ${source.collection}", t)
                    }
                }
        }

        observeSocietyPosts(appContext, firestore)

        Log.d(
            "FirestoreAutoNotif",
            "Started listeners for [events, meetings, announcements, placements, notes, societies]"
        )
    }

    private fun stopListeners() {
        registrations.forEach { it.remove() }
        registrations.clear()
        societyPostRegistrations.values.forEach { it.remove() }
        societyPostRegistrations.clear()
        listenersAttached = false
    }

    private fun observeSocietyPosts(context: Context, firestore: FirebaseFirestore) {
        Log.d("FirestoreAutoNotif", "Society listener block reached")
        Log.d(TAG, "Listening to societies collection...")

        // Seed listeners from the same society IDs shown in the app UI.
        knownSocieties.forEach { (id, name) ->
            ensureSocietyListeners(context, firestore, id, name)
        }

        // Server read keeps society discovery independent from local cache state.
        firestore.collection("societies")
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { societyDoc ->
                    val societyId = societyDoc.id
                    if (societyId.isBlank()) return@forEach

                    val societyName = firstNonBlankField(
                        societyDoc,
                        listOf("name", "title", "societyName")
                    ) ?: societyNames[societyId] ?: societyId

                    Log.d(TAG, "Society doc fields: ${societyDoc.data}")
                    ensureSocietyListeners(context, firestore, societyId, societyName)
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "SERVER get() failed for societies", error)
            }
    }

    private fun ensureSocietyListeners(
        context: Context,
        firestore: FirebaseFirestore,
        societyId: String,
        societyName: String
    ) {
        societyNames[societyId] = societyName
        Log.d("FirestoreAutoNotif", "Society listener started for: $societyId")

        societySubcollections.forEach { subcollection ->
            val registrationKey = "$societyId:$subcollection"
            if (societyPostRegistrations.containsKey(registrationKey)) {
                return@forEach
            }

            val registration = firestore.collection("societies")
                .document(societyId)
                .collection(subcollection)
                .addSnapshotListener postsListener@{ postsSnapshot, postsError ->
                    try {
                        if (postsError != null) {
                            Log.e(
                                TAG,
                                "Listener error for society=$societyId subcollection=$subcollection",
                                postsError
                            )
                            return@postsListener
                        }

                        val posts = postsSnapshot?.documents ?: return@postsListener
                        handleSocietyPostsSnapshot(context, societyId, subcollection, posts)
                    } catch (t: Throwable) {
                        Log.e(
                            TAG,
                            "Listener crash prevented for society=$societyId subcollection=$subcollection",
                            t
                        )
                    }
                }

            societyPostRegistrations[registrationKey] = registration
        }
    }

    private fun handleSnapshot(
        context: Context,
        source: Source,
        documents: List<DocumentSnapshot>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initKey = "$KEY_INIT_PREFIX${source.collection}"
        val idsKey = "$KEY_IDS_PREFIX${source.collection}"

        val initialized = prefs.getBoolean(initKey, false)
        val knownIds = prefs.getStringSet(idsKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!initialized) {
            knownIds.addAll(documents.map { it.id })
            prefs.edit()
                .putBoolean(initKey, true)
                .putStringSet(idsKey, knownIds)
                .apply()
            Log.d(TAG, "Initialized ${source.collection} baseline size=${knownIds.size}")
            return
        }

        var hasNewIds = false

        documents.forEach { document ->
            if (source.type == "placements") {
                Log.d(TAG, "Jobs doc fields: ${document.data}")
            }

            val docId = document.id
            if (!knownIds.contains(docId)) {
                knownIds.add(docId)
                hasNewIds = true

                val notificationContent = buildNotificationContent(source, document)

                NotificationHelper.showNotification(
                    context = context,
                    title = notificationContent.first,
                    body = notificationContent.second,
                    type = source.type,
                    targetId = docId
                )
            }
        }

        if (hasNewIds) {
            prefs.edit().putStringSet(idsKey, knownIds).apply()
        }
    }

    private fun firstNonBlankField(document: DocumentSnapshot, fields: List<String>): String? {
        fields.forEach { field ->
            try {
                val value = document.getString(field)?.trim()
                if (!value.isNullOrBlank()) {
                    return value
                }
            } catch (_: Exception) {
                val anyValue = document.get(field)?.toString()?.trim()
                if (!anyValue.isNullOrBlank()) {
                    return anyValue
                }
            }
        }
        return null
    }

    private fun buildNotificationContent(
        source: Source,
        document: DocumentSnapshot
    ): Pair<String, String> {
        if (source.type == "events") {
            val eventTitle = firstNonBlankField(document, listOf("title", "name")) ?: "Untitled"
            val eventDate = extractEventDate(document)
            return "New Meeting: $eventTitle on $eventDate" to "Tap to open Meetings & Announcements"
        }

        if (source.type == "notes") {
            val noteTitle = firstNonBlankField(document, listOf("title")) ?: "Untitled"
            val subject = firstNonBlankField(document, listOf("subject")) ?: "General"
            return "New Note: $noteTitle - $subject" to "Tap to open Notes"
        }

        if (source.type == "placements") {
            val jobTitle = firstNonBlankField(document, listOf("title", "jobTitle", "role")) ?: "Untitled"
            val company = firstNonBlankField(document, listOf("company", "companyName", "organization")) ?: "Unknown"
            return "New Job: $jobTitle at $company" to "Tap to open Placements"
        }

        val title = firstNonBlankField(document, source.titleFields) ?: source.defaultTitle
        val body = firstNonBlankField(document, source.bodyFields) ?: "Tap to view details"
        return title to body
    }

    private fun handleSocietyPostsSnapshot(
        context: Context,
        societyId: String,
        subcollection: String,
        documents: List<DocumentSnapshot>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initKey = "${KEY_INIT_PREFIX}society_posts_${societyId}_$subcollection"
        val idsKey = "${KEY_IDS_PREFIX}society_posts_${societyId}_$subcollection"

        val initialized = prefs.getBoolean(initKey, false)
        val knownIds = prefs.getStringSet(idsKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!initialized) {
            knownIds.addAll(documents.map { it.id })
            prefs.edit()
                .putBoolean(initKey, true)
                .putStringSet(idsKey, knownIds)
                .apply()
            Log.d(TAG, "Initialized society posts baseline for $societyId size=${knownIds.size}")
            return
        }

        var hasNewIds = false
        val societyName = societyNames[societyId] ?: societyId

        documents.forEach { postDoc ->
            if (!knownIds.contains(postDoc.id)) {
                knownIds.add(postDoc.id)
                hasNewIds = true
                Log.d("FirestoreAutoNotif", "New society post detected: ${postDoc.data}")

                val postTitle = firstNonBlankField(
                    postDoc,
                    listOf("name", "eventTitle", "title", "postTitle")
                ) ?: "New update"

                NotificationHelper.showNotification(
                    context = context,
                    title = "New event in $societyName: $postTitle",
                    body = "Tap to open",
                    type = "society",
                    targetId = postDoc.id,
                    parentId = societyId
                )
            }
        }

        if (hasNewIds) {
            prefs.edit().putStringSet(idsKey, knownIds).apply()
        }
    }

    private fun extractEventDate(document: DocumentSnapshot): String {
        val timestampDate = document.getTimestamp("dateTime")?.toDate()
        if (timestampDate != null) {
            return formatDate(timestampDate)
        }

        return firstNonBlankField(document, listOf("date", "eventDate")) ?: "Date TBA"
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}
