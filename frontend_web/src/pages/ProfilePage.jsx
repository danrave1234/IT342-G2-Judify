import { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { 
  FaMapMarkerAlt, 
  FaSave, 
  FaSync, 
  FaUser, 
  FaEdit, 
  FaStar, 
  FaGraduationCap, 
  FaSchool, 
  FaHeart,
  FaCamera,
  FaMapPin,
  FaToggleOn,
  FaToggleOff
} from 'react-icons/fa';
import { useUser } from '../context/UserContext';
import { useStudentProfile } from '../context/StudentProfileContext';
import { useTutorProfile } from '../context/TutorProfileContext';
import { tutorProfileApi } from '../api/api';

const ProfilePage = () => {
  const { user, uploadProfilePicture, loading: userLoading } = useUser();
  const { profile: studentProfile, loading: studentLoading, error: studentError, updateProfile: updateStudentProfile, refreshProfile: refreshStudentProfile } = useStudentProfile();
  const { tutorProfile, loading: tutorLoading, updateProfile: updateTutorProfile } = useTutorProfile();

  const [activeTab, setActiveTab] = useState('view');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [interests, setInterests] = useState([]);
  const [newInterest, setNewInterest] = useState('');
  const [uploadingProfile, setUploadingProfile] = useState(false);
  const [shareLocation, setShareLocation] = useState(false);
  const [mapLoaded, setMapLoaded] = useState(false);
  const mapRef = useRef(null);
  const mapMarkerRef = useRef(null);

  const { register, handleSubmit, setValue, watch, formState: { errors }, reset } = useForm();
  
  // Watch location fields for changes
  const latitude = watch('location.latitude', '');
  const longitude = watch('location.longitude', '');

  const isTutor = user?.role === 'TUTOR';
  const profile = isTutor ? tutorProfile : studentProfile;
  const loading = isTutor ? tutorLoading : studentLoading || userLoading;
  const error = isTutor ? null : studentError;

  // Set form values when profile data loads
  useEffect(() => {
    if (profile) {
      // Reset form with profile data
      if (isTutor) {
        reset({
          bio: profile.bio || '',
          expertise: profile.expertise || '',
          hourlyRate: profile.hourlyRate || '',
          location: {
            city: profile.location?.city || '',
            state: profile.location?.state || '',
            country: profile.location?.country || '',
            latitude: profile.location?.latitude || '',
            longitude: profile.location?.longitude || ''
          },
          shareLocation: profile.shareLocation || false
        });
        
        // Set share location toggle state
        setShareLocation(profile.shareLocation || false);
      } else {
        reset({
          bio: profile.bio || '',
          gradeLevel: profile.gradeLevel || '',
          school: profile.school || '',
          location: {
            city: profile.location?.city || '',
            state: profile.location?.state || '',
            country: profile.location?.country || '',
            latitude: profile.location?.latitude || '',
            longitude: profile.location?.longitude || ''
          }
        });

        // Set interests
        if (profile.interests && Array.isArray(profile.interests)) {
          setInterests(profile.interests);
        }
      }
    } else if (isTutor) {
      // Set default values for tutor if no profile exists
      reset({
        bio: '',
        expertise: '',
        hourlyRate: '0',
        location: {
          city: '',
          state: '',
          country: '',
          latitude: '',
          longitude: ''
        },
        shareLocation: false
      });
    }
  }, [profile, reset, isTutor]);

  // Initialize map when location coordinates are available and map should be shown
  useEffect(() => {
    // Only load the map if we're a tutor, have coordinates, and sharing is enabled
    if (isTutor && latitude && longitude && shareLocation) {
      // Check if Leaflet is loaded
      if (!window.L) {
        // Load Leaflet from CDN if not already loaded
        const linkElement = document.createElement('link');
        linkElement.rel = 'stylesheet';
        linkElement.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
        document.head.appendChild(linkElement);
        
        const scriptElement = document.createElement('script');
        scriptElement.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        scriptElement.onload = initializeMap;
        document.body.appendChild(scriptElement);
      } else {
        initializeMap();
      }
    }
  }, [latitude, longitude, shareLocation, isTutor, activeTab]);

  // Initialize the map with the current location
  const initializeMap = () => {
    if (!window.L || !latitude || !longitude) return;
    
    // Clear existing map if already initialized
    if (mapRef.current) {
      mapRef.current.remove();
      mapRef.current = null;
    }
    
    // Make sure the map container exists
    const mapContainer = document.getElementById('location-map');
    if (!mapContainer) return;
    
    try {
      // Create map centered at the given coordinates with Google Maps-like UI
      mapRef.current = window.L.map('location-map', {
        zoomControl: false, // Custom zoom controls
        attributionControl: false, // Custom attribution
      }).setView([latitude, longitude], 13);
      
      // Use a Google Maps-like tile layer
      window.L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
      }).addTo(mapRef.current);
      
      // Add custom controls
      const customControlsDiv = window.L.DomUtil.create('div', 'custom-map-controls');
      customControlsDiv.innerHTML = `
        <div class="map-controls-container">
          <div class="zoom-controls">
            <button class="zoom-btn zoom-in" title="Zoom in"><span class="icon"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></span></button>
            <button class="zoom-btn zoom-out" title="Zoom out"><span class="icon"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M19 13H5v-2h14z"/></svg></span></button>
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
        .zoom-controls {
          background: white;
          border-radius: 4px;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          overflow: hidden;
        }
        .zoom-btn {
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
        .zoom-btn:hover {
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
        
        .leaflet-marker-icon {
          transition: transform 0.2s;
        }
        .leaflet-marker-icon:hover {
          transform: scale(1.1);
        }
        
        .custom-marker {
          width: 32px;
          height: 42px;
          background: #4285F4;
          border-radius: 50% 50% 50% 0;
          transform: rotate(-45deg);
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          border: 2px solid white;
          position: relative;
        }
        .custom-marker::after {
          content: '';
          width: 14px;
          height: 14px;
          background: rgba(66, 133, 244, 0.3);
          border-radius: 50%;
          position: absolute;
          bottom: -18px;
          left: 9px;
          transform: rotate(45deg);
        }
        .custom-marker svg {
          transform: rotate(45deg);
        }
      `;
      document.head.appendChild(style);
      
      // Add the custom controls to the map
      const customControls = window.L.control({position: 'topright'});
      customControls.onAdd = function() {
        return customControlsDiv;
      };
      customControls.addTo(mapRef.current);
      
      // Add event listeners for custom controls
      setTimeout(() => {
        document.querySelector('.zoom-in')?.addEventListener('click', () => {
          mapRef.current.zoomIn();
        });
        document.querySelector('.zoom-out')?.addEventListener('click', () => {
          mapRef.current.zoomOut();
        });
      }, 100);
      
      // Create a Google Maps-like marker
      const customIcon = window.L.divIcon({
        html: `<div class="custom-marker">
                <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
              </div>`,
        className: '',
        iconSize: [32, 42],
        iconAnchor: [16, 42],
        popupAnchor: [0, -42]
      });
      
      // Add marker at the specified position with custom icon
      mapMarkerRef.current = window.L.marker([latitude, longitude], { icon: customIcon }).addTo(mapRef.current)
        .bindPopup(`<b>Your Location</b><br>${profile?.location?.city || ''}, ${profile?.location?.state || ''}`)
        .openPopup();
          
      setMapLoaded(true);
    } catch (error) {
      console.error('Error initializing map:', error);
      toast.error('Failed to load map. Please try again.');
    }
  };

  // Update the map marker when coordinates change
  useEffect(() => {
    if (mapLoaded && mapRef.current && mapMarkerRef.current && latitude && longitude) {
      // Update marker position with animation
      mapMarkerRef.current.setLatLng([latitude, longitude]);
      
      // Update popup content
      mapMarkerRef.current.setPopupContent(`<b>Your Location</b><br>${profile?.location?.city || ''}, ${profile?.location?.state || ''}`);
      
      // Re-center map with smooth animation
      mapRef.current.setView([latitude, longitude], 13, {
        animate: true,
        duration: 1
      });
    }
  }, [latitude, longitude, mapLoaded, profile]);

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    
    try {
      console.log('Form data being submitted:', data);
      
      // Format the profile data properly
        const profileData = {
        bio: data.bio || '',
        expertise: data.expertise || '',
        hourlyRate: parseFloat(data.hourlyRate) || 0,
        location: {
          latitude: data.location.latitude || '',
          longitude: data.location.longitude || '',
          city: data.location.city || '',
          state: data.location.state || '',
          country: data.location.country || ''
        },
        shareLocation: shareLocation,
        interests: interests
      };
      
      // Log the complete data being sent to verify all fields
      console.log('Complete profile data being submitted:', profileData);
      
      let result;
      
      // Call the appropriate update function based on user role
      if (isTutor) {
        // For tutors, make sure we include all the required fields
        result = await updateTutorProfile(profileData);
      } else {
        result = await updateStudentProfile(profileData);
      }

        if (result.success) {
          toast.success('Profile updated successfully');
        
        // Reload the profile data to ensure the UI reflects the current server state
        if (isTutor) {
          // Refresh tutor profile
          await handleRefresh();
        } else {
          // Refresh student profile
          await refreshStudentProfile();
        }
        
          setActiveTab('view');
        } else {
          toast.error(result.message || 'Failed to update profile');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      toast.error('Failed to update profile: ' + error.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRefresh = async () => {
    try {
      setIsSubmitting(true);
      
      if (isTutor) {
        // For tutors, we need to reload the profile from the tutor profile API
        console.log('Refreshing tutor profile...');
        
        // Get the user ID
        const userId = user?.userId;
        if (!userId) {
          throw new Error('User ID is missing, cannot refresh profile');
        }
        
        // Manually reload the tutor profile from the API
        const response = await tutorProfileApi.getProfileByUserId(userId);
        if (response?.data) {
          console.log('Successfully refreshed tutor profile:', response.data);
          // Now we need to update the context - this would normally be done
          // by reloading the page, but we're doing it manually here
          // We don't have a direct way to do this from here, so we'll reload the page
          toast.success('Profile refreshed successfully');
          
          // Delay the reload slightly to allow the toast to be seen
          setTimeout(() => {
            window.location.reload();
          }, 1000);
      } else {
          throw new Error('Failed to reload tutor profile data');
        }
      } else {
        // For students, use the student profile refresh function from context
        await refreshStudentProfile();
        toast.success('Profile refreshed');
      }
    } catch (error) {
      console.error('Error refreshing profile:', error);
      toast.error('Failed to refresh profile: ' + error.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAddInterest = () => {
    if (newInterest.trim() === '') {
      toast.error('Please enter an interest');
      return;
    }

    setInterests([...interests, newInterest]);
    setNewInterest('');
  };

  const handleRemoveInterest = (index) => {
    const updatedInterests = [...interests];
    updatedInterests.splice(index, 1);
    setInterests(updatedInterests);
  };

  const handleGetCurrentLocation = () => {
    if (navigator.geolocation) {
      // Show a loading toast
      const loadingToastId = toast.loading("Getting your location...");
      
      navigator.geolocation.getCurrentPosition(
        (position) => {
          // Update form values with new coordinates
          setValue('location.latitude', position.coords.latitude);
          setValue('location.longitude', position.coords.longitude);
          
          // Get city, state and country using reverse geocoding
          fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${position.coords.latitude}&lon=${position.coords.longitude}`)
            .then(response => response.json())
            .then(data => {
              // Extract relevant address components
              const address = data.address || {};
              setValue('location.city', address.city || address.town || address.village || '');
              setValue('location.state', address.state || '');
              setValue('location.country', address.country || '');
              
              // Close the loading toast
              toast.dismiss(loadingToastId);
              toast.success('Location updated successfully');

              // If location sharing is enabled, update the profile immediately
              if (shareLocation) {
                updateLocationInDatabase(
                  position.coords.latitude, 
                  position.coords.longitude,
                  address.city || address.town || address.village || '',
                  address.state || '',
                  address.country || ''
                );
              }
            })
            .catch(error => {
              console.error('Geocoding error:', error);
              
              // Close the loading toast
              toast.dismiss(loadingToastId);
              toast.success('Location coordinates updated');
              
              // Still update the database with coordinates only
              if (shareLocation) {
                updateLocationInDatabase(
                  position.coords.latitude, 
                  position.coords.longitude
                );
              }
            });
        },
        (error) => {
          // More user-friendly error handling
          let errorMessage = 'Error getting location';
          
          switch(error.code) {
            case 1:
              errorMessage = 'Please enable location access in your browser settings';
              break;
            case 2:
              errorMessage = 'Unable to determine your location';
              break;
            case 3:
              errorMessage = 'Location request timed out';
              break;
          }
          
          // Dismiss loading toast and show error
          toast.dismiss(loadingToastId);
          toast.error(errorMessage);
        },
        { 
          enableHighAccuracy: true, 
          timeout: 10000, 
          maximumAge: 0 
        }
      );
    } else {
      toast.error('Geolocation is not supported by this browser');
    }
  };

  // Add a new function to update location in the database immediately
  const updateLocationInDatabase = async (latitude, longitude, city = null, state = null, country = null) => {
    if (!user || !user.userId) {
      toast.error('Unable to update location - user information missing');
      return;
    }

    try {
      // Get the correct profile ID
      const profileId = tutorProfile?.profileId;
      
      if (!profileId) {
        console.error('Cannot update location: Missing tutor profile ID');
        toast.error('Unable to update location - tutor profile information missing');
        return;
      }

      console.log('Updating location for profileId:', profileId);
      
      // Format location data correctly
      const locationData = {
        bio: tutorProfile?.bio || '',
        expertise: tutorProfile?.expertise || '',
        hourlyRate: tutorProfile?.hourlyRate || 0,
        location: {
          latitude,
          longitude,
          city: city || tutorProfile?.location?.city || '',
          state: state || tutorProfile?.location?.state || '',
          country: country || tutorProfile?.location?.country || ''
        },
        shareLocation: true // Force share location to true when explicitly updating location
      };

      console.log('Sending location update with data:', locationData);

      // Call the update API with correct parameters
      const result = await updateTutorProfile(locationData);

      if (result.success) {
        toast.success('Location updated and shared with students');
      } else {
        toast.error(result.message || 'Failed to update location');
      }
    } catch (error) {
      console.error('Error updating location:', error);
      toast.error('Failed to update location: ' + error.message);
    }
  };

  // Toggle location sharing for tutors
  const handleToggleLocationSharing = () => {
    const newValue = !shareLocation;
    setShareLocation(newValue);
    
    // Update form value immediately
    setValue('shareLocation', newValue);
    
    // If toggling on, get the current location
    if (newValue) {
      handleGetCurrentLocation();
    }
  };

  // Handle profile picture upload
  const handleProfilePictureUpload = async (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];

      // Validate file type
      const validTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/gif'];
      if (!validTypes.includes(file.type)) {
        toast.error('Please select a valid image file (JPEG, PNG, or GIF)');
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast.error('Image size should be less than 5MB');
        return;
      }

      try {
        setUploadingProfile(true);
        const result = await uploadProfilePicture(file);
        if (result.success) {
          toast.success('Profile picture updated successfully');
        } else {
          toast.error(result.message || 'Failed to upload profile picture');
        }
      } catch (error) {
        console.error('Error uploading profile picture:', error);
        toast.error('Failed to upload profile picture');
      } finally {
        setUploadingProfile(false);
      }
    }
  };

  if (!user) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="bg-white shadow rounded-lg p-6 mb-6">
          <h1 className="text-2xl font-bold text-gray-800 mb-6">Profile</h1>
          <p className="text-gray-700">Please log in to view your profile.</p>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="bg-white shadow rounded-lg p-6 mb-6">
          <h1 className="text-2xl font-bold text-gray-800 mb-6">Profile</h1>
          <div className="flex justify-center items-center h-40">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            <p className="ml-2">Loading profile...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white shadow rounded-lg overflow-hidden">
        {/* Profile Header */}
        <div className="relative h-48 flex items-end">
          {/* Simple Background Instead of Cover Photo */}
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600 to-indigo-700">
          </div>

          <div className="absolute bottom-0 left-0 w-full h-24 bg-gradient-to-t from-black opacity-40"></div>
          <div className="relative z-10 px-6 py-4 flex justify-between items-end w-full">
            <div className="flex items-center">
              {/* Profile Picture with Upload Overlay */}
              <div className="w-24 h-24 rounded-full border-4 border-white bg-white overflow-hidden relative">
                {uploadingProfile ? (
                  <div className="w-full h-full flex items-center justify-center bg-gray-200">
                    <div className="w-8 h-8 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
                  </div>
                ) : user?.profileImage ? (
                  <img
                    src={user.profileImage}
                    alt="Profile"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-gray-200">
                    <FaUser className="text-gray-500 text-4xl" />
                  </div>
                )}

                {/* Profile Picture Upload Button - Only visible in edit mode */}
                {activeTab === 'edit' && (
                  <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                    <label 
                      htmlFor="profile-upload" 
                      className={`cursor-pointer text-white ${uploadingProfile ? 'cursor-not-allowed opacity-70' : ''}`}
                    >
                      <input
                        type="file"
                        id="profile-upload"
                        accept="image/*"
                        className="hidden"
                        onChange={handleProfilePictureUpload}
                        disabled={uploadingProfile}
                      />
                      {uploadingProfile ? (
                        <div className="w-4 h-4 border-t-2 border-white border-solid rounded-full animate-spin"></div>
                      ) : (
                        <FaCamera className="text-2xl" />
                      )}
                    </label>
                  </div>
                )}
              </div>
              <div className="ml-4 text-white">
                <h1 className="text-2xl font-bold">{user?.firstName} {user?.lastName || user?.email}</h1>
                <p>{user?.username && <span className="text-gray-200">@{user.username}</span>}</p>
                <p>{isTutor ? 'Tutor' : 'Student'}</p>
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleRefresh}
                className="flex items-center px-3 py-1 bg-white text-blue-700 rounded-lg hover:bg-blue-50"
              >
                <FaSync className="mr-1" /> Refresh
              </button>
              <button
                onClick={() => setActiveTab(activeTab === 'view' ? 'edit' : 'view')}
                className="flex items-center px-3 py-1 bg-white text-blue-700 rounded-lg hover:bg-blue-50"
              >
                {activeTab === 'view' ? (
                  <>
                    <FaEdit className="mr-1" /> Edit
                  </>
                ) : (
                  <>
                    Cancel
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Profile Content */}
        <div className="p-6">
          {error && (
            <div className="mb-6 p-4 bg-red-50 text-red-700 rounded-lg">
              <p className="font-medium">Error loading profile</p>
              <p className="text-sm">{error}</p>
              <p className="text-sm mt-2">
                User ID: {user?.userId || 'Missing'}
              </p>
            </div>
          )}

          {isTutor && (!profile || Object.keys(profile).length === 0) && activeTab === 'view' && (
            <div className="mb-6 p-4 bg-yellow-50 text-yellow-700 rounded-lg">
              <p className="font-medium">No tutor profile found</p>
              <p className="text-sm">Please click the Edit button to create your tutor profile.</p>
              <button
                onClick={() => setActiveTab('edit')}
                className="mt-2 px-3 py-1 bg-yellow-100 text-yellow-800 rounded hover:bg-yellow-200"
              >
                Create Profile
              </button>
            </div>
          )}

          {activeTab === 'view' ? (
            /* View Profile */
            <div className="space-y-8">
              {/* Bio Section */}
              <div>
                <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                  <FaUser className="mr-2 text-blue-600" /> About Me
                </h2>
                <p className="text-gray-700 whitespace-pre-line">{profile?.bio || 'No bio provided'}</p>
              </div>

              {isTutor ? (
                /* Tutor Specific Info */
                <>
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaGraduationCap className="mr-2 text-blue-600" /> Expertise
                    </h2>
                    <p className="text-gray-700">{profile?.expertise || 'No expertise specified'}</p>
                  </div>

                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaStar className="mr-2 text-blue-600" /> Rating
                    </h2>
                    <div className="flex items-center">
                      <div className="flex items-center mr-2">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <FaStar
                            key={i}
                            className={`${
                              i < Math.floor(profile?.rating || 0)
                                ? 'text-yellow-400'
                                : 'text-gray-300'
                            }`}
                          />
                        ))}
                      </div>
                      <span className="text-gray-700">{profile?.rating || 0} ({profile?.totalReviews || 0} reviews)</span>
                    </div>
                  </div>

                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaSchool className="mr-2 text-blue-600" /> Hourly Rate
                    </h2>
                    <p className="text-gray-700">${profile?.hourlyRate || 0}/hour</p>
                  </div>

                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaGraduationCap className="mr-2 text-blue-600" /> Subjects
                    </h2>
                    <div className="flex flex-wrap gap-2">
                      {profile?.subjects?.length > 0 ? (
                        profile.subjects.map((subject, index) => (
                          <span key={index} className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full">
                            {subject}
                          </span>
                        ))
                      ) : (
                        <p className="text-gray-500">No subjects specified</p>
                      )}
                    </div>
                  </div>

                  {/* Location Section for Tutors */}
                  <div>
                    <div className="flex items-center justify-between">
                      <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                        <FaMapMarkerAlt className="mr-2 text-blue-600" /> Location
                      </h2>
                      {isTutor && (
                        <div className="flex items-center text-gray-600">
                          {profile?.shareLocation ? (
                            <span className="bg-green-100 text-green-800 px-3 py-1 rounded-full flex items-center text-sm font-medium">
                              <FaMapMarkerAlt className="mr-1.5" /> Location Enabled
                            </span>
                          ) : (
                            <span className="bg-gray-100 text-gray-600 px-3 py-1 rounded-full flex items-center text-sm font-medium">
                              <FaMapPin className="mr-1.5" /> Location Disabled
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                    <div>
                      {profile?.location ? (
                        <div>
                          <p className="text-gray-700 mb-2">
                            {[
                              profile.location.city,
                              profile.location.state,
                              profile.location.country
                            ]
                              .filter(Boolean)
                              .join(', ')}
                          </p>
                          {profile.shareLocation && profile.location.latitude && profile.location.longitude && (
                            <div className="mt-3 border rounded-lg overflow-hidden">
                              <div id="view-location-map" className="h-60 w-full">
                                {/* Map will be inserted here by useEffect */}
                                {!mapLoaded && (
                                  <div className="h-full w-full flex items-center justify-center bg-gray-100">
                                    <div className="text-center">
                                      <FaMapPin className="mx-auto text-2xl text-blue-500 mb-2" />
                                      <p className="text-gray-500">Loading map...</p>
                                    </div>
                                  </div>
                                )}
                              </div>
                            </div>
                          )}
                          {isTutor && !profile.shareLocation && (
                            <div className="bg-gray-100 p-4 rounded-lg mt-2 flex items-center justify-between">
                              <div>
                                <p className="text-gray-600 text-sm">Your location is saved but not visible to students</p>
                              </div>
                              <button
                                onClick={() => setActiveTab('edit')}
                                className="px-3 py-1.5 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 transition duration-150"
                              >
                                Enable Location
                              </button>
                            </div>
                          )}
                        </div>
                      ) : (
                        <div className="bg-gray-100 p-4 rounded-lg">
                          <p className="text-gray-500">No location specified</p>
                          {isTutor && (
                            <button
                              onClick={() => setActiveTab('edit')}
                              className="mt-2 px-3 py-1.5 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 transition duration-150"
                            >
                              Add Your Location
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                </>
              ) : (
                /* Student Specific Info */
                <>
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaGraduationCap className="mr-2 text-blue-600" /> Education
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <h3 className="text-gray-600 font-medium">Grade Level</h3>
                        <p className="text-gray-700">{profile?.gradeLevel || 'Not specified'}</p>
                      </div>
                      <div>
                        <h3 className="text-gray-600 font-medium">School</h3>
                        <p className="text-gray-700">{profile?.school || 'Not specified'}</p>
                      </div>
                    </div>
                  </div>

                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaMapMarkerAlt className="mr-2 text-blue-600" /> Location
                    </h2>
                    <div>
                      {profile?.location ? (
                        <p className="text-gray-700">
                          {[
                            profile.location.city,
                            profile.location.state,
                            profile.location.country
                          ]
                            .filter(Boolean)
                            .join(', ')}
                        </p>
                      ) : (
                        <p className="text-gray-500">No location specified</p>
                      )}
                    </div>
                  </div>

                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaHeart className="mr-2 text-blue-600" /> Interests
                    </h2>
                    <div className="flex flex-wrap gap-2">
                      {profile?.interests?.length > 0 ? (
                        profile.interests.map((interest, index) => (
                          <span key={index} className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full">
                            {interest}
                          </span>
                        ))
                      ) : (
                        <p className="text-gray-500">No interests specified</p>
                      )}
                    </div>
                  </div>
                </>
              )}
            </div>
          ) : (
            /* Edit Profile */
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Bio section */}
              <div>
                <label htmlFor="bio" className="block text-sm font-medium text-gray-700 mb-1">Bio</label>
                <textarea
                  id="bio"
                  rows="4"
                  {...register('bio')}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Tell us about yourself..."
                ></textarea>
              </div>

              {isTutor ? (
                /* Tutor Form Fields */
                <>
                  <div>
                    <label htmlFor="expertise" className="block text-sm font-medium text-gray-700 mb-1">Expertise</label>
                    <input
                      type="text"
                      id="expertise"
                      {...register('expertise')}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Your areas of expertise"
                    />
                  </div>

                  <div>
                    <label htmlFor="hourlyRate" className="block text-sm font-medium text-gray-700 mb-1">Hourly Rate ($)</label>
                    <input
                      type="number"
                      id="hourlyRate"
                      min="0"
                      step="0.01"
                      {...register('hourlyRate')}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Your hourly rate"
                    />
                  </div>
                  
                  {/* Location section for tutors with GPS sharing button */}
                  <div>
                    <div className="flex justify-between items-center mb-2">
                      <h3 className="text-lg font-medium text-gray-800">Location</h3>
                      
                      {isTutor && (
                        <div className="flex items-center space-x-3">
                          <button
                            type="button"
                            onClick={handleGetCurrentLocation}
                            className="flex items-center text-sm bg-blue-100 hover:bg-blue-200 text-blue-700 px-3 py-1.5 rounded-md transition duration-150"
                          >
                            <FaMapMarkerAlt className="mr-1.5" /> Update current location
                          </button>
                          
                          <button 
                            className={`flex items-center px-3 py-1.5 rounded-md transition-all duration-200 ${
                              shareLocation 
                                ? 'bg-green-600 hover:bg-green-700 text-white' 
                                : 'bg-gray-100 hover:bg-gray-200 text-gray-700'
                            }`} 
                            onClick={handleToggleLocationSharing}
                          >
                            {shareLocation ? (
                              <>
                                <FaToggleOn className="text-xl mr-1.5" /> Location Enabled
                              </>
                            ) : (
                              <>
                                <FaToggleOff className="text-xl mr-1.5" /> Enable Location
                              </>
                            )}
                          </button>
                        </div>
                      )}
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                      <div>
                        <label htmlFor="location.city" className="block text-sm font-medium text-gray-700 mb-1">City</label>
                        <input
                          type="text"
                          id="location.city"
                          {...register('location.city')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.state" className="block text-sm font-medium text-gray-700 mb-1">State/Province</label>
                        <input
                          type="text"
                          id="location.state"
                          {...register('location.state')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.country" className="block text-sm font-medium text-gray-700 mb-1">Country</label>
                        <input
                          type="text"
                          id="location.country"
                          {...register('location.country')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label htmlFor="location.latitude" className="block text-sm font-medium text-gray-700 mb-1">Latitude</label>
                        <input
                          type="text"
                          id="location.latitude"
                          {...register('location.latitude')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.longitude" className="block text-sm font-medium text-gray-700 mb-1">Longitude</label>
                        <input
                          type="text"
                          id="location.longitude"
                          {...register('location.longitude')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                    </div>
                    
                    {/* Map preview with enable button */}
                    {isTutor && (
                      <div className="mt-4">
                        {!shareLocation && (
                          <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-4">
                            <div className="flex items-start">
                              <div className="flex-shrink-0 mt-0.5">
                                <svg className="h-5 w-5 text-amber-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                </svg>
                              </div>
                              <div className="ml-3">
                                <h3 className="text-sm font-medium text-amber-800">Enable Location Sharing</h3>
                                <div className="mt-2">
                                  <p className="text-sm text-amber-700">
                                    Students will not be able to see your location on the map. Enable location sharing to make it easier for students to find you for in-person sessions.
                                  </p>
                                  <button
                                    type="button"
                                    onClick={handleToggleLocationSharing}
                                    className="mt-2 flex items-center px-3 py-1.5 bg-amber-100 hover:bg-amber-200 text-amber-800 rounded-md transition-colors duration-150"
                                  >
                                    <FaMapMarkerAlt className="mr-1.5" /> Enable Location
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>
                        )}
                        
                        {shareLocation && latitude && longitude && (
                          <div className="border rounded-lg overflow-hidden">
                            <div id="location-map" className="h-60 w-full">
                              {/* Map will be inserted here by useEffect */}
                              {!mapLoaded && (
                                <div className="h-full w-full flex items-center justify-center bg-gray-100">
                                  <div className="text-center">
                                    <FaMapPin className="mx-auto text-2xl text-blue-500 mb-2" />
                                    <p className="text-gray-500">Loading map...</p>
                                  </div>
                                </div>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </>
              ) : (
                /* Student Form Fields */
                <>
                  {/* Education section */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label htmlFor="gradeLevel" className="block text-sm font-medium text-gray-700 mb-1">Grade Level</label>
                      <input
                        type="text"
                        id="gradeLevel"
                        {...register('gradeLevel')}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        placeholder="e.g., Freshman, Sophomore, etc."
                      />
                    </div>
                    <div>
                      <label htmlFor="school" className="block text-sm font-medium text-gray-700 mb-1">School</label>
                      <input
                        type="text"
                        id="school"
                        {...register('school')}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Your school or university name"
                      />
                    </div>
                  </div>

                  {/* Location section */}
                  <div>
                    <div className="flex justify-between items-center mb-2">
                      <h3 className="text-lg font-medium text-gray-800">Location</h3>
                      <button
                        type="button"
                        onClick={handleGetCurrentLocation}
                        className="flex items-center text-sm text-blue-600 hover:text-blue-800"
                      >
                        <FaMapMarkerAlt className="mr-1" /> Use current location
                      </button>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                      <div>
                        <label htmlFor="location.city" className="block text-sm font-medium text-gray-700 mb-1">City</label>
                        <input
                          type="text"
                          id="location.city"
                          {...register('location.city')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.state" className="block text-sm font-medium text-gray-700 mb-1">State/Province</label>
                        <input
                          type="text"
                          id="location.state"
                          {...register('location.state')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.country" className="block text-sm font-medium text-gray-700 mb-1">Country</label>
                        <input
                          type="text"
                          id="location.country"
                          {...register('location.country')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label htmlFor="location.latitude" className="block text-sm font-medium text-gray-700 mb-1">Latitude</label>
                        <input
                          type="text"
                          id="location.latitude"
                          {...register('location.latitude')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.longitude" className="block text-sm font-medium text-gray-700 mb-1">Longitude</label>
                        <input
                          type="text"
                          id="location.longitude"
                          {...register('location.longitude')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Interests section */}
                  <div>
                    <h3 className="text-lg font-medium text-gray-800 mb-2">Interests</h3>
                    <div className="flex mb-2">
                      <input
                        type="text"
                        value={newInterest}
                        onChange={(e) => setNewInterest(e.target.value)}
                        className="flex-1 px-3 py-2 border border-gray-300 rounded-l-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Add an interest (e.g., Programming, Music, etc.)"
                      />
                      <button
                        type="button"
                        onClick={handleAddInterest}
                        className="px-4 py-2 bg-blue-600 text-white rounded-r-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                      >
                        Add
                      </button>
                    </div>
                    <div className="flex flex-wrap gap-2 mt-2">
                      {interests.map((interest, index) => (
                        <div key={index} className="flex items-center bg-blue-100 text-blue-800 rounded-full px-3 py-1">
                          <span>{interest}</span>
                          <button
                            type="button"
                            onClick={() => handleRemoveInterest(index)}
                            className="ml-2 text-blue-600 hover:text-blue-800 focus:outline-none"
                          >
                            &times;
                          </button>
                        </div>
                      ))}
                      {interests.length === 0 && (
                        <p className="text-gray-500 text-sm">No interests added yet.</p>
                      )}
                    </div>
                  </div>
                </>
              )}

              {/* Submit button */}
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                >
                  {isSubmitting ? (
                    <>
                      <div className="animate-spin h-4 w-4 mr-2 border-2 border-white border-t-transparent rounded-full"></div>
                      Saving...
                    </>
                  ) : (
                    <>
                      <FaSave className="mr-2" /> Save Profile
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProfilePage; 
