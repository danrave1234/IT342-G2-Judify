package com.mobile.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Utility class for handling user preferences and settings
 */
object PreferenceUtils {
    private const val PREF_NAME = "judify_preferences"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USERNAME = "username"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_TOKEN = "token"
    private const val KEY_STUDENT_ID = "student_id"
    private const val KEY_TUTOR_ID = "tutor_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_CONTACT_DETAILS = "contact_details"
    private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save user info with all available fields
     */
    fun saveUserInfo(
        context: Context,
        userId: Long?,
        studentId: Long?,
        tutorId: Long?,
        username: String?,
        firstName: String?,
        lastName: String?,
        email: String?,
        role: String?,
        token: String?
    ) {
        val editor = getPrefs(context).edit()
        userId?.let { editor.putLong(KEY_USER_ID, it) }
        studentId?.let { editor.putLong(KEY_STUDENT_ID, it) }
        tutorId?.let { editor.putLong(KEY_TUTOR_ID, it) }
        username?.let { editor.putString(KEY_USERNAME, it) }
        firstName?.let { editor.putString(KEY_FIRST_NAME, it) }
        lastName?.let { editor.putString(KEY_LAST_NAME, it) }
        email?.let { editor.putString(KEY_EMAIL, it) }
        role?.let { editor.putString(KEY_USER_ROLE, it) }
        token?.let { editor.putString(KEY_TOKEN, it) }
        editor.apply()
        
        // Log the saved role for debugging
        Log.d("PreferenceUtils", "Saved user role: $role")
    }

    /**
     * Save login state to shared preferences
     */
    fun saveLoginState(context: Context, isLoggedIn: Boolean, email: String, rememberMe: Boolean) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        editor.putString(KEY_EMAIL, email)
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe)
        editor.apply()
    }

    /**
     * Save user details to shared preferences
     */
    fun saveUserDetails(context: Context, firstName: String, lastName: String, email: String, role: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_FIRST_NAME, firstName)
        editor.putString(KEY_LAST_NAME, lastName)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_USER_ROLE, role)
        editor.apply()
    }

    /**
     * Save user ID to shared preferences
     */
    fun saveUserId(context: Context, userId: Long) {
        val editor = getPrefs(context).edit()
        editor.putLong(KEY_USER_ID, userId)
        editor.apply()
    }

    /**
     * Save studentId to shared preferences.
     */
    fun saveStudentId(context: Context, studentId: Long?) {
        val editor = getPrefs(context).edit()
        if (studentId != null) {
            editor.putLong(KEY_STUDENT_ID, studentId)
        } else {
            editor.remove(KEY_STUDENT_ID)
        }
        editor.apply()
    }

    /**
     * Save tutorId to shared preferences.
     */
    fun saveTutorId(context: Context, tutorId: Long?) {
        val editor = getPrefs(context).edit()
        if (tutorId != null) {
            editor.putLong(KEY_TUTOR_ID, tutorId)
        } else {
            editor.remove(KEY_TUTOR_ID)
        }
        editor.apply()
    }

    /**
     * Save user contact details to shared preferences
     */
    fun saveUserContactDetails(context: Context, contactDetails: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_CONTACT_DETAILS, contactDetails)
        editor.apply()
    }

    /**
     * Save user username to shared preferences
     */
    fun saveUserUsername(context: Context, username: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    /**
     * Mark that user has seen onboarding
     */
    fun setHasSeenOnboarding(context: Context) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
        editor.apply()
    }

    /**
     * Check if user has seen onboarding
     */
    fun hasSeenOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun getUserId(context: Context): Long? {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_USER_ID)) prefs.getLong(KEY_USER_ID, -1) else null
    }

    fun getStudentId(context: Context): Long? {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_STUDENT_ID)) prefs.getLong(KEY_STUDENT_ID, -1) else null
    }

    fun getTutorId(context: Context): Long? {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_TUTOR_ID)) prefs.getLong(KEY_TUTOR_ID, -1) else null
    }

    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }
    
    fun getUserUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }
    
    fun getFirstName(context: Context): String? {
        return getPrefs(context).getString(KEY_FIRST_NAME, null)
    }
    
    fun getUserFirstName(context: Context): String? {
        return getPrefs(context).getString(KEY_FIRST_NAME, null)
    }
    
    fun getLastName(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_NAME, null)
    }
    
    fun getUserLastName(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_NAME, null)
    }
    
    fun getFullName(context: Context): String {
        val firstName = getFirstName(context) ?: ""
        val lastName = getLastName(context) ?: ""
        return "$firstName $lastName".trim()
    }

    fun getEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_EMAIL, null)
    }
    
    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_EMAIL, null)
    }

    fun getUserRole(context: Context): String? {
        val role = getPrefs(context).getString(KEY_USER_ROLE, null)
        Log.d("PreferenceUtils", "Retrieved user role: $role")
        return role
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false) && getToken(context) != null
    }

    /**
     * Check if remember me is enabled
     */
    fun isRememberMeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_ME, false)
    }

    /**
     * Clear user session (logout) but keep other preferences like onboarding state
     */
    fun clearUserSession(context: Context) {
        val editor = getPrefs(context).edit()
        editor.remove(KEY_IS_LOGGED_IN)
        editor.remove(KEY_EMAIL)
        editor.remove(KEY_FIRST_NAME)
        editor.remove(KEY_LAST_NAME)
        editor.remove(KEY_USER_ROLE)
        editor.remove(KEY_CONTACT_DETAILS)
        editor.remove(KEY_USERNAME)
        editor.remove(KEY_USER_ID)
        editor.remove(KEY_STUDENT_ID)
        editor.remove(KEY_TUTOR_ID)
        editor.remove(KEY_TOKEN)
        editor.apply()
    }

    fun logout(context: Context) {
        clearUserSession(context)
    }
    
    /**
     * Save a long value with a custom key
     */
    fun saveLong(context: Context, key: String, value: Long) {
        val editor = getPrefs(context).edit()
        editor.putLong(key, value)
        editor.apply()
    }
    
    /**
     * Save a string value with a custom key
     */
    fun saveString(context: Context, key: String, value: String) {
        val editor = getPrefs(context).edit()
        editor.putString(key, value)
        editor.apply()
    }
    
    /**
     * Get a string value with a custom key
     */
    fun getString(context: Context, key: String, defaultValue: String): String {
        return getPrefs(context).getString(key, defaultValue) ?: defaultValue
    }
    
    /**
     * Save a boolean value with a custom key
     */
    fun saveBoolean(context: Context, key: String, value: Boolean) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(key, value)
        editor.apply()
    }
    
    /**
     * Get a boolean value with a custom key
     */
    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        return getPrefs(context).getBoolean(key, defaultValue)
    }
    
    /**
     * Get a long value with a custom key
     */
    fun getLong(context: Context, key: String, defaultValue: Long): Long {
        return getPrefs(context).getLong(key, defaultValue)
    }
}
