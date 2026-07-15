package com.pocketpet.core.domain.provider

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The only way domain/data code references coroutine dispatchers. Production wiring binds
 * `Dispatchers.IO`/`Dispatchers.Default`/`Dispatchers.Main.immediate`; tests bind a
 * `TestDispatcher` so coroutine-based code runs synchronously and deterministically.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}
