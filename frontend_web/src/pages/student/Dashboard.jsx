import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { FaCalendarAlt, FaClock, FaUser } from 'react-icons/fa';
import { SESSION_STATUS } from '../../types';

const Dashboard = () => {
  const { getStudentSessions, loading } = useSession();
  const [sessions, setSessions] = useState([]);
  const [loadingSessions, setLoadingSessions] = useState(true);

  useEffect(() => {
    fetchSessions();
  }, []);

  const fetchSessions = async () => {
    setLoadingSessions(true);
    const result = await getStudentSessions();
    if (result.success) {
      setSessions(result.sessions);
    }
    setLoadingSessions(false);
  };

  const upcomingSessions = sessions.filter(session => 
    session.status === SESSION_STATUS.SCHEDULED || 
    session.status === SESSION_STATUS.CONFIRMED
  ).slice(0, 3); // Only show the first 3 upcoming sessions

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Student Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Upcoming Sessions Card */}
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Upcoming Sessions</h2>
          
          {loadingSessions ? (
            <div className="flex justify-center items-center h-40">
              <div className="w-10 h-10 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
            </div>
          ) : upcomingSessions.length > 0 ? (
            <div className="space-y-4">
              {upcomingSessions.map(session => {
                const startTime = new Date(session.startTime);
                return (
                  <div key={session.sessionId} className="border-b border-gray-200 dark:border-dark-700 pb-3 last:border-0">
                    <div className="flex items-center mb-2">
                      <div className="w-10 h-10 bg-primary-100 dark:bg-primary-900/20 rounded-full flex items-center justify-center mr-3">
                        <FaUser className="text-primary-600 dark:text-primary-400" />
                      </div>
                      <div>
                        <h3 className="font-medium text-gray-900 dark:text-white">{session.tutorName}</h3>
                        <p className="text-sm text-gray-600 dark:text-gray-400">{session.subject}</p>
                      </div>
                    </div>
                    <div className="flex items-center text-sm text-gray-600 dark:text-gray-400 mb-1">
                      <FaCalendarAlt className="mr-2" />
                      {startTime.toLocaleDateString('en-US', { 
                        weekday: 'short', 
                        month: 'short', 
                        day: 'numeric' 
                      })}
                    </div>
                    <div className="flex items-center text-sm text-gray-600 dark:text-gray-400 mb-2">
                      <FaClock className="mr-2" />
                      {startTime.toLocaleTimeString('en-US', { 
                        hour: '2-digit', 
                        minute: '2-digit' 
                      })}
                    </div>
                    <Link 
                      to={`/student/sessions/${session.sessionId}`}
                      className="text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 text-sm font-medium"
                    >
                      View Details →
                    </Link>
                  </div>
                );
              })}
              <div className="pt-2">
                <Link 
                  to="/student/sessions" 
                  className="text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 text-sm font-medium"
                >
                  View All Sessions →
                </Link>
              </div>
            </div>
          ) : (
            <>
              <p className="text-gray-600 dark:text-gray-400 mb-4">You have no upcoming tutoring sessions.</p>
              <Link 
                to="/student/find-tutors" 
                className="text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 font-medium inline-block"
              >
                Find a tutor →
              </Link>
            </>
          )}
        </div>
        
        {/* Quick Actions Card */}
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Quick Actions</h2>
          <div className="space-y-3">
            <Link 
              to="/student/find-tutors" 
              className="block w-full py-2 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded transition-colors text-center"
            >
              Find Tutors
            </Link>
            <Link 
              to="/student/sessions" 
              className="block w-full py-2 px-4 bg-gray-200 hover:bg-gray-300 dark:bg-dark-700 dark:hover:bg-dark-600 text-gray-800 dark:text-gray-200 font-medium rounded transition-colors text-center"
            >
              Manage Sessions
            </Link>
            <Link 
              to="/student/messages" 
              className="block w-full py-2 px-4 bg-gray-200 hover:bg-gray-300 dark:bg-dark-700 dark:hover:bg-dark-600 text-gray-800 dark:text-gray-200 font-medium rounded transition-colors text-center"
            >
              Messages
            </Link>
          </div>
        </div>
        
        {/* Account Status Card */}
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700">
          <h2 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">Account Status</h2>
          <div className="space-y-3">
            <div className="bg-green-100 dark:bg-green-900/20 p-3 rounded-lg">
              <p className="text-green-800 dark:text-green-400 font-medium">Your account is active</p>
            </div>
            <p className="text-gray-600 dark:text-gray-400">Complete your profile to get better tutor matches.</p>
            <Link 
              to="/student/profile" 
              className="text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 font-medium mt-2 inline-block"
            >
              Edit profile →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 