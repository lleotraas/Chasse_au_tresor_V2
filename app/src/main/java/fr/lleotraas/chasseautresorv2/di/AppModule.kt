package fr.lleotraas.chasseautresorv2.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.lleotraas.chasseautresorv2.data.repository.DirectionRepositoryImpl
import fr.lleotraas.chasseautresorv2.domain.repository.DirectionRepository
import fr.lleotraas.chasseautresorv2.domain.retrofit.DirectionApi
import fr.lleotraas.chasseautresorv2.domain.use_cases.DirectionUseCases
import fr.lleotraas.chasseautresorv2.domain.use_cases.GetDirection
import fr.lleotraas.chasseautresorv2.utils.Constant
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDirectionApi(): DirectionApi {
        return Retrofit.Builder()
            .baseUrl(Constant.DIRECTION_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDirectionRepository(api: DirectionApi): DirectionRepository {
        return DirectionRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideDirectionUseCases(repository: DirectionRepository): DirectionUseCases {
        return DirectionUseCases(
            GetDirection(repository)
        )
    }

}