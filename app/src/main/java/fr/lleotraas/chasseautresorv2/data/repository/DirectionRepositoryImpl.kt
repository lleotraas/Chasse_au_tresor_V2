package fr.lleotraas.chasseautresorv2.data.repository

import fr.lleotraas.chasseautresorv2.domain.model.Direction
import fr.lleotraas.chasseautresorv2.domain.repository.DirectionRepository
import fr.lleotraas.chasseautresorv2.domain.retrofit.DirectionApi

import retrofit2.Response
import javax.inject.Inject

class DirectionRepositoryImpl @Inject constructor(
    private val api: DirectionApi
) : DirectionRepository {

    override suspend fun getDirection(
        profile: String,
        coordinates: String,
        geometries: String,
        accessToken: String
    ): Response<Direction> {
        val direction = api.getDirection(profile, coordinates, geometries, accessToken)
        direction
        return api.getDirection(
            profile,
            coordinates,
            geometries,
            accessToken
        )
    }
}