import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { tutoringSessionApi } from '../../api/api';
import { Link } from 'react-router-dom';
import { FaCalendarAlt, FaChartLine, FaUserEdit, FaBookOpen, FaCheckCircle, FaExclamationTriangle, FaClock } from 'react-icons/fa';

const Dashboard = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { currentUser } = useAuth();

  useEffect(() => {
    const fetchTutorSessions = async () => {
      if (!currentUser || !currentUser.userId) {
        setLoading(false);
        return;
      }

      try {
        const response = await tutoringSessionApi.getTutorSessions(currentUser.userId);
        setSessions(response.data);
      } catch (err) {
        console.error('Error fetching tutor sessions:', err);
        setError('Failed to load your sessions. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchTutorSessions();
  }, [currentUser]);

  // Format date for display
  const formatDate = (dateString) => {
    const options = { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return new Date(dateString).toLocaleDateString(undefined, options);
  };

  // Get status icon based on session status
  const getStatusIcon = (status) => {
    switch(status) {
      case 'CONFIRMED':
        return <FaCheckCircle className="text-green-500" />;
      case 'PENDING':
        return <FaClock className="text-yellow-500" />;
      case 'CANCELLED':
        return <FaExclamationTriangle className="text-red-500" />;
      default:
        return null;
    }
  };

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Welcome Back, {currentUser?.firstName || 'Tutor'}</h1>
        <p className="mt-2 text-lg text-gray-600 dark:text-gray-400">Manage your tutoring sessions and monitor your progress</p>
      </div>
      
      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <Link 
          to="/tutor/availability" 
          className="bg-white dark:bg-dark-800 rounded-xl shadow-sm hover:shadow-md transition-shadow p-6 border border-gray-200 dark:border-dark-700 group"
        >
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400 group-hover:bg-blue-600 group-hover:text-white dark:group-hover:bg-blue-500 transition-colors">
              <FaCalendarAlt size={20} />
            </div>
            <div className="ml-4">
              <h3 className="font-semibold text-gray-900 dark:text-white">Availability</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">Set your schedule</p>
            </div>
          </div>
        </Link>
        
        <Link 
          to="/tutor/sessions" 
          className="bg-white dark:bg-dark-800 rounded-xl shadow-sm hover:shadow-md transition-shadow p-6 border border-gray-200 dark:border-dark-700 group"
        >
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600 dark:text-green-400 group-hover:bg-green-600 group-hover:text-white dark:group-hover:bg-green-500 transition-colors">
              <FaBookOpen size={20} />
            </div>
            <div className="ml-4">
              <h3 className="font-semibold text-gray-900 dark:text-white">Sessions</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">View all sessions</p>
            </div>
          </div>
        </Link>
        
        <Link 
          to="/tutor/payments" 
          className="bg-white dark:bg-dark-800 rounded-xl shadow-sm hover:shadow-md transition-shadow p-6 border border-gray-200 dark:border-dark-700 group"
        >
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center text-purple-600 dark:text-purple-400 group-hover:bg-purple-600 group-hover:text-white dark:group-hover:bg-purple-500 transition-colors">
              <FaChartLine size={20} />
            </div>
            <div className="ml-4">
              <h3 className="font-semibold text-gray-900 dark:text-white">Earnings</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">Track your income</p>
            </div>
          </div>
        </Link>
        
        <Link 
          to="/tutor/profile" 
          className="bg-white dark:bg-dark-800 rounded-xl shadow-sm hover:shadow-md transition-shadow p-6 border border-gray-200 dark:border-dark-700 group"
        >
          <div className="flex items-center">
            <div className="w-12 h-12 rounded-lg bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center text-amber-600 dark:text-amber-400 group-hover:bg-amber-600 group-hover:text-white dark:group-hover:bg-amber-500 transition-colors">
              <FaUserEdit size={20} />
            </div>
            <div className="ml-4">
              <h3 className="font-semibold text-gray-900 dark:text-white">Profile</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">Update your details</p>
            </div>
          </div>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Upcoming Sessions */}
        <div className="lg:col-span-2">
          <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
                <FaCalendarAlt className="mr-2 text-primary-600 dark:text-primary-400" /> 
                Upcoming Sessions
              </h2>
              <Link to="/tutor/sessions" className="text-sm text-primary-600 dark:text-primary-400 hover:text-primary-800 dark:hover:text-primary-300">
                View All
              </Link>
            </div>
            <div className="p-6">
          {loading ? (
                <div className="flex items-center justify-center h-40">
                  <div className="w-12 h-12 border-4 border-gray-200 border-t-primary-600 rounded-full animate-spin"></div>
                </div>
          ) : error ? (
                <div className="bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-400 p-4 rounded-lg">
                  <p>{error}</p>
                  <button 
                    onClick={() => window.location.reload()} 
                    className="mt-2 text-sm font-medium underline"
                  >
                    Try Again
                  </button>
                </div>
          ) : sessions.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-40 text-center">
                  <div className="w-16 h-16 bg-gray-100 dark:bg-dark-700 rounded-full flex items-center justify-center mb-4">
                    <FaCalendarAlt className="text-gray-400 dark:text-gray-500 text-xl" />
                  </div>
                  <p className="text-gray-500 dark:text-gray-400 mb-4">You have no upcoming sessions.</p>
                  <Link to="/tutor/availability" className="text-sm bg-primary-600 hover:bg-primary-700 text-white py-2 px-4 rounded-md transition-colors">
                    Set Your Availability
                  </Link>
                </div>
              ) : (
                <div className="space-y-5">
                  {sessions.slice(0, 3).map((session) => (
                    <Link 
                      to={`/tutor/sessions/${session.sessionId}`} 
                      key={session.sessionId} 
                      className="block p-4 border border-gray-100 dark:border-dark-700 rounded-lg hover:bg-gray-50 dark:hover:bg-dark-700 transition-colors"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <h3 className="font-medium text-gray-900 dark:text-white text-base">
                            {session.subject || 'Tutoring Session'}
                          </h3>
                          <div className="mt-1 flex items-center text-sm text-gray-500 dark:text-gray-400">
                            <span className="font-medium text-gray-900 dark:text-gray-300 mr-2">
                              Student:
                            </span>
                            {session.studentName || 'Anonymous Student'}
                          </div>
                          <div className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                            {formatDate(session.sessionDate)}
                          </div>
                        </div>
                        <div className="flex items-center">
                          {getStatusIcon(session.status)}
                          <span className={`ml-1.5 px-2.5 py-0.5 text-xs font-medium rounded-full ${
                            session.status === 'CONFIRMED' ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300' : 
                            session.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300' : 
                            'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300'
                          }`}>
                            {session.status}
                          </span>
                        </div>
                      </div>
                    </Link>
              ))}
            </div>
          )}
        </div>
          </div>
        </div>

        {/* Stats and Tips */}
        <div className="space-y-8">
          {/* Earnings Summary */}
          <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
                <FaChartLine className="mr-2 text-primary-600 dark:text-primary-400" /> 
                Earnings Overview
              </h2>
            </div>
            <div className="p-6 space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-green-50 dark:bg-green-900/20 p-4 rounded-lg">
                  <p className="text-sm text-green-700 dark:text-green-400 font-medium">This Month</p>
                  <p className="text-2xl font-bold text-green-800 dark:text-green-300 mt-1">$0.00</p>
                </div>
                <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
                  <p className="text-sm text-blue-700 dark:text-blue-400 font-medium">Total Earnings</p>
                  <p className="text-2xl font-bold text-blue-800 dark:text-blue-300 mt-1">$0.00</p>
                </div>
              </div>
              <div className="pt-2">
                <Link to="/tutor/payments" className="text-sm text-primary-600 dark:text-primary-400 hover:text-primary-800 dark:hover:text-primary-300 font-medium">
                  View detailed earnings →
                </Link>
              </div>
            </div>
          </div>

          {/* Profile Completion */}
          <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden">
            <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
                <FaUserEdit className="mr-2 text-primary-600 dark:text-primary-400" /> 
                Complete Your Profile
              </h2>
            </div>
            <div className="p-6">
              <div className="mb-4">
                <div className="flex justify-between mb-1">
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Profile completion</span>
                  <span className="text-sm font-medium text-primary-600 dark:text-primary-400">65%</span>
                </div>
                <div className="w-full bg-gray-200 dark:bg-dark-700 rounded-full h-2.5">
                  <div className="bg-primary-600 dark:bg-primary-500 h-2.5 rounded-full" style={{ width: '65%' }}></div>
                </div>
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                Complete your profile to attract more students and increase your booking potential.
              </p>
              <Link to="/tutor/profile" className="inline-block bg-primary-600 hover:bg-primary-700 text-white text-sm font-medium py-2 px-4 rounded-md transition-colors w-full text-center">
                Update Profile
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 
