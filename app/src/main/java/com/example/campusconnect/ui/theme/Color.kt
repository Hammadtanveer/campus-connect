@file:Suppress("unused")

package com.example.campusconnect.ui.theme

import androidx.compose.ui.graphics.Color

// Primary colors (sign-in page)
val PrimaryLight = Color(0xFF137FEC) // Sign-in primary (light)
val PrimaryDark = Color(0xFF3B82F6)  // Sign-in primary (dark)

// Primary container
val PrimaryContainerLight = Color(0xFFDCEEFF)
val PrimaryContainerDark = Color(0xFF0D3B66)

// Backgrounds
val BackgroundLight = Color(0xFFF6F7F8)
val BackgroundDark = Color(0xFF101922)

// Text
val TextPrimaryLight = Color(0xFF0F172A)
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryLight = Color(0xFF64748B)
val TextSecondaryDark = Color(0xFF9CA3AF)

// Borders / outlines
val BorderLight = Color(0xFFCBD5E1)
val BorderDark = Color(0xFF374151)

// Surface (cards, fields)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF0B1116)

// Tertiary accents
val TertiaryLight = Color(0xFF8B5CF6)
val TertiaryDark = Color(0xFFB794F4)

// Surface tint (use primary)
val SurfaceTintLight = PrimaryLight
val SurfaceTintDark = PrimaryDark

// Success / Error
val Success = Color(0xFF4CAF50)
val Error = Color(0xFFEF4444)

// Additional helpful roles for Material3 mapping and accessibility
val OnPrimaryContainerLight = TextPrimaryLight
val OnPrimaryContainerDark = TextPrimaryDark

val OnTertiaryLight = Color.White
val OnTertiaryDark = Color.White

// Inverse surface (used for elevated surfaces / snackbars)
val InverseSurfaceLight = Color(0xFF0B1216)
val InverseSurfaceDark = Color(0xFFF5F7FA)
val InverseOnSurfaceLight = Color(0xFFF9FAFB)
val InverseOnSurfaceDark = Color(0xFF0F172A)

// Useful neutral
val Transparent = Color(0x00000000)
