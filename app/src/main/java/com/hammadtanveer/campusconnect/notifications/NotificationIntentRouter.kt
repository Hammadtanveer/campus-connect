package com.hammadtanveer.campusconnect.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.hammadtanveer.campusconnect.MainActivity
import com.hammadtanveer.campusconnect.Screen

object NotificationIntentRouter {
	private const val REQUEST_CODE_OPEN_FROM_NOTIFICATION = 12001

	const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
	const val EXTRA_NOTIFICATION_TARGET_ID = "extra_notification_target_id"
	const val EXTRA_NOTIFICATION_PARENT_ID = "extra_notification_parent_id"
	const val EXTRA_NOTIFICATION_ROUTE = "extra_notification_route"

	fun createPendingIntent(
		context: Context,
		type: String?,
		targetId: String?,
		parentId: String? = null
	): PendingIntent {
		val launchIntent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			putExtra(EXTRA_NOTIFICATION_TYPE, type)
			putExtra(EXTRA_NOTIFICATION_TARGET_ID, targetId)
			putExtra(EXTRA_NOTIFICATION_PARENT_ID, parentId)
			resolveRoute(type, targetId, parentId)?.let { putExtra(EXTRA_NOTIFICATION_ROUTE, it) }
		}

		return PendingIntent.getActivity(
			context,
			REQUEST_CODE_OPEN_FROM_NOTIFICATION,
			launchIntent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
	}

	fun consumeRoute(intent: Intent?): String? {
		Log.d("NAV_DEBUG", "All intent extras: ${intent?.extras?.keySet()?.toList()}")
		Log.d("NAV_DEBUG", "type extra: ${intent?.getStringExtra("type")}")
		Log.d(
			"NAV_DEBUG",
			"EXTRA_NOTIFICATION_TYPE: ${intent?.getStringExtra(EXTRA_NOTIFICATION_TYPE)}"
		)

		val route = intent?.getStringExtra(EXTRA_NOTIFICATION_ROUTE)
			?: resolveRoute(
				intent?.getStringExtra(EXTRA_NOTIFICATION_TYPE)
					?: intent?.getStringExtra("type"),
				intent?.getStringExtra(EXTRA_NOTIFICATION_TARGET_ID)
					?: intent?.getStringExtra("targetId"),
				intent?.getStringExtra(EXTRA_NOTIFICATION_PARENT_ID)
					?: intent?.getStringExtra("parentId")
			)

		Log.d("NAV_DEBUG", "Resolved route: $route")

		if (route != null) {
			intent?.removeExtra(EXTRA_NOTIFICATION_ROUTE)
			intent?.removeExtra(EXTRA_NOTIFICATION_TYPE)
			intent?.removeExtra(EXTRA_NOTIFICATION_TARGET_ID)
			intent?.removeExtra(EXTRA_NOTIFICATION_PARENT_ID)
			intent?.removeExtra("type")
			intent?.removeExtra("targetId")
			intent?.removeExtra("parentId")
		}
		return route
	}

	fun resolveRoute(
		type: String?,
		targetId: String? = null,
		parentId: String? = null
	): String? = when (type?.trim()?.lowercase()) {
		"events" -> if (targetId.isNullOrBlank()) Screen.DrawerScreen.Events.route else "event/$targetId"
		"meetings", "announcements" -> Screen.DrawerScreen.Events.route
		"placements" -> if (targetId.isNullOrBlank()) Screen.DrawerScreen.PlacementCareer.route else "placement/$targetId"
		"society", "society_updates" -> {
			if (!parentId.isNullOrBlank() && !targetId.isNullOrBlank()) {
				"societyEvent/$parentId/$targetId"
			} else if (!parentId.isNullOrBlank()) {
				"societyEvents/$parentId/${Uri.encode("Society")}" 
			} else if (!targetId.isNullOrBlank()) {
				"societyEvents/$targetId/${Uri.encode("Society")}" 
			} else {
				Screen.DrawerScreen.Societies.route
			}
		}
		"notes" -> Screen.DrawerScreen.Notes.route
		else -> null
	}
}




