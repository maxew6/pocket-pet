package com.pocketpet.core.model

/**
 * The outcome of attempting a [SystemAction]. Every executor in `core:system` returns one of
 * these instead of a boolean or throwing — callers (ViewModels, the overlay quick menu) render
 * truthful, specific feedback instead of a generic "done"/"failed" toggle, and Pocket Pet never
 * claims an action worked when Android didn't confirm it did.
 */
sealed class ActionResult {
    /** The action was performed, or Android's own UI for it was opened successfully. */
    data class Success(val message: String) : ActionResult()

    /** The action needs a runtime or special permission the user hasn't granted yet. */
    data class RequiresPermission(val permission: String, val rationale: String) : ActionResult()

    /** Android requires the user to confirm this action themselves (e.g. system Wi-Fi toggle). */
    data class RequiresUserConfirmation(val message: String) : ActionResult()

    /** Not supported on this OS version, form factor, or configuration. */
    data class Unsupported(val reason: String) : ActionResult()

    /** No installed app can handle the intent this action needs. */
    data class NoCompatibleApplication(val message: String) : ActionResult()

    /** The action was attempted and failed. */
    data class Failed(val reason: String) : ActionResult()
}
