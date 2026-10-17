package com.gurnoor.locationchanger.service

import kotlinx.coroutines.flow.MutableSharedFlow

/** One-off signals from the service to whatever UI is currently on screen. */
enum class ServiceEvent { STARTED, STOPPED, FAILED, LOCATION_OFF }

object ServiceBus {
    val events = MutableSharedFlow<ServiceEvent>(extraBufferCapacity = 8)
    fun emit(event: ServiceEvent) {
        events.tryEmit(event)
    }
}
