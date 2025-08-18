package com.example.campusconnect

import androidx.annotation.DrawableRes

sealed class Screen(val title: String, val route: String) {

    sealed class BottomScreen(
        val bTitle: String,
        val bRoute: String,
        @DrawableRes val icon: Int
    ) : Screen(bTitle, bRoute) {
        object Notes : BottomScreen(
            "Notes",
            "notes",
            R.drawable.outline_book_2_24
        )
        object Seniors : BottomScreen(
            "Seniors ",
            "seniors",
            R.drawable.outline_person_raised_hand_24
        )
        object Societies : BottomScreen(
            "Societies",
            "societies",
            R.drawable.outline_person_play_24
        )
    }

    sealed class DrawerScreen(
        val dtitle: String,
        val dRoute: String,
        @DrawableRes val icon: Int
    ) : Screen(dtitle, dRoute) {
        object Profile : DrawerScreen(
            "Profile",
            "profile",
            R.drawable.outline_account_circle_24 // <- use account circle
        )
        object Downlode : DrawerScreen(
            "Downlode",
            "downlode",
            R.drawable.outline_book_2_24
        )
        object AddAccount : DrawerScreen(
            "Add Account",
            "add account",
            R.drawable.baseline_person_add_alt_1_24
        )
    }
}

val screenInBottom = listOf(
    Screen.BottomScreen.Notes,
    Screen.BottomScreen.Seniors,
    Screen.BottomScreen.Societies
)

val screenInDrawer = listOf(
    Screen.DrawerScreen.Profile,
    Screen.DrawerScreen.Downlode,
    Screen.DrawerScreen.AddAccount
)


