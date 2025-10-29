package com.example.campusconnect

import androidx.annotation.DrawableRes

sealed class Screen(val title: String, val route: String) {

    sealed class DrawerScreen(
        val dTitle: String,
        val dRoute: String,
        @DrawableRes val icon: Int
    ) : Screen(dTitle, dRoute) {
        object Profile : DrawerScreen(
            "Profile",
            "profile",
            R.drawable.outline_account_circle_24
        )
        object Notes : DrawerScreen(
            "Notes",
            "notes",
            R.drawable.outline_book_2_24
        )
        object Seniors : DrawerScreen(
            "Seniors",
            "seniors",
            R.drawable.outline_person_raised_hand_24
        )
        object Societies : DrawerScreen(
            "Societies",
            "societies",
            R.drawable.outline_person_play_24
        )
        object Download : DrawerScreen(
            "Download",
            "download",
            R.drawable.outline_book_2_24
        )
        object PlacementCareer : DrawerScreen(
            "Placement & Career",
            "placement_career",
            R.drawable.outline_work_24
        )
        object Events : DrawerScreen(
            "Events",
            "events",
            R.drawable.baseline_event_24
        )
        object Mentors : DrawerScreen(
            "Find Mentors",
            "mentors",
            R.drawable.outline_person_raised_hand_24
        )
        object Mentorship : DrawerScreen(
            "Mentorship",
            "mentorship",
            R.drawable.baseline_person_24
        )
    }
}

val screenInDrawer = listOf(
    Screen.DrawerScreen.Profile,
    Screen.DrawerScreen.Notes,
    Screen.DrawerScreen.Seniors,
    Screen.DrawerScreen.Societies,
    Screen.DrawerScreen.PlacementCareer,
    Screen.DrawerScreen.Events,
    Screen.DrawerScreen.Download,
    Screen.DrawerScreen.Mentors,
    Screen.DrawerScreen.Mentorship
)
