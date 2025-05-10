package com.mobile.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Custom location overlay that ensures the GPS accuracy circle is properly centered on the user's location icon.
 * This fixes the issue where the GPS circle doesn't match the icon where the user is.
 */
class CenteredLocationOverlay(
    locationProvider: GpsMyLocationProvider,
    mapView: MapView
) : MyLocationNewOverlay(locationProvider, mapView) {

    // Paint for drawing the accuracy circle
    private val accuracyPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0x186BAFD4  // Light blue with transparency
    }

    // Fixed radius for the accuracy circle (in meters)
    // This can be adjusted based on the desired visual appearance
    private val accuracyRadiusMeters = 30f

    /**
     * Override the draw method to ensure the accuracy circle is drawn centered on the location icon
     */
    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        // Get the current location
        val myLocation = myLocation ?: return

        // Don't call super.draw() as we're completely replacing the drawing logic
        // to ensure proper centering

        // Convert GeoPoint to screen coordinates
        val screenCoords = Point()
        mapView.projection.toPixels(GeoPoint(myLocation), screenCoords)

        // Draw accuracy circle with fixed radius
        // Convert meters to pixels based on current zoom level
        val radiusInPixels = accuracyRadiusMeters / mapView.projection.metersToEquatorPixels(1f)

        // Draw the accuracy circle centered on the location icon
        canvas.drawCircle(
            screenCoords.x.toFloat(),
            screenCoords.y.toFloat(),
            radiusInPixels,
            accuracyPaint
        )

        // Draw the person icon centered at the exact same position as the circle
        val personBitmap = mPersonBitmap ?: return
        val halfWidth = personBitmap.width / 2
        val halfHeight = personBitmap.height / 2

        // Draw the icon centered on the same point as the circle
        canvas.drawBitmap(
            personBitmap,
            screenCoords.x.toFloat() - halfWidth,
            screenCoords.y.toFloat() - halfHeight,
            null
        )
    }

    /**
     * Override to ensure both icons are properly centered
     */
    override fun setPersonIcon(icon: Bitmap) {
        super.setPersonIcon(icon)
        // Also set the direction icon to be the same to ensure consistency
        super.setDirectionIcon(icon)
    }
}
