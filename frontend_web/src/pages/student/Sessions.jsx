import { useState, useEffect } from 'react';
import { useSession } from '../../context/SessionContext';
import { Link } from 'react-router-dom';
import { FaVideo, FaMapMarkerAlt, FaClock, FaCalendarAlt } from 'react-icons/fa';
import { SESSION_STATUS } from '../../types';

const Sessions = () => {
  const { getStudentSessions, loading, error } = useSession();
  const [sessions, setSessions] = useState([]);
  const [activeTab, setActiveTab] = useState('upcoming');

  useEffect(() => {
    fetchSessions();
  }, []);

  const fetchSessions = async () => {
    const result = await getStudentSessions();
    if (result.success) {
      setSessions(result.sessions);
    }
  };

  const upcomingSessions = sessions.filter(session => 
    session.status === SESSION_STATUS.SCHEDULED || 
    session.status === SESSION_STATUS.CONFIRMED
  );
  
  const pastSessions = sessions.filter(session => 
    session.status === SESSION_STATUS.COMPLETED || 
    session.status === SESSION_STATUS.CANCELLED
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
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300';
    }
  };

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-6">My Tutoring Sessions</h1>
      
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

      {loading ? (
        <div className="flex justify-center items-center h-64">
          <div className="w-12 h-12 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
        </div>
      ) : error ? (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          Failed to load sessions. Please try again later.
        </div>
      ) : (
        <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden">
          <div className="p-6 border-b border-gray-200 dark:border-dark-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              {activeTab === 'upcoming' ? 'Upcoming Sessions' : 'Past Sessions'}
            </h2>
            <p className="text-gray-500 dark:text-gray-400 mt-2">
              {activeTab === 'upcoming' 
                ? 'Sessions that are scheduled or confirmed'
                : 'Your completed and cancelled sessions'}
            </p>
          </div>
          
          {(activeTab === 'upcoming' ? upcomingSessions : pastSessions).length > 0 ? (
            <div className="divide-y divide-gray-200 dark:divide-dark-700">
              {(activeTab === 'upcoming' ? upcomingSessions : pastSessions).map(session => {
                const { date, time } = formatSessionTime(session.startTime, session.endTime);
                return (
                  <div key={session.sessionId} className="p-6 hover:bg-gray-50 dark:hover:bg-dark-700">
                    <div className="flex flex-col md:flex-row md:items-center md:justify-between">
                      <div className="mb-4 md:mb-0">
                        <div className="flex items-center mb-2">
                          <img 
                            src={session.tutorProfilePicture || "https://via.placeholder.com/40"} 
                            alt={`${session.tutorName}'s profile`} 
                            className="w-10 h-10 rounded-full mr-3 object-cover"
                          />
                          <div>
                            <h3 className="font-medium text-gray-900 dark:text-white">{session.tutorName}</h3>
                            <p className="text-sm text-gray-500 dark:text-gray-400">{session.subject}</p>
                          </div>
                        </div>
                        
                        <div className="space-y-1 text-sm">
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            <FaCalendarAlt className="mr-2" /> {date}
                          </p>
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            <FaClock className="mr-2" /> {time}
                          </p>
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            {session.isOnline ? (
                              <>
                                <FaVideo className="mr-2" /> Online Session
                              </>
                            ) : (
                              <>
                                <FaMapMarkerAlt className="mr-2" /> In-person
                              </>
                            )}
                          </p>
                        </div>
                      </div>
                      
                      <div className="flex flex-col items-end">
                        <div className="mb-2">
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusClass(session.status)}`}>
                            {session.status}
                          </span>
                        </div>
                        
                        <div className="text-lg font-bold text-gray-900 dark:text-white mb-2">
                          ${session.price}
                        </div>
                        
                        <div className="flex space-x-2">
                          <Link
                            to={`/student/sessions/${session.sessionId}`}
                            className="text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 text-sm font-medium"
                          >
                            View Details
                          </Link>
                          
                          {session.status === SESSION_STATUS.SCHEDULED && (
                            <button className="text-red-600 dark:text-red-500 hover:text-red-700 dark:hover:text-red-400 text-sm font-medium">
                              Cancel
                            </button>
                          )}
                          
                          {session.status === SESSION_STATUS.CONFIRMED && session.isOnline && (
                            <a
                              href={session.meetingLink}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-green-600 dark:text-green-500 hover:text-green-700 dark:hover:text-green-400 text-sm font-medium"
                            >
                              Join Meeting
                            </a>
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
                  ? 'No upcoming sessions found. Book a session with a tutor to get started!'
                  : 'No past sessions found.'}
              </p>
              {activeTab === 'upcoming' && (
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