import { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { 
  FaVideo, 
  FaMapMarkerAlt, 
  FaClock, 
  FaCalendarAlt, 
  FaSearch, 
  FaComment, 
  FaCheck, 
  FaTimes, 
  FaThumbsUp,
  FaCheckDouble,
  FaCreditCard
} from 'react-icons/fa';
import { SESSION_STATUS } from '../../types';
import axios from 'axios';
import { conversationApi } from '../../api/api';
import { toast } from 'react-toastify';
import { formatSessionTime as formatSessionTimeUtil } from '../../utils/dateUtils';

const Sessions = () => {
  const [sessions, setSessions] = useState([]);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('upcoming');
  const [searchTerm, setSearchTerm] = useState('');
  const [processingConversation, setProcessingConversation] = useState(false);
  const navigate = useNavigate();
  const { sessionId } = useParams(); // Get sessionId from URL if present

  useEffect(() => {
    fetchSessions();
    
    // If sessionId is present in the URL, automatically redirect to conversation
    if (sessionId) {
      redirectToSessionConversation(sessionId);
    }
  }, [sessionId]);

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
    return formatSessionTimeUtil(startTime, endTime);
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

  // Function to handle session click
  const handleSessionClick = async (session) => {
    if (processingConversation) return;
    
    setProcessingConversation(true);
    
    // Display loading toast
    const loadingToastId = toast.loading("Opening conversation...");
    
    try {
      // Get user from localStorage
      const userString = localStorage.getItem('user');
      if (!userString) {
        toast.update(loadingToastId, { render: "User information not found, please login again", type: "error", isLoading: false, autoClose: 3000 });
        setProcessingConversation(false);
        return;
      }
      
      const user = JSON.parse(userString);
      
      // Create conversation data - matches mobile app format
      const conversationData = {
        studentId: user.userId,
        tutorId: session.tutorId,
        sessionId: session.sessionId || session.id,
        lastMessageTime: new Date().toISOString()
      };
      
      console.log('Creating conversation with data:', conversationData);
      
      let conversationId;
      
      // Try to find an existing conversation first
      try {
        const conversationsResponse = await conversationApi.getConversations(user.userId);
        const conversations = conversationsResponse?.data || [];
        
        // Find if there's an existing conversation for this session
        const existingConversation = conversations.find(conv => 
          (conv.sessionId === session.sessionId || conv.sessionId === session.id) ||
          (conv.studentId === user.userId && conv.tutorId === session.tutorId)
        );
        
        if (existingConversation) {
          conversationId = existingConversation.conversationId || existingConversation.id;
          console.log(`Found existing conversation: ${conversationId}`);
        }
      } catch (err) {
        console.log('Error finding existing conversations, will create new one:', err);
      }
      
      // If no existing conversation found, create a new one
      if (!conversationId) {
        try {
          const response = await conversationApi.createConversation(conversationData);
          if (response?.data) {
            conversationId = response.data.conversationId || response.data.id;
            console.log(`Created new conversation: ${conversationId}`);
          } else {
            throw new Error('No conversation ID returned from API');
          }
        } catch (err) {
          console.error('Error creating conversation:', err);
          
          // Try using findBetweenUsers as a fallback
          try {
            console.log('Trying findBetweenUsers as fallback');
            const api = await import('../../api/api').then(module => module.api);
            const fallbackResponse = await api.get(`/conversations/findBetweenUsers/${user.userId}/${session.tutorId}`);
            
            if (fallbackResponse?.data) {
              conversationId = fallbackResponse.data.conversationId || fallbackResponse.data.id;
              console.log(`Found conversation via fallback: ${conversationId}`);
            } else {
              throw new Error('Failed to get conversation from fallback method');
            }
          } catch (fallbackErr) {
            console.error('Fallback method also failed:', fallbackErr);
            toast.update(loadingToastId, { render: "Failed to create conversation", type: "error", isLoading: false, autoClose: 3000 });
            setProcessingConversation(false);
            return;
          }
        }
      }
      
      // Close loading toast
      toast.update(loadingToastId, { render: "Opening conversation...", type: "success", isLoading: false, autoClose: 1500 });
      
      // Navigate to the conversation
      if (conversationId) {
        navigate(`/messages/${conversationId}`);
      } else {
        toast.error('Could not establish a conversation with the tutor');
      }
    } catch (error) {
      console.error('Error handling session click:', error);
      toast.error('There was a problem accessing the conversation');
    } finally {
      setProcessingConversation(false);
    }
  };

  // Function to redirect from session detail to conversation
  const redirectToSessionConversation = async (sessionId) => {
    if (!sessionId || processingConversation) return;
    
    setProcessingConversation(true);
    
    // Display loading toast
    const loadingToastId = toast.loading("Opening conversation...");
    
    try {
      // Get user from localStorage
      const userString = localStorage.getItem('user');
      if (!userString) {
        toast.update(loadingToastId, { render: "User information not found, please login again", type: "error", isLoading: false, autoClose: 3000 });
        setProcessingConversation(false);
        return;
      }
      
      const user = JSON.parse(userString);
      
      // Fetch sessions first to get the session
      await fetchSessions();
      
      // Find the session with matching ID
      const targetSession = sessions.find(s => 
        s.sessionId === sessionId || s.id === sessionId || 
        s.sessionId === parseInt(sessionId) || s.id === parseInt(sessionId)
      );
      
      if (!targetSession) {
        toast.update(loadingToastId, { render: "Session not found", type: "error", isLoading: false, autoClose: 3000 });
        setProcessingConversation(false);
        navigate('/student/sessions');
        return;
      }
      
      // Continue with conversation logic using the existing function
      // This prevents code duplication - we reuse handleSessionClick implementation
      await handleSessionClick(targetSession);
    } catch (error) {
      console.error('Error redirecting to session conversation:', error);
      toast.error('There was a problem accessing the conversation');
      navigate('/student/sessions');
    } finally {
      setProcessingConversation(false);
    }
  };

  // Function to handle redirection to payment page
  const handlePayment = (session) => {
    navigate(`/student/sessions/${session.sessionId || session.id}`);
  };

  // Get status representation with icons
  const getStatusWithIcon = (status) => {
    switch(status) {
      case SESSION_STATUS.SCHEDULED:
        return <><FaCalendarAlt className="mr-1" /> {status}</>;
      case SESSION_STATUS.CONFIRMED:
        return <><FaCheck className="mr-1" /> {status}</>;
      case SESSION_STATUS.COMPLETED:
        return <><FaCheckDouble className="mr-1" /> {status}</>;
      case SESSION_STATUS.CANCELLED:
        return <><FaTimes className="mr-1" /> {status}</>;
      case SESSION_STATUS.PENDING:
        return <><FaClock className="mr-1" /> {status}</>;
      case SESSION_STATUS.APPROVED:
        return <><FaThumbsUp className="mr-1" /> {status}</>;
      default:
        return status;
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

      {processingConversation ? (
        <div className="flex flex-col justify-center items-center h-64">
          <div className="w-12 h-12 border-t-4 border-primary-600 border-solid rounded-full animate-spin mb-4"></div>
          <p className="text-gray-600 dark:text-gray-300">Opening conversation...</p>
        </div>
      ) : loadingSessions ? (
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
                  <div 
                    key={session.sessionId} 
                    className="p-6 hover:bg-gray-50 dark:hover:bg-dark-700 transition duration-150 cursor-pointer"
                    onClick={() => handleSessionClick(session)}
                  >
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
                              {getStatusWithIcon(session.status)}
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
                        
                        <div className="mt-6 flex flex-wrap gap-2">
                          <Link 
                            to={`/student/sessions/${session.sessionId || session.id}`}
                            className="inline-flex items-center px-3 py-1.5 border border-gray-300 dark:border-dark-600 text-xs font-medium rounded-md text-gray-700 dark:text-gray-300 bg-white dark:bg-dark-700 hover:bg-gray-50 dark:hover:bg-dark-600 focus:outline-none"
                          >
                            <FaSearch className="mr-1.5" />
                            View Details
                          </Link>
                          
                          {/* Show payment button for approved sessions */}
                          {(session.status === 'APPROVED' || (session.tutorAccepted && session.status !== 'COMPLETED')) && (
                            <button
                              onClick={() => handlePayment(session)}
                              className="inline-flex items-center px-3 py-1.5 border border-green-600 text-xs font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none"
                            >
                              <FaCreditCard className="mr-1.5" />
                              Pay Now
                            </button>
                          )}
                          
                          <button
                            onClick={() => handleSessionClick(session)}
                            className="inline-flex items-center px-3 py-1.5 border border-primary-600 dark:border-primary-500 text-xs font-medium rounded-md text-primary-600 dark:text-primary-500 bg-white dark:bg-dark-700 hover:bg-primary-50 dark:hover:bg-primary-900/10 focus:outline-none"
                          >
                            <FaComment className="mr-1.5" />
                            Message
                          </button>
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