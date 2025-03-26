package com.mobile.ui.search

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.*

// Data class for search state
data class SearchState(
    val tutors: List<TutorSearchItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLocationLoading: Boolean = false,
    val error: String? = null
)

// Data class for tutor search item
data class TutorSearchItem(
    val id: Long,
    val name: String,
    val expertise: String,
    val pricePerHour: Double,
    val rating: Double,
    val distance: Double? = null
)

class TutorSearchViewModel : ViewModel() {
    
    // State for search results and loading
    private val _searchState = MutableLiveData(SearchState())
    val searchState: LiveData<SearchState> = _searchState
    
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null
    
    /**
     * Search for tutors by expertise
     */
    fun searchTutors(query: String) {
        _searchState.value = _searchState.value?.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val result = NetworkUtils.findTutorsByExpertise(query)
                
                result.fold(
                    onSuccess = { tutors ->
                        val tutorItems = tutors.map { tutor ->
                            TutorSearchItem(
                                id = tutor.id,
                                name = "Tutor ${tutor.userId}", // In a real app, get tutor's name
                                expertise = tutor.expertise,
                                pricePerHour = tutor.pricePerHour,
                                rating = tutor.rating,
                                distance = calculateDistance(tutor)
                            )
                        }.sortedWith(
                            // Sort by distance if available, then by rating
                            compareBy<TutorSearchItem> { it.distance ?: Double.MAX_VALUE }
                                .thenByDescending { it.rating }
                        )
                        
                        _searchState.postValue(
                            _searchState.value?.copy(
                                tutors = tutorItems,
                                isLoading = false,
                                error = null
                            )
                        )
                    },
                    onFailure = { exception ->
                        _searchState.postValue(
                            _searchState.value?.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to search tutors"
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                _searchState.postValue(
                    _searchState.value?.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                )
            }
        }
    }
    
    /**
     * Get current location
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context) {
        _searchState.value = _searchState.value?.copy(isLocationLoading = true)
        
        try {
            val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java)
            
            // In a real app, use FusedLocationProviderClient for better accuracy
            // For simplicity, we're using last known location
            val lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            
            if (lastKnownLocation != null) {
                currentLocation = lastKnownLocation
                
                // Refresh search with location filter
                searchTutors(_searchState.value?.tutors?.firstOrNull()?.expertise ?: "")
            } else {
                _searchState.value = _searchState.value?.copy(isLocationLoading = false)
            }
        } catch (e: Exception) {
            _searchState.value = _searchState.value?.copy(isLocationLoading = false)
        }
    }
    
    /**
     * Clear location filter
     */
    fun clearLocationFilter() {
        currentLocation = null
        
        // Convert tutors list to one without distance
        val currentTutors = _searchState.value?.tutors ?: emptyList()
        _searchState.value = _searchState.value?.copy(
            tutors = currentTutors.map { it.copy(distance = null) }
                .sortedByDescending { it.rating }
        )
    }
    
    /**
     * Calculate distance between user and tutor
     * Note: In a real app, use tutor's actual location, not a mock one
     */
    private fun calculateDistance(tutor: NetworkUtils.TutorProfile): Double? {
        // If user location is not available, return null
        currentLocation ?: return null
        
        // In a real app, get tutor's location from the backend
        // For demo, create random location within 15km
        val tutorLocation = Location("mock").apply {
            latitude = currentLocation!!.latitude + (Math.random() - 0.5) * 0.2 // ~15km
            longitude = currentLocation!!.longitude + (Math.random() - 0.5) * 0.2 // ~15km
        }
        
        // Calculate distance
        val distanceInMeters = currentLocation!!.distanceTo(tutorLocation)
        return (distanceInMeters / 1000.0) // Convert to km
    }
    
    override fun onCleared() {
        super.onCleared()
        fusedLocationClient = null
    }
} 