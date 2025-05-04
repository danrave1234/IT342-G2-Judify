import { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  FaStar, 
  FaMapMarkerAlt, 
  FaVideo, 
  FaChalkboardTeacher,
  FaCalendarAlt,
  FaClock,
  FaDollarSign,
  FaChevronLeft,
  FaUser,
  FaMapPin,
  FaSearch,
  FaDirections,
  FaLocationArrow,
  FaPlus,
  FaMinus
} from 'react-icons/fa';
import { toast } from 'react-toastify';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import axios from 'axios';
import { useUser } from '../../context/UserContext';
import L from 'leaflet';
import UserAvatar from '../../components/common/UserAvatar';

const TutorDetails = () => {
  const { id } = useParams();
  const { user } = useUser();
  const [tutor, setTutor] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [availableTimeSlots, setAvailableTimeSlots] = useState([]);
  const [selectedTimeSlot, setSelectedTimeSlot] = useState(null);
  const [sessionType, setSessionType] = useState('online');
  const [submitting, setSubmitting] = useState(false);
  const [userLocation, setUserLocation] = useState(null);
  const [distance, setDistance] = useState(null);
  const [mapLoaded, setMapLoaded] = useState(false);
  const [map, setMap] = useState(null);
  const [showContact, setShowContact] = useState(false);
  const mapRef = useRef(null);
  const mapMarkerRef = useRef(null);
  const [mapInstance, setMapInstance] = useState(null);

  useEffect(() => {
    // Always attempt to get user's location automatically without explicit permission checks
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          // Silently handle errors - no need to show permission errors to users
          console.error('Error getting location:', error);
        },
        // Use high accuracy and shorter timeout for better UX
        { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
      );
    }

    const fetchTutorDetails = async () => {
      try {
        // Store basic tutor info in localStorage for potential use in BookSession
        const storeTutorInfo = (tutorData) => {
          try {
            const tutorInfo = {
              id: id,
              name: `${tutorData.firstName || tutorData.user?.firstName} ${tutorData.lastName || tutorData.user?.lastName}`,
              profilePicture: tutorData.profilePicture || tutorData.user?.profilePicture || 'https://via.placeholder.com/150',
              subjects: tutorData.subjects || []
            };
            localStorage.setItem('lastViewedTutor', JSON.stringify(tutorInfo));
            console.log('Stored tutor info in localStorage:', tutorInfo);
          } catch (storageError) {
            console.error('Failed to store tutor info in localStorage:', storageError);
          }
        };

        const token = localStorage.getItem('judify_token');
        const config = {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        };

        // Try to use the API methods from api.js if available
        try {
          // Import the API methods dynamically to avoid changing the component structure
          const { tutorProfileApi } = await import('../../api/api');

          // First try to fetch by profile ID
          try {
            const tutorResponse = await tutorProfileApi.getProfileById(id);
            const tutorData = tutorResponse.data;
            setTutor(tutorData);
            storeTutorInfo(tutorData);

            // Fetch reviews using the original method
            const reviewsRes = await axios.get(`/api/tutors/${id}/reviews`, config);
            setReviews(reviewsRes.data);

            setLoading(false);
            return;
          } catch (profileIdError) {
            console.log('Error fetching by profileId:', profileIdError.message);

            // If that fails, try to fetch by user ID
            try {
              const tutorResponse = await tutorProfileApi.getProfileByUserId(id);
              const tutorData = tutorResponse.data;
              setTutor(tutorData);
              storeTutorInfo(tutorData);

              // Fetch reviews using the original method
              const reviewsRes = await axios.get(`/api/tutors/${id}/reviews`, config);
              setReviews(reviewsRes.data);

              setLoading(false);
              return;
            } catch (userIdError) {
              console.log('Error fetching by userId:', userIdError.message);
              // Fall back to the original method
            }
          }
        } catch (importError) {
          console.error('Error importing API methods:', importError);
          // Fall back to the original method
        }

        // Original method as fallback
        const tutorRes = await axios.get(`/api/tutors/${id}`, config);
        setTutor(tutorRes.data);
        storeTutorInfo(tutorRes.data);

        const reviewsRes = await axios.get(`/api/tutors/${id}/reviews`, config);
        setReviews(reviewsRes.data);

        setLoading(false);
      } catch (error) {
        console.error('Failed to load tutor details:', error);
        toast.error('Failed to load tutor details');
        setLoading(false);
      }
    };

    fetchTutorDetails();
  }, [id]);

  useEffect(() => {
    if (userLocation && tutor?.location) {
      const dist = calculateDistance(
        userLocation.latitude,
        userLocation.longitude,
        tutor.location.latitude,
        tutor.location.longitude
      );
      setDistance(dist);
    }
  }, [userLocation, tutor]);

  useEffect(() => {
    if (!tutor || !selectedDate) return;

    // Get available time slots based on tutor's availability
    const dayOfWeek = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][selectedDate.getDay()];

    const availableSlots = [];

    // Find matching availability for selected day
    const dayAvailability = tutor.availabilities?.filter(a => a.dayOfWeek === dayOfWeek) || [];

    // Generate 1-hour slots within each availability window
    dayAvailability.forEach(availability => {
      const [startHour, startMinute] = availability.startTime.split(':').map(Number);
      const [endHour, endMinute] = availability.endTime.split(':').map(Number);

      const startTime = new Date(selectedDate);
      startTime.setHours(startHour, startMinute, 0, 0);

      const endTime = new Date(selectedDate);
      endTime.setHours(endHour, endMinute, 0, 0);

      // Create 1-hour slots
      const currentTime = new Date(startTime);
      while (currentTime < endTime) {
        const slotEndTime = new Date(currentTime);
        slotEndTime.setHours(currentTime.getHours() + 1);

        // Don't add slots that extend beyond availability end time
        if (slotEndTime <= endTime) {
          availableSlots.push({
            start: new Date(currentTime),
            end: slotEndTime
          });
        }

        // Move to next hour
        currentTime.setHours(currentTime.getHours() + 1);
      }
    });

    setAvailableTimeSlots(availableSlots);
    setSelectedTimeSlot(null);
  }, [tutor, selectedDate]);

  // Initialize map when tutor data is loaded
  useEffect(() => {
    if (tutor?.location?.latitude && tutor?.location?.longitude && tutor.shareLocation) {
      // Initialize map without checking permissions explicitly
      initializeMap();
    }
    
    return () => {
      // Clean up map instance when component unmounts
      if (mapInstance) {
        mapInstance.remove();
      }
    };
  }, [tutor]);

  // Load Leaflet CSS if not already loaded
  useEffect(() => {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
    document.head.appendChild(link);
    
    return () => {
      document.head.removeChild(link);
    };
  }, []);

  // Initialize map function with enhanced Google Maps-like styling
  const initializeMap = () => {
    if (!tutor?.location?.latitude || !tutor?.location?.longitude || !tutor.shareLocation) {
      return;
    }
    
    // Clean up previous map instance if it exists
    if (mapInstance) {
      mapInstance.remove();
    }
    
    const tutorLocation = [tutor.location.latitude, tutor.location.longitude];
    
    // Create map centered on tutor's location with enhanced UI
    const map = L.map('tutor-location-map', {
      zoomControl: false, // We'll add custom zoom controls
      attributionControl: false, // We'll add a custom attribution
    }).setView(tutorLocation, 13);
    
    setMapInstance(map);
    
    // Use a Google Maps-like tile layer
    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
    }).addTo(map);
    
    // Add custom controls for Google Maps-like UI
    const customControlsDiv = L.DomUtil.create('div', 'custom-map-controls');
    customControlsDiv.innerHTML = `
      <div class="map-controls-container">
        <div class="zoom-controls">
          <button class="zoom-btn zoom-in" title="Zoom in"><span class="icon"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></span></button>
          <button class="zoom-btn zoom-out" title="Zoom out"><span class="icon"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M19 13H5v-2h14z"/></svg></span></button>
        </div>
        <div class="locate-control">
          <button class="locate-btn" title="Show your location"><span class="icon"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0 0 13 3.06V1h-2v2.06A8.994 8.994 0 0 0 3.06 11H1v2h2.06A8.994 8.994 0 0 0 11 20.94V23h2v-2.06A8.994 8.994 0 0 0 20.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/></svg></span></button>
        </div>
      </div>
    `;
    
    // Apply CSS styles for the custom controls
    const style = document.createElement('style');
    style.innerHTML = `
      .custom-map-controls {
        position: absolute;
        right: 10px;
        top: 10px;
        z-index: 1000;
      }
      .map-controls-container {
        display: flex;
        flex-direction: column;
        gap: 10px;
      }
      .zoom-controls, .locate-control {
        background: white;
        border-radius: 4px;
        box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        overflow: hidden;
      }
      .zoom-btn, .locate-btn {
        height: 40px;
        width: 40px;
        background: white;
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background-color 0.2s;
        color: #666;
      }
      .zoom-btn:hover, .locate-btn:hover {
        background-color: #f5f5f5;
        color: #4285F4;
      }
      .zoom-controls {
        display: flex;
        flex-direction: column;
      }
      .zoom-in {
        border-bottom: 1px solid #e6e6e6;
      }
      .icon {
        width: 24px;
        height: 24px;
        display: inline-flex;
      }
    `;
    document.head.appendChild(style);
    
    // Custom attribution in Google Maps style
    const attributionDiv = L.DomUtil.create('div', 'custom-attribution');
    attributionDiv.innerHTML = `
      <div class="attribution-container">
        <div class="map-data">Map data ©${new Date().getFullYear()} OpenStreetMap contributors</div>
      </div>
    `;
    const attributionStyle = document.createElement('style');
    attributionStyle.innerHTML = `
      .custom-attribution {
        position: absolute;
        bottom: 0;
        left: 0;
        background: rgba(255, 255, 255, 0.8);
        padding: 2px 5px;
        font-size: 10px;
        color: #666;
        z-index: 1000;
      }
    `;
    document.head.appendChild(attributionStyle);
    
    // Add the custom controls to the map
    const customControls = L.control({position: 'topright'});
    customControls.onAdd = function() {
      return customControlsDiv;
    };
    customControls.addTo(map);
    
    // Add the custom attribution to the map
    const customAttribution = L.control({position: 'bottomleft'});
    customAttribution.onAdd = function() {
      return attributionDiv;
    };
    customAttribution.addTo(map);
    
    // Add event listeners for custom controls
    document.querySelector('.zoom-in').addEventListener('click', () => {
      map.zoomIn();
    });
    document.querySelector('.zoom-out').addEventListener('click', () => {
      map.zoomOut();
    });
    document.querySelector('.locate-btn').addEventListener('click', () => {
      locateUser();
    });
    
    // Create a better looking tutor marker
    const tutorIcon = L.divIcon({
      html: `<div class="tutor-marker">
              <div class="marker-icon">
                <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
              </div>
              <div class="pulse"></div>
            </div>`,
      className: '',
      iconSize: [40, 40],
      iconAnchor: [20, 40]
    });
    
    // Add marker styles
    const markerStyle = document.createElement('style');
    markerStyle.innerHTML = `
      .tutor-marker {
        display: flex;
        flex-direction: column;
        align-items: center;
        position: relative;
      }
      .marker-icon {
        width: 32px;
        height: 32px;
        background: #4285F4;
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        border: 2px solid white;
      }
      .marker-icon svg {
        transform: rotate(45deg);
      }
      .pulse {
        width: 14px;
        height: 14px;
        background: rgba(66, 133, 244, 0.3);
        border-radius: 50%;
        position: absolute;
        top: 28px;
        animation: pulse 1.5s infinite;
      }
      @keyframes pulse {
        0% {
          transform: scale(0);
          opacity: 1;
        }
        100% {
          transform: scale(2);
          opacity: 0;
        }
      }
      
      .user-marker {
        width: 24px;
        height: 24px;
        background: #4ECDC4;
        border-radius: 50%;
        border: 3px solid white;
        box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        justify-content: center;
      }
      
      .leaflet-popup-content-wrapper {
        border-radius: 8px;
        box-shadow: 0 3px 14px rgba(0,0,0,0.3);
      }
      .leaflet-popup-content {
        margin: 12px 16px;
        font-family: 'Roboto', Arial, sans-serif;
        font-size: 13px;
        line-height: 1.4;
      }
      .leaflet-popup-tip {
        box-shadow: 0 3px 14px rgba(0,0,0,0.3);
      }
      .leaflet-container a.leaflet-popup-close-button {
        color: #666;
        background: white;
        border-radius: 50%;
        width: 18px;
        height: 18px;
        font: 16px/18px Tahoma, Verdana, sans-serif;
        box-shadow: 0 1px 3px rgba(0,0,0,0.2);
        top: 6px;
        right: 6px;
      }
      
      .distance-container {
        display: flex;
        align-items: center;
        margin-top: 10px;
        padding: 10px;
        background-color: white;
        box-shadow: 0 1px 3px rgba(0,0,0,0.2);
        border-radius: 8px;
        font-family: 'Roboto', Arial, sans-serif;
      }
      .distance-icon {
        margin-right: 10px;
        color: #4285F4;
      }
      .distance-text {
        font-weight: 500;
      }
      .directions-button {
        display: flex;
        align-items: center;
        margin-left: auto;
        background: #4285F4;
        color: white;
        padding: 6px 12px;
        border-radius: 4px;
        font-weight: 500;
        text-decoration: none;
        font-size: 13px;
      }
      .directions-button svg {
        margin-right: 5px;
      }
    `;
    document.head.appendChild(markerStyle);
    
    // Place tutor marker with enhanced styling
    const tutorMarker = L.marker(tutorLocation, { icon: tutorIcon })
      .addTo(map)
      .bindPopup(`
        <div>
          <b>${tutor.user?.firstName} ${tutor.user?.lastName}</b><br>
          ${tutor.title || "Tutor"}
        </div>
      `)
      .openPopup();
    
    // Function to locate user and add to map
    const locateUser = () => {
      if (navigator.geolocation) {
        map.spin = true;
        navigator.geolocation.getCurrentPosition(
          (position) => {
            const userLocation = [position.coords.latitude, position.coords.longitude];
            
            // Remove previous user marker if exists
            map.eachLayer((layer) => {
              if (layer._userMarker) {
                map.removeLayer(layer);
              }
              if (layer._connectionLine) {
                map.removeLayer(layer);
              }
            });
            
            // Add user marker
            const userIcon = L.divIcon({
              html: `<div class="user-marker">
                      <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                        <circle cx="12" cy="12" r="8" fill="#4ECDC4"/>
                        <circle cx="12" cy="12" r="3" fill="white"/>
                      </svg>
                    </div>`,
              className: '',
              iconSize: [24, 24],
              iconAnchor: [12, 12]
            });
            
            const userMarker = L.marker(userLocation, { 
              icon: userIcon,
              zIndexOffset: 1000
            })
            .addTo(map)
            .bindPopup('Your Location');
            
            userMarker._userMarker = true;
            
            // Add line between tutor and user with nice styling
            const connectionLine = L.polyline([tutorLocation, userLocation], { 
              color: '#4285F4', 
              weight: 3,
              opacity: 0.8,
              dashArray: '8, 5',
              lineCap: 'round'
            }).addTo(map);
            connectionLine._connectionLine = true;
            
            // Calculate distance between tutor and user
            const distanceInKm = calculateDistance(
              userLocation[0], userLocation[1],
              tutorLocation[0], tutorLocation[1]
            );
            setDistance(distanceInKm);
            
            // Update user location state
            setUserLocation({
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
            });
            
            // Adjust map bounds to fit both markers
            const bounds = L.latLngBounds([tutorLocation, userLocation]);
            map.fitBounds(bounds, { padding: [50, 50] });
            
            // Add distance information at the bottom of the map
            const distanceContainer = document.createElement('div');
            distanceContainer.className = 'distance-container';
            distanceContainer.innerHTML = `
              <div class="distance-icon">
                <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                  <path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"/>
                </svg>
              </div>
              <div class="distance-text">
                Distance: <strong>${distanceInKm.toFixed(1)} km</strong> from your location
              </div>
              <a href="https://www.google.com/maps/dir/?api=1&destination=${tutorLocation[0]},${tutorLocation[1]}" 
                 class="directions-button" target="_blank">
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                  <path d="M22.43 10.59l-9.01-9.01c-.75-.75-2.07-.76-2.83 0l-9 9c-.78.78-.78 2.04 0 2.82l9 9c.39.39.9.58 1.41.58.51 0 1.02-.19 1.41-.58l8.99-8.99c.79-.76.8-2.02.03-2.82zm-10.42 10.4l-9-9 9-9 9 9-9 9z"/>
                  <path d="M8 11v4h2v-3h4v2.5l3.5-3.5L14 7.5V10H9c-.55 0-1 .45-1 1z"/>
                </svg>
                Directions
              </a>
            `;
            
            // Remove any existing distance containers
            const existingContainers = document.querySelectorAll('.distance-container');
            existingContainers.forEach(container => container.remove());
            
            // Add the distance container to the map
            document.getElementById('tutor-location-map').appendChild(distanceContainer);
            
            map.spin = false;
          },
          (error) => {
            console.error("Error getting user location:", error);
            // Show a toast instructing user to enable location
            toast.info("Enable location services to see your distance from this tutor");
            map.spin = false;
          },
          { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
      } else {
        toast.error("Geolocation is not supported by your browser");
      }
    };
    
    // Try to get user's location automatically
    locateUser();
  };

  // Calculate distance between two coordinates in kilometers using Haversine formula
  const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371; // Radius of the Earth in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = 
      Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
      Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  };

  const handleBookSession = async () => {
    if (!selectedTimeSlot) {
      toast.error('Please select a time slot');
      return;
    }

    setSubmitting(true);
    try {
      const token = localStorage.getItem('judify_token');
      const config = {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      };

      const sessionData = {
        tutorId: tutor.id,
        studentId: user.id,
        startTime: selectedTimeSlot.start.toISOString(),
        endTime: selectedTimeSlot.end.toISOString(),
        sessionType: sessionType,
        status: 'PENDING'
      };

      await axios.post('/api/sessions', sessionData, config);
      toast.success('Session request sent!');
      setSelectedTimeSlot(null);
    } catch (bookingError) {
      console.error('Error booking session:', bookingError);
      toast.error('Failed to book session. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  // Render map section of profile with enhanced UI
  const renderLocationMap = () => {
    // Show loading state
    if (loading) {
      return (
        <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-4 mt-4 flex justify-center items-center h-64">
          <div className="flex flex-col items-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-300">Loading map...</p>
          </div>
        </div>
      );
    }

    // Check if tutor exists
    if (!tutor) {
      return (
        <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-4 mt-4 h-64 flex items-center justify-center">
          <p className="text-gray-600 dark:text-gray-300">Tutor information not available</p>
        </div>
      );
    }

    // Check if tutor is sharing location
    if (!tutor.shareLocation) {
      return (
        <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-4 mt-4 flex flex-col items-center justify-center h-64">
          <div className="flex flex-col items-center text-center max-w-md">
            <FaMapMarkerAlt className="text-4xl text-gray-400 mb-3" />
            <h3 className="text-lg font-semibold text-gray-700 dark:text-gray-300 mb-1">Location Sharing Disabled</h3>
            <p className="text-gray-600 dark:text-gray-400 mb-4">
              This tutor has not enabled location sharing. You can still book a session and discuss meeting details in the chat.
            </p>
            
            {/* Location info if available */}
            {tutor.location?.city && (
              <div className="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-lg inline-block">
                <p className="text-blue-700 dark:text-blue-300 flex items-center">
                  <FaMapMarkerAlt className="mr-2" />
                  {tutor.location.city}
                  {tutor.location.state && `, ${tutor.location.state}`}
                </p>
              </div>
            )}
          </div>
        </div>
      );
    }

    // Check if location coordinates are available
    if (!tutor.location?.latitude || !tutor.location?.longitude) {
      return (
        <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-4 mt-4 h-64 flex items-center justify-center">
          <p className="text-gray-600 dark:text-gray-300">No current location available for this tutor</p>
        </div>
      );
    }

    // Show map with visual indicators
    return (
      <div className="mt-4">
        <div className="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-lg mb-3 flex items-center justify-between">
          <div className="flex items-center">
            <span className="flex h-8 w-8 bg-blue-100 dark:bg-blue-800 rounded-full items-center justify-center mr-3">
              <FaMapMarkerAlt className="text-blue-600 dark:text-blue-300" />
            </span>
            <div>
              <h3 className="font-medium text-blue-700 dark:text-blue-300">Location Available</h3>
              <p className="text-sm text-blue-600 dark:text-blue-400">
                {tutor.location.city}{tutor.location.state ? `, ${tutor.location.state}` : ''}
              </p>
            </div>
          </div>
          
          {distance !== null && (
            <div className="bg-white dark:bg-gray-800 shadow-sm px-3 py-1.5 rounded-full">
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300 flex items-center">
                <FaDirections className="mr-1.5 text-blue-500" />
                {distance.toFixed(1)} km away
              </p>
            </div>
          )}
        </div>
        
        {/* Map container */}
        <div id="tutor-location-map" className="h-80 rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700 relative"></div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="w-12 h-12 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!tutor) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <h3 className="text-lg font-medium text-gray-900 mb-2">Tutor not found</h3>
          <p className="text-gray-500 mb-4">The tutor you&apos;re looking for does not exist or has been removed.</p>
          <div className="flex justify-center space-x-4">
            <Link
              to="/student/find-tutors"
              className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
            >
              <FaChevronLeft className="mr-2" /> Back to Search
            </Link>
            <button 
              onClick={() => window.history.back()} 
              className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              Go Back
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Back Link */}
      <div className="mb-6">
        <Link
          to="/student/find-tutors"
          className="inline-flex items-center text-sm font-medium text-blue-600 hover:text-blue-800"
        >
          <FaChevronLeft className="mr-1" /> Back to Search
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2">
          {/* Tutor Header */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <div className="sm:flex items-start">
              <div className="flex-shrink-0 h-24 w-24 sm:h-32 sm:w-32 rounded-full bg-gray-300 flex items-center justify-center text-gray-700 overflow-hidden mb-4 sm:mb-0">
                <UserAvatar
                  user={tutor.user}
                  size="xl"
                  className="w-full h-full"
                />
              </div>
              <div className="sm:ml-6">
                <h1 className="text-2xl font-bold text-gray-900">
                  {tutor.user?.firstName} {tutor.user?.lastName}
                </h1>
                <p className="text-xl text-gray-600 mt-1">{tutor.title}</p>

                <div className="flex items-center mt-2">
                  {[...Array(5)].map((_, i) => (
                    <FaStar
                      key={i}
                      className={`text-lg ${
                        i < Math.round(tutor.averageRating || 0)
                          ? 'text-yellow-400'
                          : 'text-gray-300'
                      }`}
                    />
                  ))}
                  <span className="ml-2 text-gray-600">
                    {tutor.averageRating?.toFixed(1) || 'New'} ({tutor.reviewCount || 0} reviews)
                  </span>
                </div>

                <div className="flex flex-wrap items-center mt-3 text-gray-600">
                  <div className="flex items-center mr-6 mb-2">
                    <FaDollarSign className="mr-1" />
                    <span className="font-medium">${tutor.hourlyRate}/hour</span>
                  </div>

                  {tutor.isOnlineAvailable && (
                    <div className="flex items-center mr-6 mb-2">
                      <FaVideo className="text-blue-500 mr-1" />
                      <span>Online Available</span>
                    </div>
                  )}

                  {tutor.isInPersonAvailable && tutor.location && (
                    <div className="flex items-center mb-2">
                      <FaMapMarkerAlt className="text-red-500 mr-1" />
                      <span>
                        {tutor.location.city}, {tutor.location.state}
                        {distance && ` (${distance.toFixed(1)} km away)`}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Subjects & Expertise */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Subjects & Expertise</h2>
            <div className="flex flex-wrap gap-2">
              {tutor.subjects?.map((subject, index) => (
                <div
                  key={index}
                  className="px-3 py-1 rounded-full text-sm bg-blue-100 text-blue-800"
                >
                  {subject.name} <span className="text-blue-600">({subject.expertiseLevel.toLowerCase()})</span>
                </div>
              ))}
            </div>
          </div>

          {/* About */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">About</h2>
            <p className="text-gray-600 whitespace-pre-line">{tutor.bio}</p>
          </div>

          {/* Location Map - New Section */}
          {renderLocationMap()}

          {/* Education & Experience */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Education</h2>
              <p className="text-gray-600 whitespace-pre-line">{tutor.education}</p>
            </div>
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Experience</h2>
              <p className="text-gray-600 whitespace-pre-line">{tutor.experience}</p>
            </div>
          </div>

          {/* Reviews */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">
              Reviews ({reviews.length})
            </h2>
            {reviews.length === 0 ? (
              <p className="text-gray-500">No reviews yet.</p>
            ) : (
              <div className="space-y-6">
                {reviews.map((review) => (
                  <div key={review.id} className="border-b pb-6 last:border-b-0 last:pb-0">
                    <div className="flex items-start">
                      <div className="flex-shrink-0 h-10 w-10 rounded-full bg-gray-300 flex items-center justify-center overflow-hidden">
                        <UserAvatar
                          user={review.student?.user}
                          size="sm"
                          className="w-full h-full"
                        />
                      </div>
                      <div className="ml-4">
                        <h4 className="text-sm font-medium text-gray-900">
                          {review.student?.user?.firstName} {review.student?.user?.lastName?.charAt(0)}.
                        </h4>
                        <div className="flex items-center mt-1">
                          {[...Array(5)].map((_, i) => (
                            <FaStar
                              key={i}
                              className={`text-xs ${
                                i < review.rating ? 'text-yellow-400' : 'text-gray-300'
                              }`}
                            />
                          ))}
                          <span className="ml-1 text-xs text-gray-500">
                            {new Date(review.createdDate).toLocaleDateString()}
                          </span>
                        </div>
                        <p className="mt-2 text-sm text-gray-600">{review.comment}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Booking Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6 sticky top-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Book a Session</h2>

            {/* Date Selection */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <FaCalendarAlt className="inline mr-2" /> Select Date
              </label>
              <DatePicker
                selected={selectedDate}
                onChange={(date) => setSelectedDate(date)}
                minDate={new Date()}
                className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                dateFormat="MMMM d, yyyy"
              />
            </div>

            {/* Time Slot Selection */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <FaClock className="inline mr-2" /> Available Time Slots
              </label>
              {availableTimeSlots.length === 0 ? (
                <p className="text-red-500 text-sm">No available time slots for this day.</p>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  {availableTimeSlots.map((slot, index) => (
                    <button
                      key={index}
                      type="button"
                      onClick={() => setSelectedTimeSlot(slot)}
                      className={`px-3 py-2 text-sm border rounded-md ${
                        selectedTimeSlot === slot
                          ? 'bg-blue-100 border-blue-500 text-blue-700'
                          : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                      }`}
                    >
                      {slot.start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - 
                      {slot.end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Session Type Selection */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">Session Type</label>
              <div className="grid grid-cols-2 gap-3">
                {tutor.isOnlineAvailable && (
                  <button
                    type="button"
                    onClick={() => setSessionType('online')}
                    className={`flex items-center justify-center px-4 py-2 border rounded-md ${
                      sessionType === 'online'
                        ? 'bg-blue-100 border-blue-500 text-blue-700'
                        : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <FaVideo className="mr-2" /> Online
                  </button>
                )}
                {tutor.isInPersonAvailable && (
                  <button
                    type="button"
                    onClick={() => setSessionType('inPerson')}
                    className={`flex items-center justify-center px-4 py-2 border rounded-md ${
                      sessionType === 'inPerson'
                        ? 'bg-blue-100 border-blue-500 text-blue-700'
                        : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <FaMapMarkerAlt className="mr-2" /> In-person
                  </button>
                )}
              </div>
              {sessionType === 'inPerson' && distance && distance > 50 && (
                <p className="mt-2 text-sm text-yellow-600">
                  <span role="img" aria-label="warning">⚠️</span> This tutor is {distance.toFixed(1)} km away. Consider online tutoring for better convenience.
                </p>
              )}
            </div>

            {/* Pricing */}
            <div className="border-t border-gray-200 pt-4 mb-6">
              <div className="flex justify-between">
                <span className="text-gray-600">1 hour session</span>
                <span className="text-gray-900 font-medium">${tutor.hourlyRate}</span>
              </div>
            </div>

            {/* Book Button */}
            <button
              type="button"
              onClick={handleBookSession}
              disabled={!selectedTimeSlot || submitting}
              className="w-full flex items-center justify-center px-6 py-3 border border-transparent rounded-md shadow-sm text-base font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? (
                <>
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Processing...
                </>
              ) : (
                <>
                  Book Session
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TutorDetails; 
