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
    private const val KEY_USER_ID = "userId"
    private const val KEY_REMEMBER_ME = "rememberMe"
    private const val KEY_HAS_SEEN_ONBOARDING = "hasSeenOnboarding"
    private const val PREF_FILE_NAME = "com.mobile.PREFERENCES"
    private const val KEY_FIRST_NAME = "firstName"
    private const val KEY_LAST_NAME = "lastName"
    private const val KEY_EMAIL = "email"
    private const val KEY_STUDENT_ID = "studentId"
    private const val KEY_TUTOR_ID = "tutorId"
    private const val KEY_ROLE = "role"
    private const val KEY_TOKEN = "token"
    private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
    private const val KEY_FIRST_LAUNCH = "firstLaunch"
    private const val KEY_PROFILE_PICTURE_URI = "profilePictureUri"

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
     * Save user ID to shared preferences
     */
    fun saveUserId(context: Context, userId: Long) {
        val editor = getPreferences(context).edit()
        editor.putLong(KEY_USER_ID, userId)
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
    fun getUserRole(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ROLE, "LEARNER") ?: "LEARNER"
    }

    /**
     * Get user ID
     */
    fun getUserId(context: Context): Long? {
        val id = getPreferences(context).getLong(KEY_USER_ID, -1L)
        return if (id == -1L) null else id
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

    /**
     * Save studentId to shared preferences.
     * @param context The context.
     * @param studentId The studentId to save.
     */
    fun saveStudentId(context: Context, studentId: Long?) {
        val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        if (studentId != null) {
            editor.putLong(KEY_STUDENT_ID, studentId)
        } else {
            editor.remove(KEY_STUDENT_ID)
        }
        editor.apply()
    }

    /**
     * Save tutorId to shared preferences.
     * @param context The context.
     * @param tutorId The tutorId to save.
     */
    fun saveTutorId(context: Context, tutorId: Long?) {
        val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        if (tutorId != null) {
            editor.putLong(KEY_TUTOR_ID, tutorId)
        } else {
            editor.remove(KEY_TUTOR_ID)
        }
        editor.apply()
    }

    /**
     * Get studentId from shared preferences.
     * @param context The context.
     * @return The studentId or null if not set.
     */
    fun getStudentId(context: Context): Long? {
        val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        if (!sharedPreferences.contains(KEY_STUDENT_ID)) {
            return null
        }
        return sharedPreferences.getLong(KEY_STUDENT_ID, -1)
    }

    /**
     * Get tutorId from shared preferences.
     * @param context The context.
     * @return The tutorId or null if not set.
     */
    fun getTutorId(context: Context): Long? {
        val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        if (!sharedPreferences.contains(KEY_TUTOR_ID)) {
            return null
        }
        return sharedPreferences.getLong(KEY_TUTOR_ID, -1)
    }
} 
