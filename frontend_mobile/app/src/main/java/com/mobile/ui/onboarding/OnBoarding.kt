package com.mobile.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.mobile.ui.adapters.OnboardingAdapter
import com.mobile.R
import com.mobile.ui.login.LoginActivity

class OnBoarding : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var dots: Array<View>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)
        
        // Initialize views
        viewPager = findViewById(R.id.viewPager)
        dotsLayout = findViewById(R.id.dotsLayout)
        nextButton = findViewById(R.id.nextButton)
        
        // Set up the adapter
        val adapter = OnboardingAdapter()
        viewPager.adapter = adapter
        
        // Set up dots
        setupDots(0)
        
        // Set up page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
                // Change button text on last page
                if (position == adapter.itemCount - 1) {
                    nextButton.text = "Get Started"
                } else {
                    nextButton.text = "Next"
                }
            }
        })
        
        // Set up button click listener
        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < adapter.itemCount - 1) {
                // Go to next page
                viewPager.currentItem = currentItem + 1
            } else {
                 startActivity(Intent(this, LoginActivity::class.java))
                 finish()
            }
        }
    }
    
    private fun setupDots(currentPosition: Int) {
        // Clear existing dots
        dotsLayout.removeAllViews()
        
        // Create new dots
        dots = Array(3) { View(this) }
        
        // Set up each dot
        for (i in dots.indices) {
            dots[i] = View(this)
            dots[i].setBackgroundResource(
                if (i == currentPosition) R.drawable.dot_selected else R.drawable.dot_unselected
            )
            
            // Set layout parameters
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.width = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 5
            params.height = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 5
            params.setMargins(8, 0, 8, 0)
            
            // Add dot to layout
            dotsLayout.addView(dots[i], params)
        }
    }
}