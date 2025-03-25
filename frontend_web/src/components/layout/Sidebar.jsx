import { Link, useLocation } from 'react-router-dom';
import { FaHome, FaUser, FaCalendarAlt, FaChartLine, FaSearch, FaComments } from 'react-icons/fa';

const Sidebar = ({ userType }) => {
  const location = useLocation();

  const isActive = (path) => {
    return location.pathname === path;
  };

  return (
    <div className="h-full flex flex-col border-r border-gray-200 dark:border-dark-700 bg-white dark:bg-dark-800">
      <div className="p-4 border-b border-gray-200 dark:border-dark-700">
        <Link to="/" className="flex items-center justify-center">
          <span className="text-primary-600 dark:text-primary-500 text-2xl font-bold">Judify</span>
        </Link>
      </div>
      <nav className="flex-1 p-4 space-y-1">
        {userType === 'tutor' ? (
          <>
            <Link
              to="/tutor"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/tutor')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaHome className="text-xl" />
              <span>Dashboard</span>
            </Link>
            <Link
              to="/tutor/profile"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/tutor/profile')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaUser className="text-xl" />
              <span>Profile</span>
            </Link>
            <Link
              to="/tutor/sessions"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/tutor/sessions')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaCalendarAlt className="text-xl" />
              <span>Sessions</span>
            </Link>
            <Link
              to="/tutor/earnings"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/tutor/earnings')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaChartLine className="text-xl" />
              <span>Earnings</span>
            </Link>
            <Link
              to="/tutor/messages"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/tutor/messages')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaComments className="text-xl" />
              <span>Messages</span>
            </Link>
          </>
        ) : (
          <>
            <Link
              to="/student"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/student')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaHome className="text-xl" />
              <span>Dashboard</span>
            </Link>
            <Link
              to="/student/profile"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/student/profile')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaUser className="text-xl" />
              <span>Profile</span>
            </Link>
            <Link
              to="/student/find-tutors"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/student/find-tutors')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaSearch className="text-xl" />
              <span>Find Tutors</span>
            </Link>
            <Link
              to="/student/sessions"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/student/sessions')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaCalendarAlt className="text-xl" />
              <span>My Sessions</span>
            </Link>
            <Link
              to="/student/messages"
              className={`flex items-center space-x-3 p-3 rounded-lg transition-colors ${
                isActive('/student/messages')
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700'
              }`}
            >
              <FaComments className="text-xl" />
              <span>Messages</span>
            </Link>
          </>
        )}
      </nav>
    </div>
  );
};

export default Sidebar; 