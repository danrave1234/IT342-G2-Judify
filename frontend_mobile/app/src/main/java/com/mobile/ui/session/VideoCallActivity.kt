package com.mobile.ui.session

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mobile.R

/**
 * Activity for video call sessions.
 * In a real app, this would integrate with a video call API like WebRTC, Jitsi, or Agora.
 */
class VideoCallActivity : AppCompatActivity() {

    private lateinit var joinButton: Button
    private lateinit var endCallButton: ImageButton
    private lateinit var toggleMicButton: ImageButton
    private lateinit var toggleCameraButton: ImageButton
    private lateinit var toggleScreenShareButton: ImageButton
    private lateinit var toggleSpeakerButton: ImageButton
    private lateinit var participantNameText: TextView
    private lateinit var callDurationText: TextView
    private lateinit var callStatusText: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var localVideoView: View
    private lateinit var remoteVideoView: View
    
    private var sessionId: String = ""
    private var sessionStarted = false
    private var participantName: String = ""
    private var isAudioEnabled = true
    private var isVideoEnabled = true
    private var isSpeakerOn = true
    
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        // Get intent extras
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: ""
        participantName = intent.getStringExtra(EXTRA_PARTICIPANT_NAME) ?: "Participant"
        
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Invalid session ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize views
        initViews()
        
        // Check permissions
        if (!hasPermissions()) {
            requestPermissions()
        } else {
            initializeCall()
        }
    }

    private fun initViews() {
        joinButton = findViewById(R.id.joinButton)
        endCallButton = findViewById(R.id.endCallButton)
        toggleMicButton = findViewById(R.id.toggleMicButton)
        toggleCameraButton = findViewById(R.id.toggleCameraButton)
        toggleScreenShareButton = findViewById(R.id.toggleScreenShareButton)
        toggleSpeakerButton = findViewById(R.id.toggleSpeakerButton)
        participantNameText = findViewById(R.id.participantNameText)
        callDurationText = findViewById(R.id.callDurationText)
        callStatusText = findViewById(R.id.callStatusText)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        statusText = findViewById(R.id.statusText)
        localVideoView = findViewById(R.id.localVideoView)
        remoteVideoView = findViewById(R.id.remoteVideoView)
        
        // Initially hide video controls
        hideVideoControls()
        
        // Set participant name
        participantNameText.text = participantName
        
        // Set call status
        callStatusText.text = "Connecting..."
        
        // Set button listeners
        joinButton.setOnClickListener { startVideoCall() }
        endCallButton.setOnClickListener { endVideoCall() }
        toggleMicButton.setOnClickListener { toggleMic() }
        toggleCameraButton.setOnClickListener { toggleCamera() }
        toggleScreenShareButton.setOnClickListener { toggleScreenShare() }
        toggleSpeakerButton.setOnClickListener { toggleSpeaker() }
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeCall()
            } else {
                Toast.makeText(
                    this,
                    "Camera and microphone permissions are required for video calls",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun initializeCall() {
        // In a real implementation, this would:
        // 1. Initialize WebRTC
        // 2. Set up the signaling connection
        // 3. Create an offer and handle ICE candidates
        // 4. Set up the peer connection
        
        // For this demo, we'll just simulate a connection after a delay
        callStatusText.text = "Connecting..."
        
        // Simulate connection delay
        localVideoView.postDelayed({
            callStatusText.text = "Connected"
            // Start call duration timer
            startCallDurationTimer()
        }, 2000)
    }

    private fun startCallDurationTimer() {
        var seconds = 0
        Thread {
            try {
                while (!isFinishing) {
                    val minutes = seconds / 60
                    val secs = seconds % 60
                    val time = String.format("%02d:%02d", minutes, secs)
                    
                    runOnUiThread {
                        callDurationText.text = time
                    }
                    
                    Thread.sleep(1000)
                    seconds++
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun endVideoCall() {
        // In a real app, this would disconnect from video call service
        // For demo purposes, we'll just finish the activity
        Toast.makeText(this, "Video session ended", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun toggleMic() {
        isAudioEnabled = !isAudioEnabled
        
        // In a real implementation, this would enable/disable the local audio track
        
        // Update button UI
        toggleMicButton.setImageResource(
            if (isAudioEnabled) R.drawable.ic_mic_on
            else R.drawable.ic_mic_off
        )
        
        Toast.makeText(
            this,
            if (isAudioEnabled) "Microphone enabled" else "Microphone disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleCamera() {
        isVideoEnabled = !isVideoEnabled
        
        // In a real implementation, this would enable/disable the local video track
        
        // Update button UI
        toggleCameraButton.setImageResource(
            if (isVideoEnabled) R.drawable.ic_video_on
            else R.drawable.ic_video_off
        )
        
        // Update local video view
        localVideoView.visibility = if (isVideoEnabled) View.VISIBLE else View.INVISIBLE
        
        Toast.makeText(
            this,
            if (isVideoEnabled) "Camera enabled" else "Camera disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleScreenShare() {
        // In a real app, this would toggle screen sharing in the video call SDK
        toggleScreenShareButton.isSelected = !toggleScreenShareButton.isSelected
        Toast.makeText(this, 
            if (toggleScreenShareButton.isSelected) "Screen sharing started" else "Screen sharing stopped", 
            Toast.LENGTH_SHORT).show()
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        
        // In a real implementation, this would switch between earpiece and speaker
        
        // Update button UI
        toggleSpeakerButton.setImageResource(
            if (isSpeakerOn) R.drawable.ic_speaker_on
            else R.drawable.ic_speaker_off
        )
        
        Toast.makeText(
            this,
            if (isSpeakerOn) "Speaker enabled" else "Speaker disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showVideoControls() {
        endCallButton.visibility = View.VISIBLE
        toggleMicButton.visibility = View.VISIBLE
        toggleCameraButton.visibility = View.VISIBLE
        toggleScreenShareButton.visibility = View.VISIBLE
        toggleSpeakerButton.visibility = View.VISIBLE
    }

    private fun hideVideoControls() {
        endCallButton.visibility = View.GONE
        toggleMicButton.visibility = View.GONE
        toggleCameraButton.visibility = View.GONE
        toggleScreenShareButton.visibility = View.GONE
        toggleSpeakerButton.visibility = View.GONE
    }

    private fun startVideoCall() {
        // Show loading state
        loadingProgressBar.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
        statusText.text = "Starting video call..."
        
        // In a real app, this would connect to a video call service using WebRTC
        joinButton.isEnabled = false
        
        // Simulate connection delay
        joinButton.postDelayed({
            // Hide loading state
            loadingProgressBar.visibility = View.GONE
            statusText.visibility = View.GONE
            joinButton.visibility = View.GONE
            
            // Show video controls
            showVideoControls()
            
            // Update session state
            sessionStarted = true
            
            // This is where you would initialize WebRTC in a real app
            // For example:
            // 1. Create PeerConnectionFactory
            // 2. Setup local video source and track
            // 3. Create peer connection and add local streams
            // 4. Connect to signaling server
            // 5. Create and send offer/answer
            
            Toast.makeText(this, "Connected to video session", Toast.LENGTH_SHORT).show()
        }, 2000)
    }

    override fun onBackPressed() {
        if (sessionStarted) {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("End Call")
                .setMessage("Are you sure you want to end this call?")
                .setPositiveButton("End Call") { _, _ -> endVideoCall() }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
    
    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_PARTICIPANT_NAME = "extra_participant_name"
        
        fun newIntent(context: Context, sessionId: String, participantName: String): Intent {
            return Intent(context, VideoCallActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_PARTICIPANT_NAME, participantName)
            }
        }
    }
} 