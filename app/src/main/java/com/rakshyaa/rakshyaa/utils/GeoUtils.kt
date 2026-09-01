package com.rakshyaa.rakshyaa.utils

import android.location.Location

/**
 * Utility class for geospatial calculations
 * Includes Haversine formula for distance calculation and route proximity checks
 */
object GeoUtils {

    private const val EARTH_RADIUS_M = 6371000.0 // Earth's radius in meters

    /**
     * Calculates the distance between two points using the Haversine formula
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance between the two points in meters
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return EARTH_RADIUS_M * c
    }

    /**
     * Calculates the distance between two Location objects
     *
     * @param location1 First location
     * @param location2 Second location
     * @return Distance between the two locations in meters
     */
    fun distanceBetween(location1: Location, location2: Location): Double {
        return haversineDistance(
            location1.latitude,
            location1.longitude,
            location2.latitude,
            location2.longitude
        )
    }

    /**
     * Calculates the distance from a point to the nearest point on a route
     * The route is defined as a series of waypoints
     *
     * @param point The point to check distance from
     * @param routeWaypoints The waypoints defining the route
     * @return Minimum distance from the point to any point on the route in meters
     */
    fun distanceToRoute(point: Location, routeWaypoints: List<Location>): Double {
        if (routeWaypoints.isEmpty()) {
            // If no waypoints, return distance to origin or a large value
            return Double.MAX_VALUE
        }

        var minDistance = Double.MAX_VALUE

        // Check distance to each waypoint
        for (waypoint in routeWaypoints) {
            val distance = distanceBetween(point, waypoint)
            if (distance < minDistance) {
                minDistance = distance
            }
        }

        // Also check distance to line segments between waypoints for more accuracy
        // This is a simplified version - for production you might want to implement
        // proper point-to-line-segment distance calculation
        if (routeWaypoints.size >= 2) {
            for (i in 0 until routeWaypoints.size - 1) {
                val segmentStart = routeWaypoints[i]
                val segmentEnd = routeWaypoints[i + 1]
                val distanceToSegment = distanceToLineSegment(point, segmentStart, segmentEnd)
                if (distanceToSegment < minDistance) {
                    minDistance = distanceToSegment
                }
            }
        }

        return minDistance
    }

    /**
     * Calculates the distance from a point to a line segment
     *
     * @param point The point to check distance from
     * @param lineStart Start point of the line segment
     * @param lineEnd End point of the line segment
     * @return Distance from the point to the line segment in meters
     */
    private fun distanceToLineSegment(point: Location, lineStart: Location, lineEnd: Location): Double {
        // Vector from lineStart to lineEnd
        val dx = lineEnd.latitude - lineStart.latitude
        val dy = lineEnd.longitude - lineStart.longitude

        // If the line segment is actually a point, return distance to that point
        if (dx == 0.0 && dy == 0.0) {
            return distanceBetween(point, lineStart)
        }

        // Parameter t for the projection of point onto the line
        // t = [(P-A) . (B-A)] / ||B-A||^2
        val t = ((point.latitude - lineStart.latitude) * dx +
                (point.longitude - lineStart.longitude) * dy) /
                (dx * dx + dy * dy)

        // Clamp t to [0, 1] to get the closest point on the segment
        val clampedT = t.coerceIn(0.0, 1.0)

        // Find the closest point on the line segment
        val closestX = lineStart.latitude + clampedT * dx
        val closestY = lineStart.longitude + clampedT * dy

        // Return distance to the closest point
        val closestPoint = Location("")
        closestPoint.latitude = closestX
        closestPoint.longitude = closestY
        return distanceBetween(point, closestPoint)
    }

    /**
     * Calculates the bearing between two points
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Bearing from point 1 to point 2 in degrees (0-360)
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLonRad = Math.toRadians(lon2 - lon1)

        val y = Math.sin(dLonRad) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLonRad)

        val bearingRad = Math.atan2(y, x)
        var bearingDeg = Math.toDegrees(bearingRad)

        // Normalize to 0-360 degrees
        return (bearingDeg + 360) % 360
    }

    /**
     * Checks if three points make a clockwise turn
     *
     * @param p1 First point
     * @param p2 Second point
     * @param p3 Third point
     * @return True if the turn is clockwise, false if counter-clockwise
     */
    fun isClockwiseTurn(p1: Location, p2: Location, p3: Location): Boolean {
        val crossProduct = (p2.longitude - p1.longitude) * (p3.latitude - p2.latitude) -
                (p2.latitude - p1.latitude) * (p3.longitude - p2.longitude)
        return crossProduct < 0
    }
}