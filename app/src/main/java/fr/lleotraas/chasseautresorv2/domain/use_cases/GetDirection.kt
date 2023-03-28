package fr.lleotraas.chasseautresorv2.domain.use_cases

import fr.lleotraas.chasseautresorv2.domain.model.Direction
import fr.lleotraas.chasseautresorv2.domain.repository.DirectionRepository
import retrofit2.Response

class GetDirection(
    private val repository: DirectionRepository
) {
    suspend operator fun invoke(
        profile: String,
        coordinates: String,
        geometries: String,
        accessToken: String
    ): Response<Direction> {
        return repository.getDirection(
            profile, coordinates, geometries, accessToken
        )
    }
}