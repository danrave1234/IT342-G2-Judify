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
  FaCreditCard,
  FaExclamationTriangle,
  FaTimes as FaClose
} from 'react-icons/fa';
import { SESSION_STATUS } from '../../types';
import axios from 'axios';
import { conversationApi, paymentApi } from '../../api/api';
import { toast } from 'react-toastify';
import { formatSessionTime as formatSessionTimeUtil } from '../../utils/dateUtils';
import StripeWrapper from '../../components/payment/StripeWrapper';
import SessionPayment from '../../components/payment/SessionPayment';

// CSS for animations
const styles = {
  '@keyframes fadeIn': {
    from: { opacity: 0, transform: 'translateY(10px)' },
    to: { opacity: 1, transform: 'translateY(0)' }
  },
  '.animate-fade-in': {
    animation: 'fadeIn 0.3s ease-out'
  }
};

// Add styles to document
const styleSheet = document.createElement('style');
styleSheet.type = 'text/css';
styleSheet.innerText = `
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-fade-in {
    animation: fadeIn 0.3s ease-out;
  }
`;
document.head.appendChild(styleSheet);

const Sessions = () => {
  const [sessions, setSessions] = useState([]);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('upcoming');
  const [searchTerm, setSearchTerm] = useState('');
  const [processingConversation, setProcessingConversation] = useState(false);
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [selectedSession, setSelectedSession] = useState(null);
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
        
        // Normalize the session data to handle different formats
        const normalizedSessions = (response.data || []).map(session => {
          // Create a normalized session object with camelCase keys
          const normalizedSession = {
            ...session,
            id: session.id || session.sessionId || session.session_id,
            sessionId: session.sessionId || session.session_id || session.id,
            tutorId: session.tutorId || session.tutor_id,
            studentId: session.studentId || session.student_id,
            startTime: session.startTime || session.start_time,
            endTime: session.endTime || session.end_time,
            tutorAccepted: session.tutorAccepted || session.tutor_accepted,
            studentAccepted: session.studentAccepted || session.student_accepted
          };
          
          // Handle status that might be a JSON string or object
          if (typeof session.status === 'string') {
            console.log('Session has string status:', session.status);
            try {
              // Check if it's a JSON string
              if (session.status.includes('{') && session.status.includes('}')) {
                console.log('Trying to parse status JSON string');
                // Parse the JSON status
                const statusObj = JSON.parse(session.status);
                console.log('Parsed status object:', statusObj);
                
                // Handle nested status and tutorAccepted properties
                if (statusObj.status) {
                  normalizedSession.status = statusObj.status;
                  // Uppercase status for consistency
                  if (typeof normalizedSession.status === 'string') {
                    normalizedSession.status = normalizedSession.status.toUpperCase();
                  }
                }
                
                // Overwrite tutorAccepted with the value from JSON status object
                if (statusObj.tutorAccepted !== undefined) {
                  normalizedSession.tutorAccepted = statusObj.tutorAccepted;
                  console.log('Setting tutorAccepted to:', normalizedSession.tutorAccepted);
                }
              }
            } catch (e) {
              console.log('Error parsing status JSON:', e);
            }
          } else if (typeof session.status === 'object' && session.status !== null) {
            // Handle status that's already an object
            if (session.status.status) {
              normalizedSession.status = session.status.status;
              // Uppercase status for consistency
              if (typeof normalizedSession.status === 'string') {
                normalizedSession.status = normalizedSession.status.toUpperCase();
              }
            }
            
            // Get tutorAccepted from the object
            if (session.status.tutorAccepted !== undefined) {
              normalizedSession.tutorAccepted = session.status.tutorAccepted;
            }
          }
          
          // Set a default status if none was found
          if (!normalizedSession.status) {
            normalizedSession.status = 'PENDING';
          }
          
          console.log('Normalized session:', normalizedSession);
          return normalizedSession;
        });
        
        console.log('Normalized sessions:', normalizedSessions);
        setSessions(normalizedSessions);
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

  // Function to show payment modal for a session
  const handlePayment = (session) => {
    console.log('Opening payment modal for session:', session);
    setSelectedSession(session);
    setPaymentModalOpen(true);
  };

  // Function to determine if a session is approved
  const isSessionApproved = (session) => {
    // Handle case where status is a string
    if (typeof session.status === 'string') {
      // Direct match with 'APPROVED'
      if (session.status === 'APPROVED') return true;
      
      // Case-insensitive match
      if (session.status.toUpperCase() === 'APPROVED') return true;
      
      // Try to parse JSON if it's a string that looks like JSON
      if (session.status.includes('{') && session.status.includes('}')) {
        try {
          const statusObj = JSON.parse(session.status);
          // Check if status property is 'APPROVED'
          if (statusObj.status && statusObj.status.toUpperCase() === 'APPROVED') return true;
          if (statusObj.STATUS && statusObj.STATUS === 'APPROVED') return true;
          // Check tutorAccepted flag
          if (statusObj.tutorAccepted === true) return true;
        } catch (e) {
          console.log('Error parsing status JSON in isSessionApproved:', e);
        }
      }
    }
    
    // Handle case where status is an object
    if (typeof session.status === 'object' && session.status !== null) {
      if (session.status.status && session.status.status.toUpperCase() === 'APPROVED') return true;
      if (session.status.STATUS === 'APPROVED') return true;
      if (session.status.tutorAccepted === true) return true;
    }
    
    // Finally, check the tutorAccepted property directly
    return session.tutorAccepted === true;
  };
  
  // Function to determine if a session is approved and needs payment
  const isApprovedAndNeedsPayment = (session) => {
    console.log(`Session ${session.id || session.sessionId} check:`, 
      "status:", session.status, 
      "tutorAccepted:", session.tutorAccepted, 
      "paymentStatus:", session.paymentStatus
    );

    // Check if session is approved
    const isApproved = isSessionApproved(session);

    // Check if payment is not yet completed
    const paymentNotComplete = 
      !session.paymentStatus || 
      (session.paymentStatus !== 'COMPLETED' && session.paymentStatus !== 'PAID');

    const result = isApproved && paymentNotComplete;
    console.log(`Session ${session.id || session.sessionId} needs payment: ${result}`);
    return result;
  };

  // Function to handle successful payment
  const handlePaymentSuccess = async (paymentIntent) => {
    toast.success('Payment successful! Your session is now confirmed.');
    setPaymentModalOpen(false);
    
    // Update the sessions list
    await fetchSessions();
  };
  
  // Function to handle payment error
  const handlePaymentError = (error) => {
    console.error('Payment error:', error);
    // We'll keep the modal open so they can try again
    toast.error('Payment failed: ' + (error.message || 'Please try again'));
  };

  // Get status representation with icons
  const getStatusWithIcon = (status) => {
    switch(status) {
      case SESSION_STATUS.SCHEDULED:
        return <><FaCalendarAlt className="mr-1" /> Scheduled</>;
      case SESSION_STATUS.CONFIRMED:
        return <><FaCheck className="mr-1" /> Confirmed</>;
      case SESSION_STATUS.COMPLETED:
        return <><FaCheckDouble className="mr-1" /> Completed</>;
      case SESSION_STATUS.CANCELLED:
        return <><FaTimes className="mr-1" /> Cancelled</>;
      case SESSION_STATUS.PENDING:
        return <><FaClock className="mr-1" /> Pending</>;
      case 'APPROVED':
        return <><FaThumbsUp className="mr-1" /> Approved</>;
      default:
        return status;
    }
  };
  
  const getPaymentStatusBadge = (session) => {
    if (session.paymentStatus === 'COMPLETED' || session.paymentStatus === 'PAID') {
      return (
        <span className="ml-2 px-2 py-0.5 bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300 text-xs font-medium rounded-full">
          Payment Successful
        </span>
      );
    } else if (session.paymentStatus === 'FAILED') {
      return (
        <span className="ml-2 px-2 py-0.5 bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300 text-xs font-medium rounded-full">
          Payment Failed
        </span>
      );
    }
    return null;
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
                
                // Force a check if this session should show payment button
                const needsPayment = isApprovedAndNeedsPayment(session);
                console.log(`Rendering session ${session.id}: needs payment = ${needsPayment}`);
                
                return (
                  <div 
                    key={session.sessionId} 
                    className="p-6 bg-white dark:bg-dark-800 border-b border-gray-200 dark:border-dark-700 last:border-b-0"
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
                            <div className="flex items-center mt-1">
                              <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${getStatusClass(session.status)}`}>
                                {getStatusWithIcon(session.status)}
                              </span>
                              {getPaymentStatusBadge(session)}
                            </div>
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
                      
                      {/* Actions column */}
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
                        
                        {/* Debug output to help identify issues */}
                        {process.env.NODE_ENV === 'development' && (
                          <div className="text-xs text-gray-500 mt-1 mb-2">
                            <div>Status: {typeof session.status === 'object' ? JSON.stringify(session.status) : session.status}</div>
                            <div>Tutor Accepted: {String(session.tutorAccepted)}</div>
                            <div>Payment Status: {session.paymentStatus || 'Not yet paid'}</div>
                            <div>Should Show Payment: {String(needsPayment)}</div>
                          </div>
                        )}
                        
                        <div className="mt-4 flex flex-wrap gap-2 justify-end">
                          {needsPayment ? (
                            // If session is approved, only show Pay Now button
                            <button
                              onClick={() => handlePayment(session)}
                              className="inline-flex items-center px-4 py-2 border border-green-600 text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none w-full sm:w-auto"
                            >
                              <FaCreditCard className="mr-2" />
                              Pay Now
                            </button>
                          ) : (
                            // Otherwise show normal action buttons
                            <>
                              <Link 
                                to={`/student/sessions/${session.sessionId || session.id}`}
                                className="inline-flex items-center px-3 py-1.5 border border-gray-300 dark:border-dark-600 text-xs font-medium rounded-md text-gray-700 dark:text-gray-300 bg-white dark:bg-dark-700 hover:bg-gray-50 dark:hover:bg-dark-600 focus:outline-none"
                              >
                                <FaSearch className="mr-1.5" />
                                View Details
                              </Link>
                              
                              <button
                                onClick={() => redirectToSessionConversation(session.sessionId || session.id)}
                                className="inline-flex items-center px-3 py-1.5 border border-primary-600 dark:border-primary-500 text-xs font-medium rounded-md text-primary-600 dark:text-primary-500 bg-white dark:bg-dark-700 hover:bg-primary-50 dark:hover:bg-primary-900/10 focus:outline-none"
                              >
                                <FaComment className="mr-1.5" />
                                Message
                              </button>
                            </>
                          )}
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
      
      {/* Payment Modal */}
      {paymentModalOpen && selectedSession && (
        <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center p-4 z-50">
          <div className="bg-white dark:bg-dark-800 rounded-xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto animate-fade-in">
            <div className="p-6 border-b border-gray-200 dark:border-dark-700 flex justify-between items-center bg-green-50 dark:bg-green-900/20">
              <div className="flex items-center">
                <FaCreditCard className="mr-3 text-green-600 dark:text-green-400 text-xl" />
                <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Complete Payment</h2>
              </div>
              <button 
                onClick={() => setPaymentModalOpen(false)}
                className="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 p-1 rounded-full hover:bg-gray-100 dark:hover:bg-dark-700"
              >
                <FaClose size={20} />
              </button>
            </div>
            
            <div className="p-6">
              <div className="mb-6 space-y-4 bg-gray-50 dark:bg-dark-700 p-4 rounded-lg">
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Session:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{selectedSession.subject}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Tutor:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{selectedSession.tutorName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Date:</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {formatSessionTime(selectedSession.startTime, selectedSession.endTime).date}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Time:</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {formatSessionTime(selectedSession.startTime, selectedSession.endTime).time}
                  </span>
                </div>
                <div className="flex justify-between border-t pt-4 border-gray-200 dark:border-dark-700">
                  <span className="text-gray-800 dark:text-gray-200 font-medium">Total:</span>
                  <span className="font-bold text-green-600 dark:text-green-400 text-lg">
                    ${selectedSession.price || 0}
                  </span>
                </div>
              </div>
              
              <StripeWrapper>
                <SessionPayment 
                  sessionId={selectedSession.sessionId || selectedSession.id}
                  amount={(selectedSession.price || 0) * 100} // Convert to cents for Stripe
                  onSuccess={handlePaymentSuccess}
                  onError={handlePaymentError}
                  sessionData={selectedSession}
                />
              </StripeWrapper>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Sessions; 