package com.prove2break.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.prove2break.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Surfaces the launchable, student-facing installed apps for the app-selection
 * screen. Queries apps that expose a launcher entry point (the same universe
 * the home-screen app drawer shows) rather than every installed package, so
 * background system components, services and libraries never clutter the
 * selection list. Prove2Break's own package is always excluded — restricting
 * itself would be nonsensical. See APP-RESTRICTION.md's Manual App Selection.
 */
@Singleton
class InstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun getInstallableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        resolved
            .asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filterNot { it == context.packageName }
            .mapNotNull { packageName ->
                runCatching {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    InstalledAppInfo(
                        packageName = packageName,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = runCatching { pm.getApplicationIcon(packageName) }.getOrNull(),
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
