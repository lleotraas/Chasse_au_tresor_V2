package fr.lleotraas.chasseautresorv2.domain.repository

import fr.lleotraas.chasseautresorv2.domain.model.Direction
import retrofit2.Response

interface DirectionRepository {
    suspend fun getDirection(
        profile: String,
        coordinates: String,
        geometries: String,
        accessToken: String
    ): Response<Direction>
}