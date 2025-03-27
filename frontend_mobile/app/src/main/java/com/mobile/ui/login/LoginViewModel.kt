package com.mobile.ui.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mobile.data.model.AuthResponse
import com.mobile.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository()
    private val sharedPreferences = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun login(email: String, password: String, rememberMe: Boolean) {
        if (email.isEmpty() || password.isEmpty()) {
            _error.value = "Please fill in all fields"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)
                handleLoginResponse(response, rememberMe)
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred"
                _loading.value = false
            }
        }
    }

    private fun handleLoginResponse(response: AuthResponse, rememberMe: Boolean) {
        if (response.success) {
            // Use PreferenceUtils to save user data consistently
            val context = getApplication<Application>()
            com.mobile.utils.PreferenceUtils.saveLoginState(context, true, response.email ?: "", rememberMe)
            com.mobile.utils.PreferenceUtils.saveUserDetails(
                context, 
                response.firstName ?: "", 
                response.lastName ?: "", 
                response.role ?: "LEARNER"
            )

            // Also save to local SharedPreferences for backward compatibility
            sharedPreferences.edit().apply {
                putLong("user_id", response.userId ?: -1)
                putString("user_email", response.email)
                putString("user_first_name", response.firstName)
                putString("user_last_name", response.lastName)
                putString("user_role", response.role)
                putBoolean("is_logged_in", true)
                putBoolean("remember_me", rememberMe)
                apply()
            }
            _loginResult.value = true
        } else {
            _error.value = response.message ?: "Login failed"
        }
        _loading.value = false
    }

    fun checkRememberMe(): Boolean {
        return sharedPreferences.getBoolean("remember_me", false)
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    fun saveUserSession(response: AuthResponse, rememberMe: Boolean) {
        // Use PreferenceUtils to save user data consistently
        val context = getApplication<Application>()
        com.mobile.utils.PreferenceUtils.saveLoginState(context, true, response.email ?: "", rememberMe)
        com.mobile.utils.PreferenceUtils.saveUserDetails(
            context, 
            response.firstName ?: "", 
            response.lastName ?: "", 
            response.role ?: "LEARNER"
        )

        // Also save to local SharedPreferences for backward compatibility
        sharedPreferences.edit().apply {
            putLong("user_id", response.userId ?: -1)
            putString("user_email", response.email)
            putString("user_first_name", response.firstName)
            putString("user_last_name", response.lastName)
            putString("user_role", response.role)
            putBoolean("is_logged_in", true)
            putBoolean("remember_me", rememberMe)
            apply()
        }
        _loginResult.value = true
    }
}
