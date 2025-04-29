package com.mobile.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Base Fragment class that handles bottom padding for all fragments
 * to ensure content is not covered by the bottom navigation bar.
 */
abstract class BaseFragment : Fragment() {

    /**
     * Get the layout resource ID for the fragment
     */
    @LayoutRes
    protected abstract fun getLayoutResourceId(): Int

    /**
     * Called after the view is created. Subclasses should override this
     * to initialize their views and set up listeners.
     */
    protected open fun onViewCreated(view: View) {
        // Default implementation does nothing
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(getLayoutResourceId(), container, false)

        // Apply bottom padding to ensure content is not covered by bottom navigation
        applyBottomPadding(view)

        // Call the template method for subclasses to initialize their views
        onViewCreated(view)
        
        // Prevent search edit text auto-focusing
        disableSearchEditTextAutoFocus(view)

        return view
    }

    /**
     * Applies appropriate bottom padding to ensure content is not covered by bottom navigation.
     * This method uses WindowInsetsCompat to dynamically determine the navigation bar height.
     */
    private fun applyBottomPadding(view: View) {
        // Find the first scrollable container in the view hierarchy
        val scrollableView = findScrollableView(view)
        val targetView = scrollableView ?: view

        // Set up window insets listener to get the navigation bar height
        ViewCompat.setOnApplyWindowInsetsListener(targetView) { v, insets ->
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomInset = navigationBarInsets.bottom

            // Apply padding to the view
            if (bottomInset > 0) {
                // For scrollable views, ensure they don't clip padding
                when (v) {
                    is androidx.core.widget.NestedScrollView -> v.clipToPadding = false
                    is android.widget.ScrollView -> v.clipToPadding = false
                }

                // For MapView, we need to handle it differently
                if (v.javaClass.name == "org.osmdroid.views.MapView") {
                    // MapView might need special handling
                } else {
                    // Use updatePadding to only update the bottom padding
                    v.updatePadding(bottom = v.paddingBottom.coerceAtLeast(bottomInset))
                }
            }

            // Return the insets so they can be consumed by other listeners
            insets
        }

        // Request applying window insets
        ViewCompat.requestApplyInsets(view)
    }

    /**
     * Finds the first scrollable view in the view hierarchy.
     * This is typically a ScrollView, RecyclerView, or NestedScrollView.
     */
    private fun findScrollableView(view: View): View? {
        // Check if the view is a scrollable container
        if (view is androidx.core.widget.NestedScrollView ||
            view is androidx.recyclerview.widget.RecyclerView ||
            view is android.widget.ScrollView ||
            view.javaClass.name == "org.osmdroid.views.MapView") {
            return view
        }

        // If the view is a ViewGroup, check its children
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val scrollableChild = findScrollableView(child)
                if (scrollableChild != null) {
                    return scrollableChild
                }
            }
        }

        // No scrollable view found
        return null
    }
    
    /**
     * Recursively finds and configures all EditText views with "search" in their ID
     * to prevent auto-focus and showing keyboard
     */
    private fun disableSearchEditTextAutoFocus(view: View) {
        // If the view is an EditText with "search" in its ID
        if (view is android.widget.EditText && view.id != View.NO_ID) {
            try {
                val idString = resources.getResourceEntryName(view.id)
                if (idString.contains("search", ignoreCase = true)) {
                    // Disable focus and make not focusable in touch mode
                    view.isFocusable = false
                    view.isFocusableInTouchMode = false
                    // Enable focusing only when explicitly clicked
                    view.setOnClickListener {
                        view.isFocusableInTouchMode = true
                        view.isFocusable = true
                        view.requestFocus()
                    }
                }
            } catch (e: Exception) {
                // Resource name might not be available for all views
            }
        }
        
        // Recursively check child view groups
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                disableSearchEditTextAutoFocus(view.getChildAt(i))
            }
        }
    }
}
