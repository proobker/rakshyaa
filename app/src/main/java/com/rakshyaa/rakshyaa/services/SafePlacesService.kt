package com.rakshyaa.rakshyaa.services

import com.rakshyaa.rakshyaa.data.models.SafePlace
import com.rakshyaa.rakshyaa.data.repositories.SafePlacesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafePlacesService @Inject constructor(
    private val repo: SafePlacesRepository
) {

    suspend fun nearby(
        latitude: Double,
        longitude: Double,
        radiusM: Double = 5000.0
    ): List<SafePlace> = repo.nearby(
        latitude = latitude,
        longitude = longitude,
        radiusM = radiusM
    )

    suspend fun all(): List<SafePlace> = repo.getAll()

    suspend fun addPlace(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        type: String = "user"
    ): SafePlace = repo.add(
        SafePlace(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            type = type
        )
    )

    suspend fun removePlace(id: String) {
        repo.remove(id)
    }
}