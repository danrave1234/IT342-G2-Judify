package com.mobile.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mobile.R
import com.mobile.databinding.ActivitySplashBinding
import com.mobile.ui.login.LoginActivity
import com.mobile.ui.dashboard.LearnerDashboardActivity
import com.mobile.ui.onboarding.OnBoarding
import com.mobile.utils.PreferenceUtils

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DELAY = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system UI for full-screen experience
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup animations
        setupAnimations()
        
        // Navigate to the appropriate screen after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }
    
    private fun setupAnimations() {
        // Fade-in animation for logo
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000
        
        binding.splashLogo.startAnimation(fadeIn)
        
        // Sequential animations for text elements
        fadeIn.startOffset = 300
        binding.appNameText.startAnimation(fadeIn)
        
        val fadeInTagline = AlphaAnimation(0f, 1f)
        fadeInTagline.duration = 1000
        fadeInTagline.startOffset = 600
        binding.taglineText.startAnimation(fadeInTagline)
    }
    
    private fun navigateToNextScreen() {
        // Check if user has seen onboarding
        if (!PreferenceUtils.hasSeenOnboarding(this)) {
            // First-time user, go to onboarding
            startActivity(Intent(this, OnBoarding::class.java))
        } else if (!PreferenceUtils.isLoggedIn(this)) {
            // Not logged in, go to login
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            // Logged in, go to dashboard
            startActivity(Intent(this, LearnerDashboardActivity::class.java))
        }
        
        // Apply a fade-out transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        
        // Close this activity
        finish()
    }
} 