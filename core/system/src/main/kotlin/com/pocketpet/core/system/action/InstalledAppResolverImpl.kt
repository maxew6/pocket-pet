package com.pocketpet.core.system.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.pocketpet.core.domain.action.InstalledAppResolver
import com.pocketpet.core.model.LaunchAppRequest

/**
 * Resolves launchable apps through a `LAUNCHER`-category query, matching the `<queries>` element
 * declared in the app manifest. This deliberately never asks for `QUERY_ALL_PACKAGES` — a narrow,
 * intent-scoped query is enough for "let the user pick an app to launch" and stays compliant with
 * Android's package-visibility rules.
 */
class InstalledAppResolverImpl(private val appContext: Context) : InstalledAppResolver {

    override fun resolveByName(spokenName: String): LaunchAppRequest? {
        val normalizedTarget = spokenName.trim().lowercase()
        if (normalizedTarget.isEmpty()) return null
        val apps = listLaunchableApps()
        return apps.firstOrNull { it.displayName.lowercase() == normalizedTarget }
            ?: apps.firstOrNull { it.displayName.lowercase().contains(normalizedTarget) }
    }

    override fun listLaunchableApps(): List<LaunchAppRequest> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageManager = appContext.packageManager
        val resolved = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved
            .filter { it.activityInfo.packageName != appContext.packageName }
            .map { info ->
                LaunchAppRequest(
                    packageName = info.activityInfo.packageName,
                    displayName = info.loadLabel(packageManager).toString(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.displayName.lowercase() }
    }
}
