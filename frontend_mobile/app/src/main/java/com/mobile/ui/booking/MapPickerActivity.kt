package com.mobile.ui.booking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mobile.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import com.mobile.ui.map.CenteredLocationOverlay

/**
 * Activity for selecting a meeting location on a map.
 * Used when booking an in-person tutoring session.
 */
class MapPickerActivity : AppCompatActivity() {
    private val TAG = "MapPickerActivity"
    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var confirmButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var instructionsText: TextView
    private lateinit var selectedLocationMarker: Marker
    private lateinit var gotoMyLocationButton: FloatingActionButton

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedLocationName: String = ""
    private var hasLocationSelected = false
    private var isMarkerVisible = false

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        // Initialize UI components
        mapView = findViewById(R.id.mapPickerView)
        confirmButton = findViewById(R.id.confirmLocationButton)
        toolbar = findViewById(R.id.toolbar)
        instructionsText = findViewById(R.id.instructionsText)
        gotoMyLocationButton = findViewById(R.id.myLocationButton)

        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Select Meeting Location"

        // Configure OSMDroid
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        // Set up map
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 4.0
        mapView.maxZoomLevel = 19.0
        mapView.controller.setZoom(15.0)

        // Request permissions if needed
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        // Add location overlay using our custom CenteredLocationOverlay
        locationOverlay = CenteredLocationOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()

        // Set up a runnable to execute when location is first available
        locationOverlay.runOnFirstFix {
            runOnUiThread {
                Log.d(TAG, "Got first location fix")
                // Center map on user's location
                val myLocation = locationOverlay.myLocation
                if (myLocation != null) {
                    mapView.controller.animateTo(myLocation)
                }
            }
        }

        mapView.overlays.add(locationOverlay)

        // Create a marker for selected location
        selectedLocationMarker = Marker(mapView)
        selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        selectedLocationMarker.icon = ContextCompat.getDrawable(this, R.drawable.ic_location_pin)
        selectedLocationMarker.title = "Meeting Location"
        isMarkerVisible = false // Initially invisible
        mapView.overlays.add(selectedLocationMarker)

        // Set up map click listener to place a marker using MapEventsOverlay
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                // Update selected location
                selectedLatitude = p.latitude
                selectedLongitude = p.longitude
                selectedLocationName = "Location at ${String.format("%.5f", selectedLatitude)}, ${String.format("%.5f", selectedLongitude)}"

                // Update marker
                selectedLocationMarker.position = p
                isMarkerVisible = true // Track marker visibility with our custom field
                selectedLocationMarker.title = "Meeting Location"
                selectedLocationMarker.snippet = selectedLocationName
                mapView.invalidate()

                // Enable confirm button
                confirmButton.isEnabled = true
                hasLocationSelected = true

                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        })
        mapView.overlays.add(mapEventsOverlay)

        // Set up confirm button
        confirmButton.isEnabled = false // Initially disabled until location is selected
        confirmButton.setOnClickListener {
            if (hasLocationSelected) {
                // Return the selected location
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_LATITUDE, selectedLatitude)
                    putExtra(EXTRA_LONGITUDE, selectedLongitude)
                    putExtra(EXTRA_LOCATION_NAME, selectedLocationName)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please select a meeting location", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up my location button
        gotoMyLocationButton.setOnClickListener {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                mapView.controller.animateTo(myLocation)
            } else {
                Toast.makeText(this, "Location not available yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = ArrayList<String>()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        locationOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationOverlay.disableMyLocation()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val REQUEST_CODE = 101

        fun newIntent(context: Context): Intent {
            return Intent(context, MapPickerActivity::class.java)
        }
    }
} 
