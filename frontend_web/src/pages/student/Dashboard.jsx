import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaCalendarAlt, FaClock} from 'react-icons/fa';
import axios from 'axios';
import { FaStar, FaMapMarkerAlt, FaDollarSign, FaVideo, FaUserFriends } from 'react-icons/fa';

const Dashboard = () => {
  const [sessions, setSessions] = useState([]);
  const [tutors, setTutors] = useState([]);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [loadingTutors, setLoadingTutors] = useState(true);
  const [error, setError] = useState(null);
  const [tutorError, setTutorError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchSessions();
    fetchFeaturedTutors();
  }, []);

  // Function to fetch featured tutors
  const fetchFeaturedTutors = async () => {
    setLoadingTutors(true);
    try {
      // Get user from localStorage
      const userString = localStorage.getItem('user');
      if (!userString) {
        console.error('No user found in localStorage');
        setLoadingTutors(false);
        return;
      }

      // Create a dynamic API URL that works in both environments
      const apiBaseUrl = window.location.hostname.includes('localhost') 
        ? 'http://localhost:8080' 
        : 'https://judify-795422705086.asia-east1.run.app';
      
      // Get token from localStorage for authentication
      const token = localStorage.getItem('token');
      
      // Headers configuration
      const headers = {
        'Authorization': token ? `Bearer ${token}` : '',
        'Content-Type': 'application/json'
      };

      // First try to fetch users with TUTOR role
      const response = await axios.get(
        `${apiBaseUrl}/api/users/findByRole/TUTOR`, 
        { headers }
      );
      
      if (response.data && Array.isArray(response.data)) {
        console.log('Found tutors:', response.data.length);
        
        // Take only a few tutors for the featured section (max 4)
        const tutorUsers = response.data.slice(0, 4);
        const tutorProfiles = [];
        
        // For each tutor user, try to get their profile details
        for (const user of tutorUsers) {
          try {
            // Try to get tutor profile by user ID
            const profileResponse = await axios.get(
              `${apiBaseUrl}/api/tutors/findByUserId/${user.userId}`,
              { headers }
            );
            
            if (profileResponse.data) {
              // Combine user and profile data
              tutorProfiles.push({
                ...profileResponse.data,
                user,
                // Copy some user fields to the top level for easier access
                firstName: user.firstName,
                lastName: user.lastName,
                username: user.username,
                email: user.email,
                profilePicture: user.profilePicture,
              });
            } else {
              // If no profile exists, just use the user data
              tutorProfiles.push({
                profileId: null,
                userId: user.userId,
                user,
                firstName: user.firstName,
                lastName: user.lastName,
                username: user.username,
                email: user.email,
                profilePicture: user.profilePicture,
                subjects: [],
                hourlyRate: 0,
                rating: 0,
                totalReviews: 0,
              });
            }
          } catch (err) {
            console.warn(`Could not fetch profile for user ${user.userId}:`, err);
            // Still include the user even without profile data
            tutorProfiles.push({
              profileId: null,
              userId: user.userId,
              user,
              firstName: user.firstName,
              lastName: user.lastName,
              username: user.username,
              email: user.email,
              profilePicture: user.profilePicture,
              subjects: [],
              hourlyRate: 0,
              rating: 0,
              totalReviews: 0,
            });
          }
        }
        
        setTutors(tutorProfiles);
      } else {
        setTutors([]);
      }
    } catch (err) {
      console.error('Error fetching tutors:', err);
      setTutorError('Failed to load featured tutors');
    } finally {
      setLoadingTutors(false);
    }
  };

  const fetchSessions = async () => {
    setLoadingSessions(true);
    try {
      // Get user from localStorage
      const userString = localStorage.getItem('user');
      if (!userString) {
        console.error('No user found in localStorage');
        setLoadingSessions(false);
        return;
      }

      const user = JSON.parse(userString);
      
      // Create a dynamic API URL that works in both environments
      const apiBaseUrl = window.location.hostname.includes('localhost') 
        ? 'http://localhost:8080' 
        : 'https://judify-795422705086.asia-east1.run.app';
      
      // Get token from localStorage for authentication
      const token = localStorage.getItem('token');
      
      // Headers configuration
      const headers = {
        'Authorization': token ? `Bearer ${token}` : '',
        'Content-Type': 'application/json'
      };

      // Try multiple endpoints in sequence, just like the mobile app does
      const endpointsToTry = [
        `/api/tutoring-sessions/findByStudent/${user.userId}`,
        `/api/tutoring-sessions/learner/${user.userId}`,
        `/api/tutoring-sessions/student/${user.userId}`,
        `/api/sessions/learner/${user.userId}`
      ];

      let response = null;
      let lastError = null;
      
      // Try each endpoint until one works
      for (const endpoint of endpointsToTry) {
        try {
          console.log(`Trying endpoint: ${apiBaseUrl}${endpoint}`);
          response = await axios.get(`${apiBaseUrl}${endpoint}`, { headers });
          
          // If we get here, request was successful
          console.log(`Success with endpoint: ${endpoint}`, response.data);
          break;
        } catch (err) {
          lastError = err;
          console.log(`Endpoint ${endpoint} failed:`, err.message);
          // Continue to next endpoint
        }
      }
      
      // If we have a successful response, use it
      if (response && response.data) {
        console.log('Sessions API response:', response);
        setSessions(response.data);
      } else {
        // If all endpoints failed, throw the last error
        throw lastError || new Error('All endpoints failed');
      }
    } catch (err) {
      console.error('Error fetching sessions:', err);
      // Only show error message if it's not a 404 (which likely means no sessions yet)
      if (err.response && err.response.status === 404) {
        console.log('No sessions found (404). This is normal for new users.');
        setSessions([]);
      } else {
        setError('Failed to load your sessions');
      }
    } finally {
      setLoadingSessions(false);
    }
  };

  // Helper to handle tutor profile navigation
  const handleViewTutorProfile = (tutor) => {
    const tutorId = tutor.profileId || tutor.userId;
    
    // Store tutor info in localStorage before navigation
    try {
      localStorage.setItem('lastViewedTutor', JSON.stringify(tutor));
    } catch (e) {
      console.warn('Failed to store tutor info in localStorage:', e);
    }
    
    // Navigate to tutor profile page using the correct route
    navigate(`/tutors/${tutorId}`);
  };

  // Include ALL statuses for upcoming sessions
  const upcomingSessions = sessions
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime)); // Sort by nearest session first

  // Helper to format date
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      month: 'short',
      day: 'numeric',
      year: 'numeric' 
    });
  };

  // Helper to format time
  const formatTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', { 
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  };

  // Helper to get status class for styling
  const getStatusClass = (status) => {
    switch(status) {
      case 'PENDING':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
      case 'NEGOTIATING':
        return 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300';
      case 'SCHEDULED':
        return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
      case 'ONGOING':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300';
      case 'COMPLETED':
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300';
    }
  };

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Student Dashboard</h1>
      
      {/* Featured Tutors Section */}
      <div className="mb-8">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Featured Tutors</h2>
          <Link 
            to="/student/find-tutors" 
            className="text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 text-sm font-medium"
          >
            See All →
          </Link>
        </div>
        
        {loadingTutors ? (
          <div className="flex justify-center items-center h-40">
            <div className="w-10 h-10 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
          </div>
        ) : tutorError ? (
          <div className="bg-red-100 dark:bg-red-900/20 rounded-lg p-4 text-red-800 dark:text-red-400 text-center">
            {tutorError}
          </div>
        ) : tutors.length > 0 ? (
          <div className="overflow-x-auto pb-4">
            <div className="flex space-x-4 min-w-max">
              {tutors.map((tutor) => (
                <div 
                  key={tutor.profileId || tutor.userId} 
                  className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-4 w-72 flex-shrink-0 flex flex-col h-full cursor-pointer"
                  onClick={() => handleViewTutorProfile(tutor)}
                >
                  <div className="flex items-center mb-3">
                    <div className="w-12 h-12 rounded-full overflow-hidden mr-3 cursor-pointer">
                      {tutor.profilePicture ? (
                        <img 
                          src={tutor.profilePicture} 
                          alt={`${tutor.firstName} ${tutor.lastName}`} 
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full bg-gray-300 dark:bg-gray-700 flex items-center justify-center">
                          <span className="text-xl">
                            {(tutor.firstName || '?')[0]}
                            {(tutor.lastName || '?')[0]}
                          </span>
                        </div>
                      )}
                    </div>
                    <div>
                      <h3 className="font-medium text-gray-900 dark:text-white cursor-pointer hover:text-primary-600 dark:hover:text-primary-400">
                        {tutor.firstName} {tutor.lastName}
                      </h3>
                      <div className="flex items-center">
                        <FaStar className="text-yellow-400 mr-1" />
                        <span className="text-sm text-gray-600 dark:text-gray-400">{tutor.rating || 'New'}</span>
                      </div>
                    </div>
                  </div>
                  
                  <div className="flex items-center text-sm text-gray-600 dark:text-gray-400 mb-1">
                    <FaDollarSign className="mr-1 flex-shrink-0" />
                    <span>${tutor.hourlyRate || 0}/hour</span>
                  </div>
                  
                  <div className="flex flex-wrap gap-1 my-2">
                    {(tutor.subjects || []).slice(0, 2).map((subject, index) => (
                      <span 
                        key={index} 
                        className="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900/20 text-blue-800 dark:text-blue-200 rounded-full"
                      >
                        {typeof subject === 'string' ? subject : (subject.subject || subject.name || 'Subject')}
                      </span>
                    ))}
                    {(!tutor.subjects || tutor.subjects.length === 0) && (
                      <span className="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-full">
                        General Tutoring
                      </span>
                    )}
                  </div>
                  
                  <button
                    className="mt-auto w-full bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-3 rounded transition duration-200"
                    onClick={(e) => {
                      e.stopPropagation();
                      window.location.href = `/student/book/${tutor.profileId || tutor.userId}`;
                    }}
                  >
                    Book Session
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="bg-white dark:bg-dark-800 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700 p-6 text-center">
            <p className="text-gray-600 dark:text-gray-400 mb-4">No tutors available at the moment.</p>
            <Link 
              to="/student/find-tutors" 
              className="inline-block py-2 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded transition-colors"
            >
              Find Tutors
            </Link>
          </div>
        )}
      </div>

      {/* All Sessions Row */}
      <div className="mt-8">
        <h2 className="text-xl font-semibold mb-4 text-gray-900 dark:text-white">All Sessions</h2>
        
        {loadingSessions ? (
          <div className="flex justify-center items-center h-40">
            <div className="w-10 h-10 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
          </div>
        ) : error ? (
          <div className="bg-red-100 dark:bg-red-900/20 rounded-lg p-4 text-red-800 dark:text-red-400 text-center">
            {error}
          </div>
        ) : upcomingSessions.length > 0 ? (
          <div className="overflow-x-auto pb-4">
            <div className="flex space-x-4 min-w-max">
              {upcomingSessions.map(session => (
                <div 
                  key={session.sessionId || session.id} 
                  className="bg-white dark:bg-dark-800 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700 p-4 w-72 flex-shrink-0"
                >
                  <div className="flex flex-col h-full">
                    <div className="mb-2">
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white">{session.subject}</h3>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        Tutor: {session.tutorName}
                      </p>
                    </div>
                    
                    <div className="flex items-center text-sm text-gray-600 dark:text-gray-400 mb-1">
                      <FaCalendarAlt className="mr-2 text-primary-500" />
                      <span>{formatDate(session.startTime)}</span>
                    </div>
                    
                    <div className="flex items-center text-sm text-gray-600 dark:text-gray-400 mb-3">
                      <FaClock className="mr-2 text-primary-500" />
                      <span>{formatTime(session.startTime)} - {formatTime(session.endTime)}</span>
                    </div>
                    
                    <div className="mt-auto">
                      <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${getStatusClass(session.status)}`}>
                        {session.status}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="bg-white dark:bg-dark-800 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700 p-6 text-center">
            <p className="text-gray-600 dark:text-gray-400 mb-4">You have no sessions scheduled.</p>
            <Link 
              to="/student/find-tutors" 
              className="inline-block py-2 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded transition-colors"
            >
              Find a tutor
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;