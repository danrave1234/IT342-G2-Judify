package com.mobile.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.mobile.R

/**
 * Utility class for UI operations including displaying snackbars, field validation, and user alerts
 */
object UiUtils {

    /**
     * Show a short Snackbar message
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     */
    fun showSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Show a short Snackbar message with a resource string
     * @param view The view to attach the Snackbar to
     * @param messageResId The resource ID of the message to display
     */
    fun showSnackbar(view: View, @StringRes messageResId: Int) {
        Snackbar.make(view, messageResId, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Show a long Snackbar message
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     */
    fun showLongSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Show a Snackbar message with an action
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     * @param actionText The text for the action button
     * @param action The action to perform when the button is clicked
     */
    fun showSnackbarWithAction(
        view: View,
        message: String,
        actionText: String,
        action: (View) -> Unit
    ) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setAction(actionText, action)
            .show()
    }

    /**
     * Show a colored Snackbar message (success, error, warning)
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     * @param backgroundColor The background color resource ID
     * @param textColor The text color resource ID
     */
    fun showColoredSnackbar(
        view: View,
        message: String,
        @ColorRes backgroundColor: Int,
        @ColorRes textColor: Int = R.color.white
    ) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        
        // Set background color
        snackbarView.setBackgroundColor(
            ContextCompat.getColor(view.context, backgroundColor)
        )
        
        // Find the text view and set its color
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(view.context, textColor))
        
        snackbar.show()
    }

    /**
     * Show a success Snackbar
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     */
    fun showSuccessSnackbar(view: View, message: String) {
        showColoredSnackbar(view, message, R.color.success_green)
    }

    /**
     * Show an error Snackbar
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     */
    fun showErrorSnackbar(view: View, message: String) {
        showColoredSnackbar(view, message, R.color.error_red)
    }

    /**
     * Show a warning Snackbar
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     */
    fun showWarningSnackbar(view: View, message: String) {
        showColoredSnackbar(view, message, R.color.warning_amber)
    }

    /**
     * Show an info Snackbar
     * @param view The view to attach the Snackbar to
     * @param message The message to display
     */
    fun showInfoSnackbar(view: View, message: String) {
        showColoredSnackbar(view, message, R.color.primary_blue)
    }

    /**
     * Set an error on a TextInputLayout field with validation
     * @param textInputLayout The TextInputLayout to set error on
     * @param errorMessage The error message to display, or null to clear error
     * @return true if there's no error, false if error was set
     */
    fun validateField(textInputLayout: TextInputLayout, errorMessage: String?): Boolean {
        textInputLayout.error = errorMessage
        return errorMessage == null
    }

    /**
     * Helper method to find a root view that can be used for Snackbars
     * Especially useful in fragments or when the view hierarchy is complex
     * @param context The context
     * @return A suitable root view or null if none found
     */
    fun findSuitableParent(context: Context): View? {
        return when (context) {
            is Activity -> {
                val rootView = (context as Activity).window.decorView.findViewById<View>(android.R.id.content)
                rootView
            }
            else -> null
        }
    }
} 