package com.gurnoor.locationchanger.data

import kotlinx.serialization.Serializable

/** A place the user picked or saved. Coordinates stay internal — never shown to the daily user. */
@Serializable
data class SavedPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
