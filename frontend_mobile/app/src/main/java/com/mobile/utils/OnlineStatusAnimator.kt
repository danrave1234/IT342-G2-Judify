package com.mobile.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Utility class to add smooth pulsing animation to online status indicators
 */
object OnlineStatusAnimator {
    
    /**
     * Apply pulsing animation to a View that serves as an online status indicator
     * @param view The view to animate (usually a small circle view)
     * @param scale Maximum scale factor for the pulse (1.0 = no scaling)
     * @param duration Duration of one pulse cycle in milliseconds
     */
    fun applyPulseAnimation(view: View, scale: Float = 1.3f, duration: Long = 1500) {
        // Create scale animations for X and Y axes
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, scale)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, scale)
        
        // Create alpha animation for fading effect
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.7f, 0.3f)
        
        // Set animation properties
        scaleX.duration = duration
        scaleY.duration = duration
        alpha.duration = duration
        
        // Set animation to repeat forever
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        alpha.repeatCount = ObjectAnimator.INFINITE
        
        // Set animation to reverse direction when it reaches the end
        scaleX.repeatMode = ObjectAnimator.REVERSE
        scaleY.repeatMode = ObjectAnimator.REVERSE
        alpha.repeatMode = ObjectAnimator.REVERSE
        
        // Use acceleration/deceleration for smoother animation
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        alpha.interpolator = AccelerateDecelerateInterpolator()
        
        // Combine animations into one set
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        
        // Start animation
        animatorSet.start()
    }
    
    /**
     * Stop any animations running on the view
     * @param view The view to stop animating
     */
    fun stopAnimation(view: View) {
        view.clearAnimation()
        view.animate().cancel()
    }
} 