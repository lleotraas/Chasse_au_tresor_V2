package fr.lleotraas.chasseautresorv2.domain.retrofit

import fr.lleotraas.chasseautresorv2.domain.model.Direction
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DirectionApi {

    @GET("directions/v5/mapbox/{profile}/{coordinates}")
    suspend fun getDirection(
        @Path("profile") profile: String,
        @Path("coordinates") coordinates: String,
        @Query("geometries") geometries: String,
        @Query("access_token") accessToken: String
    ): Response<Direction>

}