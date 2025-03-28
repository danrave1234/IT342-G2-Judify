package com.mobile.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mobile.ui.base.BaseFragment
import com.mobile.R
import com.mobile.ui.dashboard.TutorDashboardActivity
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : BaseFragment() {
    private val TAG = "MapFragment"
    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var pinLocationButton: FloatingActionButton
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    // Flag to track auto-follow mode
    private var isAutoFollowEnabled = false

    // Flag to track if user is a tutor
    private var isTutor = false

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_map
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Configure OSMDroid
        val ctx = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        mapView = view.findViewById(R.id.mapView)
        pinLocationButton = view.findViewById(R.id.pinLocationButton)

        // Check if user is a tutor (based on parent activity)
        isTutor = activity is TutorDashboardActivity

        // Show pin location button if user is a tutor
        if (isTutor) {
            pinLocationButton.visibility = View.VISIBLE
        }

        // Set tile source based on theme (light/dark mode)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        // Apply dark mode styling if night mode is active
        if (isNightModeActive()) {
            // Apply a color filter to darken the map
            mapView.overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(
                ColorMatrix().apply {
                    setSaturation(0.8f)  // Slightly reduce saturation
                    postConcat(ColorMatrix().apply {
                        set(floatArrayOf(
                            0.8f, 0f, 0f, 0f, -50f,  // Red component
                            0f, 0.8f, 0f, 0f, -50f,  // Green component
                            0f, 0f, 0.8f, 0f, -50f,  // Blue component
                            0f, 0f, 0f, 1f, 0f       // Alpha component
                        ))
                    })
                }
            ))
        }

        // Enhanced map navigation configuration
        // These settings improve the user experience when swiping, zooming in and out
        mapView.setMultiTouchControls(true)

        // Set minimum and maximum zoom levels
        mapView.minZoomLevel = 4.0
        mapView.maxZoomLevel = 19.0

        // Enable built-in zoom controls
        mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        mapView.zoomController.setZoomInEnabled(true)
        mapView.zoomController.setZoomOutEnabled(true)

        // Configure gesture sensitivity - increase to improve responsiveness
        mapView.isTilesScaledToDpi = true

        // Enable hardware acceleration for smoother rendering
        mapView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Improve scrolling/panning experience
        mapView.isHorizontalMapRepetitionEnabled = true
        mapView.isVerticalMapRepetitionEnabled = true

        // Optimize map for smoother scrolling
        mapView.isFlingEnabled = true  // Ensure fling is enabled
        mapView.setMultiTouchControls(true)  // Ensure multi-touch is enabled

        // Configure fling gesture (swiping) with better defaults
        // Using zoomController instead of deprecated setBuiltInZoomControls

        // Set scroll limits to prevent getting lost
        val worldMapBoundingBox = org.osmdroid.util.BoundingBox(
            85.0, 180.0, -85.0, -180.0
        )
        mapView.setScrollableAreaLimitDouble(worldMapBoundingBox)

        // Add rotation gesture support for better map interaction
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)

        // Add map listener for smoother interactions
        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                // Actively enhance scroll behavior by ensuring smooth rendering during scrolling
                mapView.postInvalidate()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                // Ensure smooth zoom behavior by forcing immediate redraw
                mapView.postInvalidate()
                return false
            }
        })

        // Add custom touch handler to improve responsiveness
        mapView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Improve touch response by prioritizing this touch event
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP -> {
                    // Reset when touch is released
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            // Allow the map to handle the event normally
            false
        }

        // Request permissions if needed
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        // Add location overlay
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        locationOverlay.enableMyLocation()

        // Set up a runnable to execute when location is first available
        locationOverlay.runOnFirstFix {
            activity?.runOnUiThread {
                Log.d(TAG, "Got first location fix")
                // Center map on user's location
                val myLocation = locationOverlay.myLocation
                if (myLocation != null) {
                    mapView.controller.animateTo(myLocation)
                }
            }
        }

        // Enable following mode by default
        locationOverlay.enableFollowLocation()
        isAutoFollowEnabled = true

        mapView.overlays.add(locationOverlay)

        // Set default zoom
        val mapController = mapView.controller
        mapController.setZoom(15.0)

        // Initialize map control buttons
        initMapControls(view)

        // Add sample tutors
        addSampleTutors()
    }

    /**
     * Checks if the app is currently in night mode
     */
    private fun isNightModeActive(): Boolean {
        return (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
               android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun addSampleTutors() {
        // Sample tutor locations in Iloilo
        val tutors = listOf(
            TutorLocation("Jacob Stevens", "Mathematics", 10.6480, 122.9673),
            TutorLocation("Claire Watson", "English", 10.6520, 122.9630),
            TutorLocation("Priscilla Chen", "Computer Science", 10.6450, 122.9690),
            TutorLocation("Wade Wilson", "Engineering", 10.6510, 122.9710)
        )

        // Choose the appropriate icon based on the current theme
        val iconResId = if (isNightModeActive()) {
            R.drawable.ic_person_dark  // Use the dark mode icon (blue tint)
        } else {
            R.drawable.ic_person  // Use the regular icon
        }

        tutors.forEach { tutor ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(tutor.latitude, tutor.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = tutor.name
            marker.snippet = tutor.expertise
            marker.icon = ContextCompat.getDrawable(requireContext(), iconResId)
            mapView.overlays.add(marker)
        }

        mapView.invalidate() // refresh the map
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        mapView.onResume()
        // Refresh location when returning to the map
        locationOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        mapView.onPause()
        // Disable location updates when leaving the map
        locationOverlay.disableMyLocation()
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = ArrayList<String>()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun initMapControls(view: View) {
        // Find the buttons
        val autoFollowButton = view.findViewById<View>(R.id.autoFollowButton)
        val resetOrientationButton = view.findViewById<View>(R.id.resetOrientationButton)

        // Auto-Follow toggle button - enables/disables auto-follow mode
        autoFollowButton.setOnClickListener {
            if (isAutoFollowEnabled) {
                // Disable auto-follow
                locationOverlay.disableFollowLocation()
                isAutoFollowEnabled = false
                Log.d(TAG, "Auto-follow disabled")
            } else {
                // Enable auto-follow
                locationOverlay.enableFollowLocation()
                isAutoFollowEnabled = true
                Log.d(TAG, "Auto-follow enabled")
            }
        }

        // Reset Orientation button - resets the map orientation to north
        resetOrientationButton.setOnClickListener {
            // Reset map rotation to 0 (north up)
            mapView.mapOrientation = 0f
            Log.d(TAG, "Map orientation reset")
        }

        // Pin Location button - pins the tutor's location on the map
        pinLocationButton.setOnClickListener {
            // Get the current location
            val currentLocation = locationOverlay.myLocation
            if (currentLocation != null) {
                // Pin the location
                pinTutorLocation(currentLocation.latitude, currentLocation.longitude)
            } else {
                Toast.makeText(requireContext(), "Location not available. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pinTutorLocation(latitude: Double, longitude: Double) {
        // Get tutor email from preferences
        val tutorEmail = PreferenceUtils.getUserEmail(requireContext())
        if (tutorEmail == null) {
            Toast.makeText(requireContext(), "User email not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show a loading indicator
        val loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Updating Location")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Launch a coroutine to update the tutor's location
        lifecycleScope.launch {
            try {
                // For simplicity, we're using a hardcoded tutor profile ID (1L)
                // In a real implementation, you would get the tutor's profile ID
                val result = com.mobile.utils.NetworkUtils.updateTutorLocation(1L, latitude, longitude)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()

                    result.fold(
                        onSuccess = { _ ->
                            Toast.makeText(
                                requireContext(),
                                "Location updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Add a marker at the pinned location
                            addTutorMarker(latitude, longitude, "Your Location", "This is where learners will find you")
                        },
                        onFailure = { error ->
                            Toast.makeText(
                                requireContext(),
                                "Failed to update location: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun addTutorMarker(latitude: Double, longitude: Double, title: String, snippet: String) {
        val marker = Marker(mapView)
        marker.position = GeoPoint(latitude, longitude)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        marker.snippet = snippet
        marker.icon = ContextCompat.getDrawable(requireContext(), 
            if (isNightModeActive()) R.drawable.ic_person_dark else R.drawable.ic_person)
        mapView.overlays.add(marker)
        mapView.invalidate() // refresh the map
    }
}

// Data class to hold tutor location information
data class TutorLocation(
    val name: String,
    val expertise: String,
    val latitude: Double,
    val longitude: Double
) 
