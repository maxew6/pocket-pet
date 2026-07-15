package com.pocketpet.core.domain.action

import com.pocketpet.core.model.ActionResult
import com.pocketpet.core.model.LaunchAppRequest
import com.pocketpet.core.model.SystemAction

/**
 * Executes a [SystemAction] through the narrowest public Android API or safe intent that can
 * fulfill it, and always returns a truthful [ActionResult] — see that type's documentation.
 * Implemented in `core:system`; this interface itself has no Android framework dependency, so
 * `feature:*` modules can depend on it without pulling in `Context`.
 */
interface PetActionExecutor {
    suspend fun execute(action: SystemAction, launchAppRequest: LaunchAppRequest? = null): ActionResult
}
