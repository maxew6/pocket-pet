package com.pocketpet.core.domain.action

import com.pocketpet.core.model.LaunchAppRequest

/**
 * Resolves a spoken or typed app name to an actually-installed app. Implemented in `core:system`
 * using a narrow, user-driven package query — never an unrestricted package listing.
 */
interface InstalledAppResolver {
    fun resolveByName(spokenName: String): LaunchAppRequest?

    /** The full list of user-launchable apps, for the customization/settings app picker. */
    fun listLaunchableApps(): List<LaunchAppRequest>
}
