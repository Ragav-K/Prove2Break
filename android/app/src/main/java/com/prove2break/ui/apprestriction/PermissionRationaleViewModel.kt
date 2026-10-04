package com.prove2break.ui.apprestriction

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.prove2break.apprestriction.UsageAccessHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Backs the permission-explainer + honest-limitations flow shown before App
 * Restriction can be set up (APP-RESTRICTION.md's "Requirement: Explain Every
 * Sensitive Permission"). Never requests anything itself — the screen decides
 * when the student has acknowledged each explainer and only then triggers the
 * settings intent / system dialog.
 */
@HiltViewModel
class PermissionRationaleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val requiresNotificationRuntimePermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var hasSeenDisclosure: Boolean = AppRestrictionPrefs.hasSeenDisclosure(context)
        private set

    fun markDisclosureSeen() {
        AppRestrictionPrefs.markDisclosureSeen(context)
        hasSeenDisclosure = true
    }

    fun hasUsageAccess(): Boolean = UsageAccessHelper.hasUsageAccess(context)
}
