package com.mobile.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobile.R
import com.mobile.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Only add the fragment if it's not already added (e.g., on configuration change)
        if (savedInstanceState == null) {
            // Load the ProfileFragment into the container
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .commit()
        }
    }
}
