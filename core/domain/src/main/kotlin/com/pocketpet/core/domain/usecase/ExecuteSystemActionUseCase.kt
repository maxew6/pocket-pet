package com.pocketpet.core.domain.usecase

import com.pocketpet.core.domain.action.PetActionExecutor
import com.pocketpet.core.model.ActionResult
import com.pocketpet.core.model.LaunchAppRequest
import com.pocketpet.core.model.SystemAction

class ExecuteSystemActionUseCase(
    private val executor: PetActionExecutor,
) {
    suspend operator fun invoke(action: SystemAction, launchAppRequest: LaunchAppRequest? = null): ActionResult =
        executor.execute(action, launchAppRequest)
}
