package com.mobile.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class for handling shared preferences
 */
object PreferenceUtils {

    private const val PREF_NAME = "JudifyPrefs"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_USER_EMAIL = "userEmail"
    private const val KEY_USER_FIRST_NAME = "userFirstName"
    private const val KEY_USER_LAST_NAME = "userLastName"
    private const val KEY_USER_ROLE = "userRole"
    private const val KEY_USER_CONTACT_DETAILS = "userContactDetails"
    private const val KEY_USER_USERNAME = "userUsername"
    private const val KEY_USER_BIO = "userBio"
    private const val KEY_REMEMBER_ME = "rememberMe"
    private const val KEY_HAS_SEEN_ONBOARDING = "hasSeenOnboarding"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save login state to shared preferences
     */
    fun saveLoginState(context: Context, isLoggedIn: Boolean, email: String, rememberMe: Boolean) {
        val editor = getPreferences(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe)
        editor.apply()
    }

    /**
     * Save user details to shared preferences
     */
    fun saveUserDetails(context: Context, firstName: String, lastName: String, email: String, role: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_USER_FIRST_NAME, firstName)
        editor.putString(KEY_USER_LAST_NAME, lastName)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_ROLE, role)
        editor.apply()
    }

    /**
     * Save user contact details to shared preferences
     */
    fun saveUserContactDetails(context: Context, contactDetails: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_USER_CONTACT_DETAILS, contactDetails)
        editor.apply()
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Check if remember me is enabled
     */
    fun isRememberMeEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_REMEMBER_ME, false)
    }

    /**
     * Get user email
     */
    fun getUserEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_EMAIL, null)
    }

    /**
     * Get user first name
     */
    fun getUserFirstName(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_FIRST_NAME, null)
    }

    /**
     * Get user last name
     */
    fun getUserLastName(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_LAST_NAME, null)
    }

    /**
     * Get user role
     */
    fun getUserRole(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_ROLE, null)
    }

    /**
     * Get user contact details
     */
    fun getUserContactDetails(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_CONTACT_DETAILS, null)
    }

    /**
     * Save user username to shared preferences
     */
    fun saveUserUsername(context: Context, username: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_USER_USERNAME, username)
        editor.apply()
    }

    /**
     * Get user username
     */
    fun getUserUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_USERNAME, null)
    }

    /**
     * Save user bio to shared preferences
     */
    fun saveUserBio(context: Context, bio: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_USER_BIO, bio)
        editor.apply()
    }

    /**
     * Get user bio
     */
    fun getUserBio(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_BIO, null)
    }

    /**
     * Mark that user has seen onboarding
     */
    fun setHasSeenOnboarding(context: Context) {
        val editor = getPreferences(context).edit()
        editor.putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
        editor.apply()
    }

    /**
     * Check if user has seen onboarding
     */
    fun hasSeenOnboarding(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    /**
     * Clear user session (logout) but keep other preferences like onboarding state
     */
    fun clearUserSession(context: Context) {
        val editor = getPreferences(context).edit()
        editor.remove(KEY_IS_LOGGED_IN)
        editor.remove(KEY_USER_EMAIL)
        editor.remove(KEY_USER_FIRST_NAME)
        editor.remove(KEY_USER_LAST_NAME)
        editor.remove(KEY_USER_ROLE)
        editor.remove(KEY_USER_CONTACT_DETAILS)
        editor.remove(KEY_USER_USERNAME)
        editor.remove(KEY_USER_BIO)
        editor.apply()
    }

    /**
     * Clear all preferences (complete reset)
     */
    fun clearPreferences(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()
    }
} 
