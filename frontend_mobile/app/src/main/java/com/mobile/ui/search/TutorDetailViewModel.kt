package com.mobile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TutorDetailViewModel(private val tutorId: Long) : ViewModel() {

    private val _tutorState = MutableStateFlow(TutorDetailState())
    val tutorState: StateFlow<TutorDetailState> = _tutorState.asStateFlow()

    private val _reviewsState = MutableStateFlow(ReviewsState())
    val reviewsState: StateFlow<ReviewsState> = _reviewsState.asStateFlow()

    fun loadTutorProfile() {
        _tutorState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val result = NetworkUtils.getTutorProfile(tutorId)

                result.fold(
                    onSuccess = { profile ->
                        _tutorState.update { 
                            it.copy(
                                tutorProfile = profile,
                                isLoading = false,
                                error = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        _tutorState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load tutor profile"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _tutorState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun loadTutorReviews() {
        _reviewsState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val result = NetworkUtils.getTutorReviews(tutorId)

                result.fold(
                    onSuccess = { reviews ->
                        _reviewsState.update { 
                            it.copy(
                                reviews = reviews,
                                isLoading = false,
                                error = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        _reviewsState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load reviews"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _reviewsState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun submitReview(rating: Int, comment: String) {
        viewModelScope.launch {
            try {
                // For demo purposes, we'll use ID 1 as the student ID
                // In a real app, you would get this from user session
                val studentId = 1L

                val review = NetworkUtils.Review(
                    id = 0, // Server will assign ID
                    tutorId = tutorId,
                    studentId = studentId,
                    rating = rating,
                    comment = comment,
                    dateCreated = "" // Server will set this
                )

                val result = NetworkUtils.createReview(review)

                result.fold(
                    onSuccess = { newReview ->
                        // Add the new review to the list
                        _reviewsState.update { current -> 
                            current.copy(
                                reviews = current.reviews + newReview
                            )
                        }
                    },
                    onFailure = { exception ->
                        // Handle error
                    }
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

data class TutorDetailState(
    val tutorProfile: NetworkUtils.TutorProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ReviewsState(
    val reviews: List<NetworkUtils.Review> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) 
