package com.example.campusconnect.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.campusconnect.R

class SeniorProfileActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the simple placeholder layout that already exists
        setContentView(R.layout.activity_senior_profile)
    }

    companion object {
        const val EXTRA_SENIOR_ID = "extra_senior_id"

        fun createIntent(context: Context, seniorId: Int): Intent {
            return Intent(context, SeniorProfileActivity::class.java).apply {
                putExtra(EXTRA_SENIOR_ID, seniorId)
            }
        }
    }
}
