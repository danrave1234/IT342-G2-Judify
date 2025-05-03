import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaVideo, FaMapMarkerAlt, FaClock, FaCalendarAlt, FaSearch } from 'react-icons/fa';
import { SESSION_STATUS } from '../../types';
import axios from 'axios';

const Sessions = () => {
  const [sessions, setSessions] = useState([]);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('upcoming');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchSessions();
  }, []);

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

  // Calculate upcoming sessions - all except COMPLETED
  const upcomingSessions = sessions
    .filter(session => session.status !== SESSION_STATUS.COMPLETED)
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime)); // Sort by nearest session first
  
  // Calculate past sessions - only COMPLETED
  const pastSessions = sessions
    .filter(session => session.status === SESSION_STATUS.COMPLETED)
    .sort((a, b) => new Date(b.startTime) - new Date(a.startTime)); // Most recent completed session first

  // Filter sessions based on search term
  const filteredSessions = activeTab === 'upcoming' 
    ? upcomingSessions.filter(session => 
        searchTerm === '' || 
        session.subject?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        session.tutorName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        session.notes?.toLowerCase().includes(searchTerm.toLowerCase())
      )
    : pastSessions.filter(session => 
        searchTerm === '' || 
        session.subject?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        session.tutorName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        session.notes?.toLowerCase().includes(searchTerm.toLowerCase())
      );

  const formatSessionTime = (startTime, endTime) => {
    const start = new Date(startTime);
    const end = new Date(endTime);
    
    const date = start.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short', 
      day: 'numeric',
      year: 'numeric'
    });
    
    const startStr = start.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit'
    });
    
    const endStr = end.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit'
    });
    
    return { date, time: `${startStr} - ${endStr}` };
  };

  const getStatusClass = (status) => {
    switch(status) {
      case SESSION_STATUS.SCHEDULED: 
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-300';
      case SESSION_STATUS.CONFIRMED:
        return 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300';
      case SESSION_STATUS.COMPLETED:
        return 'bg-purple-100 text-purple-800 dark:bg-purple-900/20 dark:text-purple-300';
      case SESSION_STATUS.CANCELLED:
        return 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300';
      case SESSION_STATUS.PENDING:
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-300';
      case SESSION_STATUS.NEGOTIATING:
        return 'bg-orange-100 text-orange-800 dark:bg-orange-900/20 dark:text-orange-300';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300';
    }
  };

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">My Tutoring Sessions</h1>
      
      {/* Tabs */}
      <div className="mb-6 border-b border-gray-200 dark:border-dark-700">
        <div className="flex space-x-8">
          <button
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'upcoming'
                ? 'border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
            }`}
            onClick={() => setActiveTab('upcoming')}
          >
            Upcoming Sessions
          </button>
          <button
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'past'
                ? 'border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
            }`}
            onClick={() => setActiveTab('past')}
          >
            Past Sessions
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="mb-6">
        <div className="relative">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
            <FaSearch className="text-gray-400 dark:text-gray-500" />
          </div>
          <input
            type="text"
            className="block w-full pl-10 pr-3 py-2 bg-white dark:bg-dark-700 border border-gray-300 dark:border-dark-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 dark:focus:ring-primary-400 focus:border-primary-500 dark:focus:border-primary-400 text-gray-900 dark:text-white"
            placeholder={`Search ${activeTab === 'upcoming' ? 'upcoming' : 'past'} sessions...`}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {loadingSessions ? (
        <div className="flex justify-center items-center h-64">
          <div className="w-12 h-12 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
        </div>
      ) : error ? (
        <div className="bg-red-100 dark:bg-red-900/20 border border-red-400 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded mb-4">
          Failed to load sessions. Please try again later.
        </div>
      ) : (
        <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden">
          <div className="p-6 border-b border-gray-200 dark:border-dark-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              {activeTab === 'upcoming' ? 'All Upcoming Sessions' : 'Past Sessions'}
            </h2>
            <p className="text-gray-500 dark:text-gray-400 mt-2">
              {activeTab === 'upcoming' 
                ? 'Displaying all pending, scheduled, and confirmed sessions'
                : 'Displaying only completed sessions'}
            </p>
          </div>
          
          {filteredSessions.length > 0 ? (
            <div className="divide-y divide-gray-200 dark:divide-dark-700">
              {filteredSessions.map(session => {
                const { date, time } = formatSessionTime(session.startTime, session.endTime);
                return (
                  <div key={session.sessionId} className="p-6 hover:bg-gray-50 dark:hover:bg-dark-700 transition duration-150">
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 md:gap-6">
                      {/* Left column - Tutor info */}
                      <div className="md:col-span-1">
                        <div className="flex items-center">
                          <img 
                            src={session.tutorProfilePicture || "https://via.placeholder.com/40"} 
                            alt={`${session.tutorName}'s profile`} 
                            className="w-12 h-12 rounded-full mr-3 object-cover flex-shrink-0"
                          />
                          <div>
                            <h3 className="font-medium text-gray-900 dark:text-white">{session.tutorName || 'Unnamed Tutor'}</h3>
                            <p className="text-sm text-gray-500 dark:text-gray-400">{session.subject || 'General Tutoring'}</p>
                            <span className={`inline-block mt-1 px-3 py-1 rounded-full text-xs font-medium ${getStatusClass(session.status)}`}>
                              {session.status}
                            </span>
                          </div>
                        </div>
                      </div>
                      
                      {/* Middle column - Session details */}
                      <div className="md:col-span-1">
                        <div className="space-y-2">
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            <FaCalendarAlt className="mr-2 text-primary-500 flex-shrink-0" /> 
                            <span>{date}</span>
                          </p>
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            <FaClock className="mr-2 text-primary-500 flex-shrink-0" /> 
                            <span>{time}</span>
                          </p>
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            {session.meetingLink ? (
                              <>
                                <FaVideo className="mr-2 text-primary-500 flex-shrink-0" /> 
                                <span>Online Session</span>
                              </>
                            ) : (
                              <>
                                <FaMapMarkerAlt className="mr-2 text-primary-500 flex-shrink-0" /> 
                                <span>In-person</span>
                              </>
                            )}
                          </p>
                        </div>
                      </div>
                      
                      {/* Right column - Price and actions */}
                      <div className="md:col-span-1 flex flex-col md:items-end justify-between">
                        <div className="text-xl font-bold text-gray-900 dark:text-white mb-2">
                          ${session.price || 0}
                        </div>
                        
                        {session.notes && (
                          <div className="bg-gray-50 dark:bg-dark-700 p-2 rounded-md mb-2 max-w-xs md:ml-auto">
                            <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
                              {session.notes}
                            </p>
                          </div>
                        )}
                        
                        <div>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="p-6 text-center">
              <p className="text-gray-500 dark:text-gray-400">
                {activeTab === 'upcoming' 
                  ? searchTerm ? 'No matching upcoming sessions found.' : 'No upcoming sessions found. Book a session with a tutor to get started!'
                  : searchTerm ? 'No matching past sessions found.' : 'No completed sessions found.'}
              </p>
              {activeTab === 'upcoming' && !searchTerm && (
                <Link to="/student/find-tutors" className="mt-4 inline-block px-4 py-2 bg-primary-600 text-white rounded hover:bg-primary-700 transition duration-200">
                  Find a Tutor
                </Link>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Sessions; 