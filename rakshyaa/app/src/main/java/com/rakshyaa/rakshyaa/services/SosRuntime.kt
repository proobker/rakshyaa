package com.rakshyaa.rakshyaa.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Runtime state is set only after the foreground service actually starts. */
object SosRuntime {
    data class State(val active: Boolean = false, val error: String? = null)
    private val mutable = MutableStateFlow(State())
    val state = mutable.asStateFlow()
    fun started() { mutable.value = State(active = true) }
    fun stopped() { mutable.value = State() }
    fun failed(message: String) { mutable.value = State(error = message) }
}
