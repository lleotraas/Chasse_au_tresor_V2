package fr.lleotraas.chasseautresorv2.presentation.map

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.lleotraas.chasseautresorv2.domain.use_cases.DirectionUseCases
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val useCases: DirectionUseCases
) : ViewModel() {

    private val _state = mutableStateOf(DirectionState())
    val state = _state

    fun getDirection(
        profile: String,
        coordinates: String,
        geometries: String,
        accessToken: String
    ) {

        viewModelScope.launch {

            val response = try {
                useCases.getDirection(profile, coordinates, geometries, accessToken)
            } catch (e: IOException) {
                Log.e(javaClass.simpleName, "getDirection: You may have Internet connection, ${e.message}")
                return@launch
            } catch (e: HttpException) {
                Log.e(
                    javaClass.simpleName,
                    "getDirection: HttpException, unexpected response, ${e.message}"
                )
                return@launch
            }
            Log.e(javaClass.simpleName, "getDirection: response is unsuccessful ${response.isSuccessful}")
            if (response.isSuccessful && response.body() != null) {
                Log.e(javaClass.simpleName, "getDirection: response is successful")
                val direction = response.body()
//                _state.value = state.value.copy(
//                    route = direction?.routes ?: emptyList()
//                )
            }
        }
    }

    fun event(event: MapEvent) {
        when(event) {
           is MapEvent.AddMarker -> {
               state.value.pointList.add(event.point)
                _state.value = state.value.copy(
                    pointList = state.value.pointList
                )
            }
            is MapEvent.RemoveLastMarkers -> {
                if (state.value.pointList.isNotEmpty()) {
                    state.value.pointList.remove(state.value.pointList.last())
                }
                _state.value = state.value.copy(
                    pointList = state.value.pointList
                )
            }
            is MapEvent.UpdateRoutes -> {
//                if(state.value.route.isNotEmpty()) {
//                    state.value.route.add(event.route)
//                }
                _state.value = state.value.copy(
                    routes = event.routes
                )
            }
            is MapEvent.UpdateLastKnownPosition -> {
                _state.value = state.value.copy(
                    lastKnownLocation = event.location
                )
            }
        }
    }

}