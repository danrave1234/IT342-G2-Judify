import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { SESSION_STATUS } from '../../types';

const SessionDetails = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { getSessionById, updateSessionStatus, loading } = useSession();
  const [session, setSession] = useState(null);
  const [updating, setUpdating] = useState(false);
  const [meetingLink, setMeetingLink] = useState('');
  const [error, setError] = useState('');
  const [showMeetingLinkForm, setShowMeetingLinkForm] = useState(false);

  useEffect(() => {
    fetchSessionDetails();
  }, [sessionId]);

  const fetchSessionDetails = async () => {
    console.log('Fetching session details for ID:', sessionId);
    const result = await getSessionById(sessionId);
    if (result.success) {
      // Normalize the session data to ensure it has both id and sessionId
      const normalizedSession = {
        ...result.session,
        id: result.session.id || result.session.sessionId,
        sessionId: result.session.sessionId || result.session.id
      };
      console.log('Session details:', normalizedSession);
      setSession(normalizedSession);
      if (normalizedSession.meetingLink) {
        setMeetingLink(normalizedSession.meetingLink);
      }
    } else {
      console.error('Failed to fetch session details:', result.message);
      // Handle error or redirect
      navigate('/tutor/sessions');
    }
  };

  const handleStatusUpdate = async (newStatus) => {
    if (window.confirm(`Are you sure you want to mark this session as ${newStatus}?`)) {
      setUpdating(true);
      try {
        const result = await updateSessionStatus(sessionId, newStatus);
        if (result.success) {
          setSession(result.session);
        } else {
          setError(result.message || 'Failed to update session status');
        }
      } catch {
        setError('An error occurred while updating the session');
      } finally {
        setUpdating(false);
      }
    }
  };

  const handleMeetingLinkChange = (e) => {
    setMeetingLink(e.target.value);
  };

  const handleMeetingLinkSubmit = async (e) => {
    e.preventDefault();
    setUpdating(true);

    try {
      // This would call an API to update the session's meeting link
      const result = await updateSessionStatus(sessionId, session.status, { meetingLink });
      if (result.success) {
        setSession(result.session);
        setShowMeetingLinkForm(false);
      } else {
        setError(result.message || 'Failed to update meeting link');
      }
    } catch {
      setError('An error occurred while updating the meeting link');
    } finally {
      setUpdating(false);
    }
  };

  if (loading || !session) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  const isUpcoming = session.status === SESSION_STATUS.SCHEDULED;
  const isCompleted = session.status === SESSION_STATUS.COMPLETED;

  // Safely create date objects with validation
  let sessionDate, endTime, durationHours;

  try {
    // Check if startTime and endTime are valid
    if (!session.startTime) {
      console.warn('Session startTime is missing or invalid');
      sessionDate = new Date(); // Fallback to current date
    } else {
      sessionDate = new Date(session.startTime);
      if (isNaN(sessionDate.getTime())) {
        console.warn('Invalid startTime format:', session.startTime);
        sessionDate = new Date(); // Fallback to current date
      }
    }

    if (!session.endTime) {
      console.warn('Session endTime is missing or invalid');
      // Default to startTime + 1 hour if available, otherwise current time + 1 hour
      endTime = new Date(sessionDate.getTime() + 60 * 60 * 1000);
    } else {
      endTime = new Date(session.endTime);
      if (isNaN(endTime.getTime())) {
        console.warn('Invalid endTime format:', session.endTime);
        endTime = new Date(sessionDate.getTime() + 60 * 60 * 1000);
      }
    }

    // Calculate duration in hours
    durationHours = (endTime - sessionDate) / (1000 * 60 * 60);
    // Ensure duration is positive and reasonable
    if (durationHours <= 0 || durationHours > 24) {
      console.warn('Unusual session duration:', durationHours);
      durationHours = 1; // Default to 1 hour
    }
  } catch (error) {
    console.error('Error processing session dates:', error);
    // Fallback values
    sessionDate = new Date();
    endTime = new Date(sessionDate.getTime() + 60 * 60 * 1000);
    durationHours = 1;
  }

  const isOnline = Boolean(session.meetingLink) || !session.locationData;

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-6">
        <Link to="/tutor/sessions" className="text-primary-600 dark:text-primary-500 flex items-center">
          ← Back to sessions
        </Link>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 dark:bg-red-900/20 dark:border-red-800 dark:text-red-400 px-4 py-3 rounded mb-6">
          {error}
        </div>
      )}

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700 mb-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            {session.subject} Session
          </h1>
          <div className="mt-2 sm:mt-0">
            <span className={`inline-flex px-3 py-1 rounded-full text-sm font-medium
              ${session.status === SESSION_STATUS.SCHEDULED ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-300' :
                session.status === SESSION_STATUS.COMPLETED ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300' :
                session.status === SESSION_STATUS.CANCELLED ? 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300' :
                'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-300'
              }`}
            >
              {session.status}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Session Details</h2>
            <div className="space-y-3">
              <div>
                <span className="text-gray-500 dark:text-gray-400">Date:</span>
                <p className="text-gray-900 dark:text-white">
                  {sessionDate.toLocaleDateString('en-US', { weekday: 'short', year: 'numeric', month: 'long', day: 'numeric' })}
                </p>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Time:</span>
                <p className="text-gray-900 dark:text-white">
                  {sessionDate.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })} - 
                  {endTime.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}
                </p>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Duration:</span>
                <p className="text-gray-900 dark:text-white">{durationHours} hours</p>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Price:</span>
                <p className="text-gray-900 dark:text-white">${session.price}</p>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Session Type:</span>
                <p className="text-gray-900 dark:text-white">{isOnline ? 'Online' : 'In-person'}</p>
              </div>
              {session.locationData && (
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Location:</span>
                  <p className="text-gray-900 dark:text-white">{session.locationData}</p>
                </div>
              )}
              {session.meetingLink && (
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Meeting Link:</span>
                  <p className="text-gray-900 dark:text-white">
                    <a 
                      href={session.meetingLink} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-primary-600 dark:text-primary-500 hover:underline"
                    >
                      {session.meetingLink}
                    </a>
                  </p>
                </div>
              )}
            </div>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Student Information</h2>
            <div className="flex items-center mb-4">
              <div className="h-16 w-16 rounded-full bg-gray-300 dark:bg-dark-600 flex items-center justify-center text-gray-600 dark:text-gray-400 mr-4">
                {session.studentName?.[0] || 'S'}
              </div>
              <div>
                <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                  {session.studentName || 'Student'}
                </h3>
                <p className="text-gray-600 dark:text-gray-400">
                  {session.studentEmail || 'No email provided'}
                </p>
              </div>
            </div>

            {session.notes && (
              <div>
                <h3 className="text-md font-semibold text-gray-900 dark:text-white mb-2">Session Notes:</h3>
                <p className="text-gray-700 dark:text-gray-300 bg-gray-50 dark:bg-dark-700 p-3 rounded-lg">
                  {session.notes}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Meeting Link Management - Only show for online sessions that are upcoming */}
        {isOnline && isUpcoming && (
          <div className="mt-8 border-t border-light-700 dark:border-dark-700 pt-6">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">Meeting Link</h3>

            {showMeetingLinkForm ? (
              <form onSubmit={handleMeetingLinkSubmit} className="mb-4">
                <div className="flex flex-col sm:flex-row gap-2">
                  <input
                    type="url"
                    value={meetingLink}
                    onChange={handleMeetingLinkChange}
                    placeholder="https://zoom.us/j/123456789"
                    className="input flex-grow"
                    required
                  />
                  <div className="flex gap-2">
                    <button
                      type="submit"
                      disabled={updating}
                      className="btn-primary whitespace-nowrap"
                    >
                      {updating ? 'Saving...' : 'Save Link'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowMeetingLinkForm(false)}
                      className="bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-dark-700 dark:text-gray-300 dark:hover:bg-dark-600 px-4 py-2 rounded-lg transition-colors whitespace-nowrap"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              </form>
            ) : (
              <div className="mb-4">
                {session.meetingLink ? (
                  <div className="flex flex-col sm:flex-row sm:items-center gap-2 justify-between">
                    <a 
                      href={session.meetingLink} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-primary-600 dark:text-primary-500 hover:underline"
                    >
                      {session.meetingLink}
                    </a>
                    <button
                      onClick={() => setShowMeetingLinkForm(true)}
                      className="bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-dark-700 dark:text-gray-300 dark:hover:bg-dark-600 px-4 py-2 rounded-lg transition-colors whitespace-nowrap sm:ml-2"
                    >
                      Edit Link
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => setShowMeetingLinkForm(true)}
                    className="btn-primary"
                  >
                    Add Meeting Link
                  </button>
                )}
              </div>
            )}

            <p className="text-sm text-gray-600 dark:text-gray-400">
              Add a Zoom, Google Meet, or other video conferencing link for your online session.
            </p>
          </div>
        )}

        <div className="mt-8 flex flex-wrap gap-3">
          {isUpcoming && (
            <>
              <button
                onClick={() => handleStatusUpdate(SESSION_STATUS.COMPLETED)}
                disabled={updating}
                className="bg-green-100 text-green-700 hover:bg-green-200 dark:bg-green-900/20 dark:text-green-400 dark:hover:bg-green-900/30 px-4 py-2 rounded-lg transition-colors"
              >
                Mark as Completed
              </button>

              <button
                onClick={() => handleStatusUpdate(SESSION_STATUS.CANCELLED)}
                disabled={updating}
                className="bg-red-100 text-red-700 hover:bg-red-200 dark:bg-red-900/20 dark:text-red-400 dark:hover:bg-red-900/30 px-4 py-2 rounded-lg transition-colors"
              >
                Cancel Session
              </button>

              {session.meetingLink && (
                <a
                  href={session.meetingLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn-primary"
                >
                  Join Meeting
                </a>
              )}
            </>
          )}

          <Link
            to={`/tutor/messages`}
            state={{ studentId: session.studentId }}
            className="bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-dark-700 dark:text-gray-300 dark:hover:bg-dark-600 px-4 py-2 rounded-lg transition-colors"
          >
            Message Student
          </Link>
        </div>
      </div>

      {/* Earnings Section */}
      {(isCompleted || isUpcoming) && (
        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Earnings</h2>

          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-gray-700 dark:text-gray-300">Session Price:</span>
              <span className="text-gray-900 dark:text-white font-medium">${session.price}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-700 dark:text-gray-300">Platform Fee (15%):</span>
              <span className="text-gray-900 dark:text-white font-medium">-${(session.price * 0.15).toFixed(2)}</span>
            </div>
            <div className="border-t border-light-700 dark:border-dark-700 pt-2 mt-2">
              <div className="flex justify-between items-center">
                <span className="text-gray-900 dark:text-white font-semibold">Your Earnings:</span>
                <span className="text-primary-600 dark:text-primary-500 font-bold">${(session.price * 0.85).toFixed(2)}</span>
              </div>
            </div>
          </div>

          <div className="mt-4">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {isCompleted
                ? 'Payment will be processed within 48 hours after session completion.'
                : 'You will receive payment after the session is completed.'}
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default SessionDetails; 
