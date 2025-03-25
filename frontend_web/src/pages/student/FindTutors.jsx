import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaMapMarkerAlt, FaFilter, FaStar, FaChalkboardTeacher, FaVideo, FaSearch } from 'react-icons/fa';
import { toast } from 'react-toastify';
import axios from 'axios';

const FindTutors = () => {
  const [tutors, setTutors] = useState([]);
  const [filteredTutors, setFilteredTutors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSubject, setSelectedSubject] = useState('');
  const [selectedDistance, setSelectedDistance] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [minRating, setMinRating] = useState(0);
  const [sessionType, setSessionType] = useState('all');
  const [availableNow, setAvailableNow] = useState(false);
  const [priceRange, setPriceRange] = useState({ min: 0, max: 200 });
  const [userLocation, setUserLocation] = useState(null);
  const [subjects, setSubjects] = useState([]);

  useEffect(() => {
    // Get user's location
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          toast.error('Error getting location: ' + error.message);
        }
      );
    }

    const fetchTutors = async () => {
      try {
        const token = localStorage.getItem('judify_token');
        const config = {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        };

        const res = await axios.get('/api/tutors', config);
        setTutors(res.data);
        setFilteredTutors(res.data);
        
        // Extract unique subjects from tutors
        const allSubjects = res.data.flatMap(tutor => 
          tutor.subjects ? tutor.subjects.map(subject => subject.name) : []
        );
        const uniqueSubjects = [...new Set(allSubjects)];
        setSubjects(uniqueSubjects);
        
        setLoading(false);
      } catch (error) {
        toast.error('Failed to load tutors');
        setLoading(false);
      }
    };

    fetchTutors();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [searchQuery, selectedSubject, selectedDistance, minRating, sessionType, availableNow, priceRange]);

  const applyFilters = () => {
    let filtered = [...tutors];

    // Filter by search query
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(tutor => 
        tutor.user?.firstName?.toLowerCase().includes(query) ||
        tutor.user?.lastName?.toLowerCase().includes(query) ||
        tutor.title?.toLowerCase().includes(query) ||
        tutor.bio?.toLowerCase().includes(query) ||
        tutor.subjects?.some(subject => subject.name.toLowerCase().includes(query))
      );
    }

    // Filter by subject
    if (selectedSubject) {
      filtered = filtered.filter(tutor => 
        tutor.subjects?.some(subject => subject.name === selectedSubject)
      );
    }

    // Filter by distance (if user location available)
    if (selectedDistance && userLocation) {
      const distanceInKm = parseInt(selectedDistance);
      filtered = filtered.filter(tutor => {
        if (!tutor.location?.latitude || !tutor.location?.longitude) return false;
        
        const distance = calculateDistance(
          userLocation.latitude,
          userLocation.longitude,
          tutor.location.latitude,
          tutor.location.longitude
        );
        
        return distance <= distanceInKm;
      });
    }

    // Filter by rating
    if (minRating > 0) {
      filtered = filtered.filter(tutor => (tutor.averageRating || 0) >= minRating);
    }

    // Filter by session type
    if (sessionType !== 'all') {
      if (sessionType === 'online') {
        filtered = filtered.filter(tutor => tutor.isOnlineAvailable);
      } else if (sessionType === 'inPerson') {
        filtered = filtered.filter(tutor => tutor.isInPersonAvailable);
      }
    }

    // Filter by available now
    if (availableNow) {
      const now = new Date();
      const dayOfWeek = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][now.getDay()];
      const currentTime = now.getHours() * 60 + now.getMinutes();
      
      filtered = filtered.filter(tutor => {
        if (!tutor.availabilities) return false;
        
        return tutor.availabilities.some(slot => {
          if (slot.dayOfWeek !== dayOfWeek) return false;
          
          const [startHour, startMinute] = slot.startTime.split(':').map(Number);
          const [endHour, endMinute] = slot.endTime.split(':').map(Number);
          
          const slotStartMinutes = startHour * 60 + startMinute;
          const slotEndMinutes = endHour * 60 + endMinute;
          
          return currentTime >= slotStartMinutes && currentTime <= slotEndMinutes;
        });
      });
    }

    // Filter by price range
    filtered = filtered.filter(tutor => {
      const rate = parseFloat(tutor.hourlyRate || 0);
      return rate >= priceRange.min && rate <= priceRange.max;
    });

    setFilteredTutors(filtered);
  };

  // Calculate distance between two points using Haversine formula
  const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371; // Earth radius in km
    const dLat = deg2rad(lat2 - lat1);
    const dLon = deg2rad(lon2 - lon1);
    const a = 
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
      Math.sin(dLon / 2) * Math.sin(dLon / 2); 
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); 
    const distance = R * c; // Distance in km
    return distance;
  };

  const deg2rad = (deg) => {
    return deg * (Math.PI / 180);
  };

  const resetFilters = () => {
    setSearchQuery('');
    setSelectedSubject('');
    setSelectedDistance('');
    setMinRating(0);
    setSessionType('all');
    setAvailableNow(false);
    setPriceRange({ min: 0, max: 200 });
    setFilteredTutors(tutors);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Find Tutors</h1>
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="mt-3 md:mt-0 inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
        >
          <FaFilter className="mr-2" /> {showFilters ? 'Hide Filters' : 'Show Filters'}
        </button>
      </div>

      <div className="mb-6">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1">
            <div className="relative">
              <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Search by name, subject, or keyword..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </div>
          <div className="md:w-1/4">
            <select
              value={selectedSubject}
              onChange={(e) => setSelectedSubject(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">All Subjects</option>
              {subjects.map((subject, index) => (
                <option key={index} value={subject}>{subject}</option>
              ))}
            </select>
          </div>
          {userLocation && (
            <div className="md:w-1/4">
              <select
                value={selectedDistance}
                onChange={(e) => setSelectedDistance(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">Any Distance</option>
                <option value="5">Within 5 km</option>
                <option value="10">Within 10 km</option>
                <option value="25">Within 25 km</option>
                <option value="50">Within 50 km</option>
              </select>
            </div>
          )}
        </div>
      </div>

      {showFilters && (
        <div className="bg-gray-50 p-4 rounded-lg mb-6 border border-gray-200">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Minimum Rating</label>
              <div className="flex items-center">
                {[1, 2, 3, 4, 5].map((rating) => (
                  <button
                    key={rating}
                    type="button"
                    onClick={() => setMinRating(rating === minRating ? 0 : rating)}
                    className="mr-1"
                  >
                    <FaStar 
                      className={`text-xl ${rating <= minRating ? 'text-yellow-400' : 'text-gray-300'}`} 
                    />
                  </button>
                ))}
                {minRating > 0 && (
                  <span className="text-sm text-gray-500 ml-2">{minRating}+ stars</span>
                )}
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Session Type</label>
              <div className="flex space-x-4">
                <label className="inline-flex items-center">
                  <input
                    type="radio"
                    name="sessionType"
                    checked={sessionType === 'all'}
                    onChange={() => setSessionType('all')}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <span className="ml-2 text-sm text-gray-700">All</span>
                </label>
                <label className="inline-flex items-center">
                  <input
                    type="radio"
                    name="sessionType"
                    checked={sessionType === 'online'}
                    onChange={() => setSessionType('online')}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <span className="ml-2 text-sm text-gray-700">Online</span>
                </label>
                <label className="inline-flex items-center">
                  <input
                    type="radio"
                    name="sessionType"
                    checked={sessionType === 'inPerson'}
                    onChange={() => setSessionType('inPerson')}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <span className="ml-2 text-sm text-gray-700">In-person</span>
                </label>
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Availability</label>
              <label className="inline-flex items-center">
                <input
                  type="checkbox"
                  checked={availableNow}
                  onChange={() => setAvailableNow(!availableNow)}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <span className="ml-2 text-sm text-gray-700">Available now</span>
              </label>
            </div>
          </div>
          
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Price Range: ${priceRange.min} - ${priceRange.max}
            </label>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <input
                  type="range"
                  min="0"
                  max="200"
                  value={priceRange.min}
                  onChange={(e) => setPriceRange({ ...priceRange, min: parseInt(e.target.value) })}
                  className="w-full"
                />
                <div className="flex justify-between text-xs text-gray-500">
                  <span>$0</span>
                  <span>$100</span>
                  <span>$200</span>
                </div>
              </div>
              <div>
                <input
                  type="range"
                  min="0"
                  max="200"
                  value={priceRange.max}
                  onChange={(e) => setPriceRange({ ...priceRange, max: parseInt(e.target.value) })}
                  className="w-full"
                />
                <div className="flex justify-between text-xs text-gray-500">
                  <span>$0</span>
                  <span>$100</span>
                  <span>$200</span>
                </div>
              </div>
            </div>
          </div>
          
          <div className="flex justify-end">
            <button
              onClick={resetFilters}
              className="px-4 py-2 text-sm text-gray-700 hover:text-gray-900"
            >
              Reset Filters
            </button>
          </div>
        </div>
      )}
      
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="w-12 h-12 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
        </div>
      ) : filteredTutors.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <FaSearch className="mx-auto text-4xl text-gray-400 mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No tutors found</h3>
          <p className="text-gray-500">Try adjusting your filters or search query.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredTutors.map((tutor) => (
            <div key={tutor.id} className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200">
              <div className="p-6">
                <div className="flex items-start">
                  <div className="flex-shrink-0 h-16 w-16 rounded-full bg-gray-300 flex items-center justify-center text-gray-700 overflow-hidden">
                    {tutor.user?.profileImage ? (
                      <img
                        src={tutor.user.profileImage}
                        alt={`${tutor.user?.firstName} ${tutor.user?.lastName}`}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <FaChalkboardTeacher size={32} />
                    )}
                  </div>
                  <div className="ml-4 flex-1">
                    <h3 className="text-lg font-medium text-gray-900">
                      {tutor.user?.firstName} {tutor.user?.lastName}
                    </h3>
                    <p className="text-sm text-gray-500">{tutor.title}</p>
                    <div className="flex items-center mt-1">
                      {[...Array(5)].map((_, i) => (
                        <FaStar
                          key={i}
                          className={`text-sm ${
                            i < Math.round(tutor.averageRating || 0)
                              ? 'text-yellow-400'
                              : 'text-gray-300'
                          }`}
                        />
                      ))}
                      <span className="ml-1 text-sm text-gray-500">
                        {tutor.averageRating?.toFixed(1) || 'New'} ({tutor.reviewCount || 0} reviews)
                      </span>
                    </div>
                  </div>
                </div>
                
                {/* Subjects */}
                <div className="mt-4">
                  <div className="flex flex-wrap gap-2">
                    {tutor.subjects?.slice(0, 3).map((subject, index) => (
                      <span
                        key={index}
                        className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                      >
                        {subject.name}
                      </span>
                    ))}
                    {tutor.subjects && tutor.subjects.length > 3 && (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                        +{tutor.subjects.length - 3} more
                      </span>
                    )}
                  </div>
                </div>
                
                {/* Session Types and Location */}
                <div className="mt-4 text-sm text-gray-500">
                  <div className="flex items-center">
                    {tutor.isOnlineAvailable && (
                      <div className="flex items-center mr-4">
                        <FaVideo className="text-blue-500 mr-1" />
                        <span>Online</span>
                      </div>
                    )}
                    {tutor.isInPersonAvailable && tutor.location && (
                      <div className="flex items-center">
                        <FaMapMarkerAlt className="text-red-500 mr-1" />
                        <span>{tutor.location.city}, {tutor.location.state}</span>
                      </div>
                    )}
                  </div>
                </div>
                
                {/* Distance if user location is available */}
                {userLocation && tutor.location?.latitude && tutor.location?.longitude && (
                  <div className="mt-2 text-sm text-gray-500">
                    <span>
                      {calculateDistance(
                        userLocation.latitude,
                        userLocation.longitude,
                        tutor.location.latitude,
                        tutor.location.longitude
                      ).toFixed(1)} km away
                    </span>
                  </div>
                )}
                
                {/* Hourly Rate */}
                <div className="mt-4">
                  <span className="text-xl font-bold text-gray-900">${tutor.hourlyRate}/hr</span>
                </div>
                
                <div className="mt-4">
                  <Link
                    to={`/student/tutor/${tutor.id}`}
                    className="block w-full text-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                  >
                    View Profile
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default FindTutors; 