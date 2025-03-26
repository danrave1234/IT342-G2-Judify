//package com.mobile
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import androidx.activity.ComponentActivity
//import com.mobile.ui.onboarding.OnBoarding
//
//class MainActivity : ComponentActivity() {
//    private lateinit var getStartedButton: Button
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        getStartedButton = findViewById(R.id.get_started_button)
//        getStartedButton.setOnClickListener {
//            val intent = Intent(this, OnBoarding::class.java)
//            startActivity(intent)
//        }
//    }
//}
