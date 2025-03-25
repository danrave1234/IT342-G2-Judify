package com.mobile.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mobile.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {
    private val TAG = "MapFragment"
    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        
        // Configure OSMDroid
        val ctx = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        
        mapView = view.findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
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
        mapView.overlays.add(locationOverlay)
        
        // Set default zoom and center
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        
        // Default to a location until we get GPS fix
        val startPoint = GeoPoint(10.647600, 122.964800) // Iloilo City, Philippines
        mapController.setCenter(startPoint)
        
        // Add sample tutors
        addSampleTutors()
        
        return view
    }

    private fun addSampleTutors() {
        // Sample tutor locations in Iloilo
        val tutors = listOf(
            TutorLocation("Jacob Stevens", "Mathematics", 10.6480, 122.9673),
            TutorLocation("Claire Watson", "English", 10.6520, 122.9630),
            TutorLocation("Priscilla Chen", "Computer Science", 10.6450, 122.9690),
            TutorLocation("Wade Wilson", "Engineering", 10.6510, 122.9710)
        )
        
        tutors.forEach { tutor ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(tutor.latitude, tutor.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = tutor.name
            marker.snippet = tutor.expertise
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_person)
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
}

// Data class to hold tutor location information
data class TutorLocation(
    val name: String,
    val expertise: String,
    val latitude: Double,
    val longitude: Double
) 