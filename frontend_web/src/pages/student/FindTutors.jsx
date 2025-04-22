import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaMapMarkerAlt, FaFilter, FaStar, FaChalkboardTeacher, FaVideo, FaSearch, FaTable, FaTh, FaDollarSign, FaUserFriends } from 'react-icons/fa';
import { toast } from 'react-toastify';
import { useTutorProfile } from '../../context/TutorProfileContext';
import { tutorProfileApi } from '../../api/api';

const FindTutors = () => {
  const navigate = useNavigate();
  const { searchTutors, loading } = useTutorProfile();
  const [tutors, setTutors] = useState([]);
  const [filteredTutors, setFilteredTutors] = useState([]);
  const [loadingTutors, setLoadingTutors] = useState(true);
  const [viewMode, setViewMode] = useState('grid');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSubject, setSelectedSubject] = useState('');
  const [selectedDistance, setSelectedDistance] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [minRating, setMinRating] = useState(0);
  const [sessionType, setSessionType] = useState('all');
  const [availableNow, setAvailableNow] = useState(false);
  const [priceRange, setPriceRange] = useState({ min: '', max: '' });
  const [userLocation, setUserLocation] = useState(null);
  const [subjects, setSubjects] = useState([]);

  // Static list of subject options for the filter dropdown
  const subjectOptions = [
    'All Subjects', 'Mathematics', 'Physics', 'Chemistry', 
    'Biology', 'Computer Science', 'English', 'History'
  ];

  useEffect(() => {
    // Get user's location on component mount
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          console.warn('Error getting location:', error.message);
        }
      );
    }

    // Initial fetch of tutors
    fetchTutors();
  }, []); // Empty dependency array means this runs once on mount

  // This useEffect only handles client-side filtering of already-fetched tutors
  useEffect(() => {
    if (tutors.length > 0) {
      applyFilters();
    }
  }, [selectedSubject, minRating, sessionType, availableNow, priceRange]);

  const fetchTutors = async () => {
    setLoadingTutors(true);
    try {
      console.log('Fetching tutors with search query:', searchQuery);

      // Prepare search parameters
      const params = {
        query: searchQuery,
        minRating: minRating > 0 ? minRating : null,
        minRate: priceRange.min ? parseFloat(priceRange.min) : null,
        maxRate: priceRange.max ? parseFloat(priceRange.max) : null,
        subject: selectedSubject !== 'All Subjects' && selectedSubject !== '' ? selectedSubject : null
      };

      // Search for tutors using the context function
      const result = await searchTutors(params);

      if (result && result.success) {
        console.log('Fetched tutors:', result.results);
        setTutors(result.results);
        setFilteredTutors(result.results);
      } else {
        console.error('Error fetching tutors:', result?.message);
        toast.error(result?.message || 'Failed to load tutors');

        // No mock data, just show empty state
        setTutors([]);
        setFilteredTutors([]);
      }
    } catch (error) {
      console.error('Error in fetchTutors:', error);
      toast.error('Failed to load tutors');

      // No mock data, just show empty state
      setTutors([]);
      setFilteredTutors([]);
    } finally {
      setLoadingTutors(false);
    }
  };

  const applyFilters = () => {
    if (!tutors.length) return;

    let filtered = [...tutors];

    // Subject filter
    if (selectedSubject && selectedSubject !== 'All Subjects') {
      filtered = filtered.filter(tutor => 
        tutor.subjects && tutor.subjects.some(subject => {
          const subjectName = typeof subject === 'string' ? subject : (subject.subject || subject.name || '');
          return subjectName.toLowerCase().includes(selectedSubject.toLowerCase());
        })
      );
    }

    // Rating filter (already applied in search, but filter client-side too for immediate feedback)
    if (minRating > 0) {
      filtered = filtered.filter(tutor => (tutor.rating || 0) >= minRating);
    }

    // Price range filter (already applied in search, but filter client-side too)
    if (priceRange.min) {
      filtered = filtered.filter(tutor => (tutor.hourlyRate || 0) >= parseFloat(priceRange.min));
    }
    if (priceRange.max) {
      filtered = filtered.filter(tutor => (tutor.hourlyRate || 0) <= parseFloat(priceRange.max));
    }

    // Session type filter
    if (sessionType !== 'all') {
      if (sessionType === 'online') {
        filtered = filtered.filter(tutor => tutor.isOnlineAvailable || tutor.onlineAvailable);
      } else if (sessionType === 'inPerson') {
        filtered = filtered.filter(tutor => tutor.isInPersonAvailable || tutor.inPersonAvailable);
      }
    }

    // Available now filter - disabled until real implementation is available
    // if (availableNow) {
    //   // This would check availability time slots from the backend
    // }

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
    setMinRating(0);
    setPriceRange({ min: '', max: '' });
    setSessionType('all');
    setAvailableNow(false);
    // Re-fetch tutors with cleared filters
    fetchTutors();
  };

  const handleViewProfileClick = (tutorId) => {
    console.log('Navigating to tutor profile:', tutorId);
    if (!tutorId) {
      toast.error('Cannot view profile: Missing tutor ID');
      return;
    }
    navigate(`/student/tutors/${tutorId}`);
  };

  const handleBookSessionClick = (tutorId, tutor) => {
    // Log detailed information about the tutor for debugging
    console.log('Tutor data for booking:', {
      tutorId,
      profileId: tutor.profileId,
      userId: tutor.userId,
      fullName: `${tutor.firstName || tutor.user?.firstName} ${tutor.lastName || tutor.user?.lastName}`
    });

    if (!tutorId) {
      toast.error('Cannot book session: Missing tutor ID');
      return;
    }

    // Prefer profileId if available, otherwise use userId
    const idToUse = tutor.profileId || tutor.userId;
    console.log(`Navigating to book session with ${tutor.profileId ? 'profileId' : 'userId'}: ${idToUse}`);

    // Store some basic tutor info in localStorage to help with error recovery
    try {
      const tutorInfo = {
        id: idToUse,
        name: `${tutor.firstName || tutor.user?.firstName} ${tutor.lastName || tutor.user?.lastName}`,
        profilePicture: tutor.profilePicture || tutor.user?.profilePicture || 'https://via.placeholder.com/150',
        subjects: tutor.subjects || []
      };
      localStorage.setItem('lastViewedTutor', JSON.stringify(tutorInfo));
    } catch (error) {
      console.error('Failed to store tutor info in localStorage:', error);
    }

    navigate(`/student/book/${idToUse}`);
  };

  const handleSearchInputChange = (e) => {
    setSearchQuery(e.target.value);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchTutors();
  };


  const renderGridView = () => {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredTutors.map(tutor => (
          <div key={tutor.profileId || tutor.userId} className="bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden">
            <div className="p-4">
              <div className="flex items-center mb-4">
                <div 
                  className="w-16 h-16 rounded-full overflow-hidden mr-4 cursor-pointer"
                  onClick={() => handleViewProfileClick(tutor.profileId || tutor.userId)}
                >
                  {tutor.profilePicture || tutor.user?.profilePicture ? (
                    <img 
                      src={tutor.profilePicture || tutor.user?.profilePicture} 
                      alt={`${tutor.firstName || tutor.user?.firstName} ${tutor.lastName || tutor.user?.lastName}`} 
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full bg-gray-300 dark:bg-gray-700 flex items-center justify-center">
                      <span className="text-xl">
                        {(tutor.firstName || tutor.user?.firstName || '?')[0]}
                        {(tutor.lastName || tutor.user?.lastName || '?')[0]}
                      </span>
                    </div>
                  )}
                </div>
                <div>
                  <h3 
                    className="text-lg font-semibold mb-1 hover:text-primary-600 dark:hover:text-primary-400 cursor-pointer"
                    onClick={() => handleViewProfileClick(tutor.profileId || tutor.userId)}
                  >
                    {tutor.firstName || tutor.user?.firstName} {tutor.lastName || tutor.user?.lastName}
                  </h3>
                  <div className="flex items-center">
                    <FaStar className="text-yellow-400 mr-1" />
                    <span>{tutor.rating || 'New'}</span>
                  </div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    {tutor.expertise || tutor.username || tutor.user?.username}
                  </p>
                </div>
              </div>

              <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 mb-2">
                <FaMapMarkerAlt className="mr-1" />
                {tutor.location?.city || tutor.city ? (
                  <span>
                    {tutor.location?.city || tutor.city}
                    {(tutor.location?.state || tutor.state) ? `, ${tutor.location?.state || tutor.state}` : ''}
                  </span>
                ) : (
                  <span>Location not specified</span>
                )}
              </div>

              <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 mb-3">
                <FaDollarSign className="mr-1" />
                <span>${tutor.hourlyRate || 0}/hour</span>
              </div>

              <div className="flex items-center mb-3 text-sm text-gray-500 dark:text-gray-400">
                {(tutor.isOnlineAvailable || tutor.onlineAvailable) && (
                  <div className="flex items-center mr-3">
                    <FaVideo className="mr-1" />
                    <span>Online Sessions</span>
                  </div>
                )}
                {(tutor.isInPersonAvailable || tutor.inPersonAvailable) && (
                  <div className="flex items-center">
                    <FaUserFriends className="mr-1" />
                    <span>In-person</span>
                  </div>
                )}
              </div>

              <div className="flex flex-wrap gap-1 mb-4">
                {(tutor.subjects || []).slice(0, 3).map((subject, index) => (
                  <span key={index} className="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-full">
                    {typeof subject === 'string' ? subject : subject.subject || subject.name || 'Subject'}
                  </span>
                ))}
                {(!tutor.subjects || tutor.subjects.length === 0) && (
                  <span className="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-full">
                    No subjects listed
                  </span>
                )}
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => handleViewProfileClick(tutor.profileId || tutor.userId)} 
                  className="flex-1 bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-4 rounded transition duration-200"
                >
                  View Profile
                </button>
                <button
                  onClick={() => handleBookSessionClick(tutor.profileId || tutor.userId, tutor)} 
                  className="flex-1 bg-green-600 hover:bg-green-700 text-white font-medium py-2 px-4 rounded transition duration-200"
                >
                  Book Session
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  };

  const renderListView = () => {
    return (
      <div className="space-y-4">
        {filteredTutors.map(tutor => (
          <div key={tutor.profileId || tutor.userId} className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-4">
            <div className="flex flex-col sm:flex-row">
              <div 
                className="w-20 h-20 rounded-full overflow-hidden mb-4 sm:mb-0 mr-4 cursor-pointer"
                onClick={() => handleViewProfileClick(tutor.profileId || tutor.userId)}
              >
                {tutor.profilePicture || tutor.user?.profilePicture ? (
                  <img 
                    src={tutor.profilePicture || tutor.user?.profilePicture} 
                    alt={`${tutor.firstName || tutor.user?.firstName} ${tutor.lastName || tutor.user?.lastName}`} 
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full bg-gray-300 dark:bg-gray-700 flex items-center justify-center">
                    <span className="text-xl">
                      {(tutor.firstName || tutor.user?.firstName || '?')[0]}
                      {(tutor.lastName || tutor.user?.lastName || '?')[0]}
                    </span>
                  </div>
                )}
              </div>

              <div className="flex-1">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start">
                  <div>
                    <h3 
                      className="text-lg font-semibold hover:text-primary-600 dark:hover:text-primary-400 cursor-pointer"
                      onClick={() => handleViewProfileClick(tutor.profileId || tutor.userId)}
                    >
                      {tutor.firstName || tutor.user?.firstName} {tutor.lastName || tutor.user?.lastName}
                    </h3>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                      {tutor.expertise || tutor.username || tutor.user?.username}
                    </p>

                    <div className="flex items-center mb-2">
                      <FaStar className="text-yellow-400 mr-1" />
                      <span>{tutor.rating || 'New'}</span>
                      <span className="mx-1">•</span>
                      <span>{tutor.totalReviews || 0} reviews</span>
                    </div>
                  </div>

                  <div className="mt-2 sm:mt-0 sm:text-right">
                    <p className="text-lg font-semibold">${tutor.hourlyRate || 0}/hour</p>

                    <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 justify-start sm:justify-end">
                      <FaMapMarkerAlt className="mr-1" />
                      {tutor.location?.city || tutor.city ? (
                        <span>
                          {tutor.location?.city || tutor.city}
                          {(tutor.location?.state || tutor.state) ? `, ${tutor.location?.state || tutor.state}` : ''}
                        </span>
                      ) : (
                        <span>Location not specified</span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex flex-wrap gap-1 my-2">
                  {(tutor.subjects || []).map((subject, index) => (
                    <span key={index} className="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-full">
                      {typeof subject === 'string' ? subject : subject.subject || subject.name || 'Subject'}
                    </span>
                  ))}
                  {(!tutor.subjects || tutor.subjects.length === 0) && (
                    <span className="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-full">
                      No subjects listed
                    </span>
                  )}
                </div>

                <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-3">
                  {tutor.bio || tutor.biography || 'No bio available'}
                </p>

                <div className="flex items-center justify-between mt-2">
                  <div className="flex items-center text-sm text-gray-500 dark:text-gray-400">
                    {(tutor.isOnlineAvailable || tutor.onlineAvailable) && (
                      <div className="flex items-center mr-3">
                        <FaVideo className="mr-1" />
                        <span>Online</span>
                      </div>
                    )}
                    {(tutor.isInPersonAvailable || tutor.inPersonAvailable) && (
                      <div className="flex items-center">
                        <FaUserFriends className="mr-1" />
                        <span>In-person</span>
                      </div>
                    )}
                  </div>

                  <div className="flex gap-2">
                    <button
                      onClick={() => handleViewProfileClick(tutor.profileId || tutor.userId)} 
                      className="bg-primary-600 hover:bg-primary-700 text-white font-medium py-1.5 px-4 rounded transition duration-200"
                    >
                      View Profile
                    </button>
                    <button
                      onClick={() => handleBookSessionClick(tutor.profileId || tutor.userId, tutor)} 
                      className="bg-green-600 hover:bg-green-700 text-white font-medium py-1.5 px-4 rounded transition duration-200"
                    >
                      Book Session
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="p-4 md:p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold mb-2">Find Tutors</h1>
        <p className="text-gray-600 dark:text-gray-400">
          Browse our selection of qualified tutors and find the perfect match for your learning needs
        </p>
      </div>

      <div className="flex flex-col md:flex-row gap-6">
        {/* Filters Panel */}
        <div className={`${showFilters ? 'block' : 'hidden md:block'} w-full md:w-64 lg:w-72 bg-white dark:bg-dark-800 p-4 rounded-lg shadow mb-4 md:mb-0`}>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold">Filters</h2>
            <button 
              onClick={resetFilters}
              className="text-primary-600 dark:text-primary-400 text-sm hover:underline"
            >
              Reset
            </button>
          </div>

          <div className="space-y-4">
            {/* Subject Filter */}
            <div>
              <label htmlFor="subject" className="block text-sm font-medium mb-1">Subject</label>
              <select
                id="subject"
                value={selectedSubject}
                onChange={(e) => setSelectedSubject(e.target.value)}
                className="w-full p-2 border border-gray-300 dark:border-gray-700 rounded bg-white dark:bg-dark-700 text-gray-900 dark:text-gray-100"
              >
                <option value="">All Subjects</option>
                {subjectOptions.filter(s => s !== 'All Subjects').map((subject, index) => (
                  <option key={index} value={subject}>{subject}</option>
                ))}
              </select>
            </div>

            {/* Rating Filter */}
            <div>
              <label className="block text-sm font-medium mb-1">Minimum Rating</label>
              <div className="flex items-center">
                {[0, 1, 2, 3, 4, 5].map(rating => (
                  <button
                    key={rating}
                    onClick={() => setMinRating(rating)}
                    className={`mr-1 p-1 ${minRating >= rating ? 'text-yellow-400' : 'text-gray-300 dark:text-gray-600'}`}
                  >
                    {rating === 0 ? 'Any' : <FaStar />}
                  </button>
                ))}
              </div>
            </div>

            {/* Price Range Filter */}
            <div>
              <label className="block text-sm font-medium mb-1">Price Range ($/hr)</label>
              <div className="flex items-center space-x-2">
                <input
                  type="number"
                  placeholder="Min"
                  value={priceRange.min}
                  onChange={(e) => setPriceRange({...priceRange, min: e.target.value})}
                  className="w-full p-2 border border-gray-300 dark:border-gray-700 rounded bg-white dark:bg-dark-700 text-gray-900 dark:text-gray-100"
                />
                <span>-</span>
                <input
                  type="number"
                  placeholder="Max"
                  value={priceRange.max}
                  onChange={(e) => setPriceRange({...priceRange, max: e.target.value})}
                  className="w-full p-2 border border-gray-300 dark:border-gray-700 rounded bg-white dark:bg-dark-700 text-gray-900 dark:text-gray-100"
                />
              </div>
            </div>

            {/* Session Type Filter */}
            <div>
              <label className="block text-sm font-medium mb-1">Session Type</label>
              <div className="space-y-2">
                <div className="flex items-center">
                  <input
                    type="radio"
                    id="session-all"
                    checked={sessionType === 'all'}
                    onChange={() => setSessionType('all')}
                    className="mr-2"
                  />
                  <label htmlFor="session-all">All</label>
                </div>
                <div className="flex items-center">
                  <input
                    type="radio"
                    id="session-online"
                    checked={sessionType === 'online'}
                    onChange={() => setSessionType('online')}
                    className="mr-2"
                  />
                  <label htmlFor="session-online">Online Only</label>
                </div>
                <div className="flex items-center">
                  <input
                    type="radio"
                    id="session-inperson"
                    checked={sessionType === 'inPerson'}
                    onChange={() => setSessionType('inPerson')}
                    className="mr-2"
                  />
                  <label htmlFor="session-inperson">In-Person Only</label>
                </div>
              </div>
            </div>

            {/* Available Now Filter */}
            <div className="flex items-center">
              <input
                type="checkbox"
                id="available-now"
                checked={availableNow}
                onChange={() => setAvailableNow(!availableNow)}
                className="mr-2"
              />
              <label htmlFor="available-now">Available Now</label>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="flex-1">
          {/* Search and Controls */}
          <div className="bg-white dark:bg-dark-800 p-4 rounded-lg shadow mb-4">
            <div className="flex flex-col md:flex-row gap-2 justify-between">
              <div className="w-full md:w-2/3">
                <form onSubmit={handleSearchSubmit} className="relative">
                  <input
                    type="text"
                    placeholder="Search tutors by name or username..."
                    value={searchQuery}
                    onChange={handleSearchInputChange}
                    className="w-full p-2 pl-8 border border-gray-300 dark:border-gray-700 rounded bg-white dark:bg-dark-700 text-gray-900 dark:text-gray-100"
                  />
                  <div className="absolute left-2 top-1/2 transform -translate-y-1/2 text-gray-500">
                    <FaSearch />
                  </div>
                  <button 
                    type="submit"
                    className="absolute right-2 top-1/2 transform -translate-y-1/2 bg-primary-600 text-white p-1 rounded"
                  >
                    <FaSearch />
                  </button>
                </form>
              </div>

              <div className="flex md:self-start gap-2">
                <button
                  onClick={() => setShowFilters(!showFilters)}
                  className="md:hidden flex items-center bg-gray-100 dark:bg-dark-700 px-3 py-2 rounded"
                >
                  <FaFilter className="mr-1" /> 
                  {showFilters ? 'Hide' : 'Show'} Filters
                </button>

                <div className="flex items-center bg-gray-100 dark:bg-dark-700 rounded">
                  <button
                    onClick={() => setViewMode('grid')}
                    className={`p-2 ${viewMode === 'grid' ? 'text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400'}`}
                  >
                    <FaTh />
                  </button>
                  <button
                    onClick={() => setViewMode('list')}
                    className={`p-2 ${viewMode === 'list' ? 'text-primary-600 dark:text-primary-400' : 'text-gray-500 dark:text-gray-400'}`}
                  >
                    <FaTable />
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Tutors List */}
          <div className="bg-gray-50 dark:bg-dark-900 p-4 rounded-lg">
            {loadingTutors ? (
              <div className="flex justify-center items-center h-64">
                <div className="w-12 h-12 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
                <p className="ml-3">Loading tutors...</p>
              </div>
            ) : filteredTutors.length > 0 ? (
              <>
                <div className="mb-4">
                  <p className="text-gray-600 dark:text-gray-400">
                    Showing {filteredTutors.length} {filteredTutors.length === 1 ? 'tutor' : 'tutors'}
                  </p>
                </div>
                {viewMode === 'grid' ? renderGridView() : renderListView()}
              </>
            ) : (
              <div className="text-center py-12">
                <p className="text-lg text-gray-600 dark:text-gray-400 mb-4">No tutors found matching your criteria</p>
                <button
                  onClick={resetFilters}
                  className="bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-4 rounded transition duration-200"
                >
                  Reset Filters
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default FindTutors; 
