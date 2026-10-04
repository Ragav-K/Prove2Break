package com.prove2break.domain.model

import android.graphics.drawable.Drawable

/** A launchable installed app surfaced by the app-selection screen (icon loaded lazily by the UI). */
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)
