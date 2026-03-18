package com.example.campusconnect.data.repository

import com.example.campusconnect.data.models.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminAnalyticsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    data class AnalyticsCounts(
        val users: Int,
        val notes: Int,
        val jobs: Int,
        val events: Int,
        val societies: Int
    )

    suspend fun fetchCounts(): Resource<AnalyticsCounts> {
        return try {
            val usersCount = firestore.collection("users").get().await().size()
            val notesCount = firestore.collection("notes").get().await().size()
            val jobsCount = firestore.collection("placements").get().await().size()
            val eventsCount = firestore.collection("events").get().await().size()
            val societiesCount = firestore.collection("societies").get().await().size()

            Resource.Success(
                AnalyticsCounts(
                    users = usersCount,
                    notes = notesCount,
                    jobs = jobsCount,
                    events = eventsCount,
                    societies = societiesCount
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load analytics")
        }
    }
}

