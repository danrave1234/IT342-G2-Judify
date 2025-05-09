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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mobile.ui.base.BaseFragment
import com.mobile.R
import com.mobile.ui.dashboard.TutorDashboardActivity
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.mobile.utils.UiUtils

// Extension function to convert Drawable to Bitmap
private fun Drawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        intrinsicWidth,
        intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

class MapFragment : BaseFragment() {
    private val TAG = "MapFragment"
    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var pinLocationButton: FloatingActionButton
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var tutorMarkersOverlay: FolderOverlay
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>


    // Flag to track if user is a tutor
    private var isTutor = false

    // Current tutor markers
    private val tutorMarkers = mutableListOf<Marker>()

    // Default location (Iloilo)
    private val defaultLocation = GeoPoint(10.7202, 122.5621)

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_map
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Configure OSMDroid
        val ctx = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = requireActivity().packageName

        mapView = view.findViewById(R.id.mapView)
        pinLocationButton = view.findViewById(R.id.pinLocationButton)

        // Set up bottom sheet
        val bottomSheet = view.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Set up bottom sheet callback
        setupBottomSheetCallback()

        // Disable auto-focus for search edit text
        view.findViewById<EditText>(R.id.searchEditText)?.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener {
                isFocusableInTouchMode = true
                isFocusable = true
                requestFocus()
            }
        }

        // Check if user is a tutor (based on parent activity)
        isTutor = activity is TutorDashboardActivity

        // Show pin location button if user is a tutor
        if (isTutor) {
            pinLocationButton.visibility = View.VISIBLE
        }

        // Configure map appearance
        configureMapAppearance()

        // Request permissions if needed
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        // Set up location tracking
        setupLocationTracking()

        // Initialize map control buttons
        initMapControls(view)

        // Setup the tutor markers overlay
        setupTutorMarkersOverlay()

        // Add real tutors or sample tutors if in demo mode
        loadTutorLocations()

        // Set up filter button listener
        view.findViewById<View>(R.id.filterButton).setOnClickListener {
            showFilterDialog()
        }

        // Set up search functionality
        setupSearch(view.findViewById(R.id.searchEditText))

        // Set up location buttons in bottom sheet
        setupBottomSheetButtons(view)
    }

    private fun setupBottomSheetCallback() {
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Animate FABs when bottom sheet is expanded
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    pinLocationButton.animate().translationY(-bottomSheetBehavior.peekHeight.toFloat()).start()
                    view?.findViewById<FloatingActionButton>(R.id.myLocationFab)?.animate()?.translationY(-bottomSheetBehavior.peekHeight.toFloat())?.start()
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    pinLocationButton.animate().translationY(0f).start()
                    view?.findViewById<FloatingActionButton>(R.id.myLocationFab)?.animate()?.translationY(0f)?.start()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Optional slide animation
            }
        })
    }

    private fun setupBottomSheetButtons(view: View) {
        // Set up the directions button
        view.findViewById<MaterialButton>(R.id.directionsButton)?.setOnClickListener {
            val locationTitle = view.findViewById<TextView>(R.id.locationTitleTextView).text.toString()
            UiUtils.showSnackbar(requireView(), "Navigating to $locationTitle")
            // Here you would launch external navigation
        }

        // Set up the save location button
        view.findViewById<MaterialButton>(R.id.saveLocationButton)?.setOnClickListener {
            val locationTitle = view.findViewById<TextView>(R.id.locationTitleTextView).text.toString()
            UiUtils.showSuccessSnackbar(requireView(), "Saved $locationTitle")
            // Toggle button state
            (it as MaterialButton).apply {
                if (text == "Save") {
                    text = "Saved"
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bookmark_filled)
                } else {
                    text = "Save"
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bookmark)
                }
            }
        }
    }

    private fun setupSearch(searchEditText: EditText) {
        searchEditText.setOnEditorActionListener { textView, actionId, event ->
            val searchText = textView.text.toString()
            if (searchText.isNotEmpty()) {
                // Perform search by filtering markers
                searchTutors(searchText)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun searchTutors(query: String) {
        // Show loading indicator
        view?.findViewById<View>(R.id.progressBar)?.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Simulate search delay
            delay(500)

            // Filter markers based on query
            val matchingMarkers = tutorMarkers.filter { marker ->
                marker.title?.contains(query, ignoreCase = true) == true || 
                marker.snippet?.contains(query, ignoreCase = true) == true
            }

            withContext(Dispatchers.Main) {
                // Hide loading indicator
                view?.findViewById<View>(R.id.progressBar)?.visibility = View.GONE

                if (matchingMarkers.isEmpty()) {
                    UiUtils.showInfoSnackbar(requireView(), "No tutors found matching '$query'")
                } else {
                    // Center on first match
                    val firstMatch = matchingMarkers.first()
                    mapView.controller.animateTo(firstMatch.position)

                    // Show bottom sheet with tutor info
                    showTutorInfo(firstMatch.title, firstMatch.snippet, firstMatch.position)

                    // If there are multiple matches, show count
                    if (matchingMarkers.size > 1) {
                        UiUtils.showSuccessSnackbar(requireView(), "Found ${matchingMarkers.size} tutors matching '$query'")
                    }
                }
            }
        }
    }

    private fun showFilterDialog() {
        val subjectItems = arrayOf("Mathematics", "Physics", "Chemistry", "Biology", "Computer Science", "English", "History")
        val checkedItems = BooleanArray(subjectItems.size) { false }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter Tutors by Subject")
            .setMultiChoiceItems(subjectItems, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Apply") { _, _ ->
                // Get selected subjects
                val selectedSubjects = subjectItems.filterIndexed { index, _ -> checkedItems[index] }

                if (selectedSubjects.isEmpty()) {
                    // Show all tutors if no filter is selected
                    tutorMarkers.forEach { it.isEnabled = true }
                    mapView.invalidate()
                    UiUtils.showInfoSnackbar(requireView(), "Showing all tutors")
                } else {
                    // Filter tutors by subject
                    tutorMarkers.forEach { marker ->
                        marker.isEnabled = selectedSubjects.any { subject ->
                            marker.snippet?.contains(subject, ignoreCase = true) == true
                        }
                    }
                    mapView.invalidate()
                    UiUtils.showInfoSnackbar(requireView(), "Filtered by ${selectedSubjects.joinToString()}")
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear All") { _, _ ->
                // Show all tutors
                tutorMarkers.forEach { it.isEnabled = true }
                mapView.invalidate()
                UiUtils.showInfoSnackbar(requireView(), "Showing all tutors")
            }
            .show()
    }

    private fun configureMapAppearance() {
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
                // Simple scroll handler
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
    }

    private fun setupLocationTracking() {
        // Add location overlay using our custom CenteredLocationOverlay
        locationOverlay = CenteredLocationOverlay(GpsMyLocationProvider(requireContext()), mapView)
        locationOverlay.enableMyLocation()

        // Set up a runnable to execute when location is first available
        locationOverlay.runOnFirstFix {
            activity?.runOnUiThread {
                Log.d(TAG, "Got first location fix")
                // Center map on user's location
                val myLocation = locationOverlay.myLocation
                if (myLocation != null) {
                    mapView.controller.animateTo(myLocation)
                    // Set map zoom to a comfortable level for street navigation
                    mapView.controller.setZoom(16.0)
                } else {
                    // If location isn't available yet, use a default location
                    mapView.controller.animateTo(defaultLocation)
                    mapView.controller.setZoom(14.0)
                }
            }
        }

        // Don't enable following mode by default to allow user to browse the map freely

        // Use the arrow icon for both person and direction to prevent flickering
        val arrowIcon = ContextCompat.getDrawable(requireContext(), 
            if (isNightModeActive()) R.drawable.ic_direction_arrow_dark 
            else R.drawable.ic_direction_arrow)?.toBitmap()

        // Set both personIcon and directionIcon to be the arrow
        arrowIcon?.let {
            locationOverlay.setPersonIcon(it)
            locationOverlay.setDirectionIcon(it)
        }

        mapView.overlays.add(locationOverlay)

        // Set default zoom
        val mapController = mapView.controller
        mapController.setZoom(15.0)
    }

    private fun loadTutorLocations() {
        // Clear previous tutors
        tutorMarkers.clear()
        if (::tutorMarkersOverlay.isInitialized) {
            tutorMarkersOverlay.items.clear()
        }

        // Show loading state
        view?.findViewById<View>(R.id.progressBar)?.visibility = View.VISIBLE

        // Launch coroutine to load tutors
        lifecycleScope.launch {
            try {
                // Try to load real tutors from API
                val result = NetworkUtils.getAllTutorProfiles()

                if (result.isSuccess) {
                    val tutors = result.getOrNull() ?: emptyList()

                    if (tutors.isNotEmpty()) {
                        // Create markers for each tutor with location
                        tutors.forEach { tutor ->
                            if (tutor.latitude != null && tutor.longitude != null) {
                                addTutorMarker(
                                    tutor.latitude,
                                    tutor.longitude,
                                    tutor.name,
                                    tutor.subjects.joinToString(", ")
                                )
                            }
                        }
                    } else {
                        // If no tutors found, add sample tutors
                        addSampleTutors()
                    }
                } else {
                    // If API call fails, add sample tutors
                    Log.e(TAG, "Failed to load tutors: ${result.exceptionOrNull()?.message}")
                    addSampleTutors()
                }
            } catch (e: Exception) {
                // If there's an error, add sample tutors
                Log.e(TAG, "Error loading tutors: ${e.message}", e)
                addSampleTutors()
            } finally {
                // Hide loading indicator
                withContext(Dispatchers.Main) {
                    view?.findViewById<View>(R.id.progressBar)?.visibility = View.GONE
                }
            }
        }
    }

    private fun addSampleTutors() {
        // Sample tutor locations in Iloilo
        val tutors = listOf(
            TutorLocation("Jacob Stevens", "Mathematics", 10.6480, 122.9673),
            TutorLocation("Claire Watson", "English", 10.6520, 122.9630),
            TutorLocation("Priscilla Chen", "Computer Science", 10.6450, 122.9690),
            TutorLocation("Wade Wilson", "Engineering", 10.6510, 122.9710),
            TutorLocation("Maria Santos", "History", 10.6530, 122.9650),
            TutorLocation("James Lee", "Chemistry", 10.6515, 122.9695),
            TutorLocation("Sofia Rodriguez", "Biology", 10.6490, 122.9645),
            TutorLocation("David Park", "Physics", 10.6525, 122.9680)
        )

        tutors.forEach { tutor ->
            addTutorMarker(tutor.latitude, tutor.longitude, tutor.name, tutor.expertise)
        }
    }

    private fun addTutorMarker(latitude: Double, longitude: Double, name: String, expertise: String) {
        val position = GeoPoint(latitude, longitude)
        val marker = Marker(mapView)

        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = name
        marker.snippet = expertise

        // Choose the appropriate icon based on the current theme
        val iconResId = if (isNightModeActive()) {
            R.drawable.ic_person_dark  // Use the dark mode icon (blue tint)
        } else {
            R.drawable.ic_person  // Use the regular icon
        }

        marker.icon = ContextCompat.getDrawable(requireContext(), iconResId)

        // Set up marker click listener to show bottom sheet
        marker.setOnMarkerClickListener { clickedMarker, _ ->
            showTutorInfo(clickedMarker.title, clickedMarker.snippet, clickedMarker.position)
            true
        }

        // Add to marker list and to folder overlay
        tutorMarkers.add(marker)
        tutorMarkersOverlay.add(marker)
        mapView.invalidate()
    }

    private fun showTutorInfo(name: String?, expertise: String?, position: GeoPoint?) {
        if (name == null || expertise == null || position == null) return

        // Set bottom sheet content
        view?.findViewById<TextView>(R.id.locationTitleTextView)?.text = name
        view?.findViewById<TextView>(R.id.locationAddressTextView)?.text = "Tutor for: $expertise"
        view?.findViewById<TextView>(R.id.locationDescriptionTextView)?.text = 
            "An experienced tutor specializing in $expertise. " +
            "Available for in-person and online sessions. Tap the buttons below to get directions or save this tutor for later."

        // Customize the icons based on subject
        if (expertise.contains("Math", ignoreCase = true)) {
            view?.findViewById<TextView>(R.id.wifiStatusTextView)?.text = "Online"
            view?.findViewById<TextView>(R.id.noiseTextView)?.text = "In-Person"
            view?.findViewById<TextView>(R.id.outletsTextView)?.text = "Math Specialist"
        } else if (expertise.contains("English", ignoreCase = true) || expertise.contains("History", ignoreCase = true)) {
            view?.findViewById<TextView>(R.id.wifiStatusTextView)?.text = "Online"
            view?.findViewById<TextView>(R.id.noiseTextView)?.text = "Group Sessions"
            view?.findViewById<TextView>(R.id.outletsTextView)?.text = "Essay Coaching"
        } else if (expertise.contains("Computer", ignoreCase = true)) {
            view?.findViewById<TextView>(R.id.wifiStatusTextView)?.text = "Remote"
            view?.findViewById<TextView>(R.id.noiseTextView)?.text = "In-Person"
            view?.findViewById<TextView>(R.id.outletsTextView)?.text = "Coding Help"
        } else {
            view?.findViewById<TextView>(R.id.wifiStatusTextView)?.text = "Online"
            view?.findViewById<TextView>(R.id.noiseTextView)?.text = "In-Person"
            view?.findViewById<TextView>(R.id.outletsTextView)?.text = "1-on-1 Help"
        }

        // Show bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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
        val resetOrientationButton = view.findViewById<FloatingActionButton>(R.id.resetOrientationButton)
        val myLocationFab = view.findViewById<FloatingActionButton>(R.id.myLocationFab)

        // My Location FAB - centers map on user location
        myLocationFab.setOnClickListener {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                // Animate to location with smooth zoom
                mapView.controller.animateTo(myLocation)
                // Don't enable follow mode to allow the user to continue browsing
            } else {
                UiUtils.showWarningSnackbar(requireView(), "Location not available yet")
            }
        }

        // Reset Orientation button - resets the map orientation to north
        resetOrientationButton.setOnClickListener {
            // Reset map rotation to 0 (north up) with animation
            val currentRotation = mapView.mapOrientation
            // Only animate if the rotation is significant
            if (abs(currentRotation) > 5) {
                val animator = android.animation.ValueAnimator.ofFloat(currentRotation, 0f)
                animator.duration = 300 // Duration in milliseconds
                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    mapView.mapOrientation = value
                }
                animator.start()

                UiUtils.showSnackbar(requireView(), "Map orientation reset")
            } else {
                mapView.mapOrientation = 0f
            }
        }

        // Pin Location button - pins the tutor's location on the map
        pinLocationButton.setOnClickListener {
            // Get the current location
            val currentLocation = locationOverlay.myLocation
            if (currentLocation != null) {
                // Pin the location
                pinTutorLocation(currentLocation.latitude, currentLocation.longitude)
            } else {
                UiUtils.showErrorSnackbar(requireView(), "Location not available. Please try again.")
            }
        }
    }

    private fun pinTutorLocation(latitude: Double, longitude: Double) {
        // Get user ID from preferences
        val userId = PreferenceUtils.getUserId(requireContext())
        if (userId == null) {
            UiUtils.showErrorSnackbar(requireView(), "User ID not found")
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
                // First, get the tutor profile for this user
                val tutorProfileResult = NetworkUtils.findTutorByUserId(userId)

                if (tutorProfileResult.isSuccess) {
                    val tutorProfile = tutorProfileResult.getOrNull()

                    if (tutorProfile != null) {
                        // Update the tutor's location
                        val updateResult = NetworkUtils.updateTutorLocation(tutorProfile.id, latitude, longitude)

                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()

                            updateResult.fold(
                                onSuccess = { _ ->
                                    UiUtils.showSuccessSnackbar(requireView(), "Location updated successfully")

                                    // Update or add a marker at the pinned location
                                    val existingMarker = tutorMarkers.find { it.title == tutorProfile.name }
                                    if (existingMarker != null) {
                                        // Update existing marker
                                        existingMarker.position = GeoPoint(latitude, longitude)
                                        mapView.invalidate()
                                    } else {
                                        // Add a new marker
                                        addTutorMarker(
                                            latitude, 
                                            longitude, 
                                            tutorProfile.name, 
                                            tutorProfile.subjects.joinToString(", ")
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    UiUtils.showErrorSnackbar(requireView(), "Failed to update location: ${error.message}")
                                }
                            )
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            UiUtils.showErrorSnackbar(requireView(), "Tutor profile not found")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        UiUtils.showErrorSnackbar(requireView(), "Failed to find tutor profile")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    UiUtils.showErrorSnackbar(requireView(), "Error: ${e.message}")
                }
            }
        }
    }

    /**
     * Checks if the app is currently in night mode
     */
    private fun isNightModeActive(): Boolean {
        return (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
               android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupTutorMarkersOverlay() {
        // Initialize a folder overlay to contain all tutor markers
        tutorMarkersOverlay = FolderOverlay()
        mapView.overlays.add(tutorMarkersOverlay)
    }
}

// Data class to hold tutor location information
data class TutorLocation(
    val name: String,
    val expertise: String,
    val latitude: Double,
    val longitude: Double
) 
