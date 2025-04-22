import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { SESSION_STATUS } from '../../types';
import { FaVideo, FaMapMarkerAlt, FaClock, FaCalendarAlt, FaArrowLeft, FaUser, FaStar } from 'react-icons/fa';
import { toast } from 'react-toastify';

const SessionDetail = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { getSessionById, updateSessionStatus, loading } = useSession();
  const [session, setSession] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);

  useEffect(() => {
    fetchSessionData();
  }, [sessionId]);

  const fetchSessionData = async () => {
    if (!sessionId) return;
    
    const result = await getSessionById(sessionId);
    if (result.success) {
      setSession(result.session);
    } else {
      toast.error('Failed to load session details');
    }
  };

  const formatSessionTime = (startTime, endTime) => {
    if (!startTime || !endTime) return { date: 'N/A', time: 'N/A' };
    
    const start = new Date(startTime);
    const end = new Date(endTime);
    
    const date = start.toLocaleDateString('en-US', {
      weekday: 'long',
      month: 'long', 
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
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300';
    }
  };

  const handleCancelSession = async () => {
    setActionLoading(true);
    try {
      const result = await updateSessionStatus(sessionId, SESSION_STATUS.CANCELLED);
      if (result.success) {
        toast.success('Session cancelled successfully');
        setSession(result.session);
        setShowCancelModal(false);
      } else {
        toast.error(result.message || 'Failed to cancel session');
      }
    } catch (error) {
      toast.error('An error occurred. Please try again.');
      console.error('Error cancelling session:', error);
    } finally {
      setActionLoading(false);
    }
  };

  const handleLeaveReview = () => {
    navigate(`/student/review/session/${sessionId}`);
  };

  if (loading || !session) {
    return (
      <div className="flex justify-center items-center min-h-[70vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  const { date, time } = formatSessionTime(session.startTime, session.endTime);
  const canCancel = session.status === SESSION_STATUS.SCHEDULED;
  const canReview = session.status === SESSION_STATUS.COMPLETED && !session.hasReview;
  const isUpcoming = session.status === SESSION_STATUS.SCHEDULED || session.status === SESSION_STATUS.CONFIRMED;

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="mb-6">
        <Link to="/student/sessions" className="text-primary-600 dark:text-primary-500 flex items-center">
          <FaArrowLeft className="mr-2" /> Back to Sessions
        </Link>
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-6 border-b border-gray-200 dark:border-dark-700">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2 md:mb-0">
              Session Details
            </h1>
            <div>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusClass(session.status)}`}>
                {session.status}
              </span>
            </div>
          </div>
        </div>

        <div className="p-6">
          {/* Tutor Information */}
          <div className="mb-8">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Tutor</h2>
            <div className="flex items-center">
              <img 
                src={session.tutorProfilePicture || "https://via.placeholder.com/60"} 
                alt={`${session.tutorName}'s profile`} 
                className="w-14 h-14 rounded-full mr-4 object-cover"
              />
              <div>
                <h3 className="font-medium text-gray-900 dark:text-white text-lg">{session.tutorName}</h3>
                {session.tutorRating && (
                  <div className="flex items-center mt-1">
                    <FaStar className="text-yellow-500 mr-1" />
                    <span className="text-gray-600 dark:text-gray-400">{session.tutorRating}</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Session Details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Session Information</h2>
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Subject</p>
                  <p className="font-medium text-gray-900 dark:text-white">{session.subject}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Date</p>
                  <p className="font-medium text-gray-900 dark:text-white flex items-center">
                    <FaCalendarAlt className="mr-2 text-gray-500 dark:text-gray-400" />
                    {date}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Time</p>
                  <p className="font-medium text-gray-900 dark:text-white flex items-center">
                    <FaClock className="mr-2 text-gray-500 dark:text-gray-400" />
                    {time}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Location</p>
                  <p className="font-medium text-gray-900 dark:text-white flex items-center">
                    {session.isOnline ? (
                      <>
                        <FaVideo className="mr-2 text-gray-500 dark:text-gray-400" />
                        Online Session
                      </>
                    ) : (
                      <>
                        <FaMapMarkerAlt className="mr-2 text-gray-500 dark:text-gray-400" />
                        In-person ({session.locationData || 'No location specified'})
                      </>
                    )}
                  </p>
                </div>
              </div>
            </div>

            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Payment Details</h2>
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Price</p>
                  <p className="font-medium text-gray-900 dark:text-white text-xl">${session.price}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Payment Status</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    <span className="px-2 py-1 rounded bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300 text-xs">
                      {session.isPaid ? 'Paid' : 'Pending'}
                    </span>
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Duration</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {Math.round((new Date(session.endTime) - new Date(session.startTime)) / (1000 * 60 * 60))} hours
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Notes Section */}
          {session.notes && (
            <div className="mb-8">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">Session Notes</h2>
              <div className="bg-gray-50 dark:bg-dark-700 p-4 rounded-lg">
                <p className="text-gray-700 dark:text-gray-300 whitespace-pre-line">{session.notes}</p>
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex flex-wrap justify-end gap-3 border-t border-gray-200 dark:border-dark-700 pt-6">
            {session.isOnline && session.status === SESSION_STATUS.CONFIRMED && session.meetingLink && (
              <a
                href={session.meetingLink}
                target="_blank"
                rel="noopener noreferrer"
                className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded transition-colors"
              >
                Join Meeting
              </a>
            )}

            {canReview && (
              <button
                onClick={handleLeaveReview}
                className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded transition-colors"
              >
                Leave Review
              </button>
            )}

            {canCancel && (
              <button
                onClick={() => setShowCancelModal(true)}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded transition-colors"
              >
                Cancel Session
              </button>
            )}

            {!isUpcoming && (
              <button
                onClick={() => navigate('/student/book/' + session.tutorId)}
                className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded transition-colors"
              >
                Book Again
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Cancel Confirmation Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-dark-800 rounded-lg shadow-xl max-w-md w-full p-6">
            <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-4">Cancel Session</h3>
            <p className="text-gray-700 dark:text-gray-300 mb-6">
              Are you sure you want to cancel this session? This action cannot be undone.
            </p>
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => setShowCancelModal(false)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-800 dark:bg-dark-700 dark:hover:bg-dark-600 dark:text-gray-200 rounded transition-colors"
                disabled={actionLoading}
              >
                Keep Session
              </button>
              <button
                onClick={handleCancelSession}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded transition-colors"
                disabled={actionLoading}
              >
                {actionLoading ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Cancelling...
                  </span>
                ) : (
                  'Confirm Cancel'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SessionDetail; 