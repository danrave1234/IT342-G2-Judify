import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { FaUser, FaBell, FaSignOutAlt, FaMoneyBill } from 'react-icons/fa';
import DarkModeToggle from './DarkModeToggle';

const Navbar = ({ userType }) => {
  const { user, logout } = useUser();
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showNotificationsMenu, setShowNotificationsMenu] = useState(false);

  const toggleProfileMenu = () => {
    setShowProfileMenu(!showProfileMenu);
    setShowNotificationsMenu(false);
  };

  const toggleNotificationsMenu = () => {
    setShowNotificationsMenu(!showNotificationsMenu);
    setShowProfileMenu(false);
  };

  const handleLogout = () => {
    logout();
  };

  // Get display name - prefer username, fallback to email or 'User'
  const displayName = user?.username || user?.email?.split('@')[0] || 'User';

  return (
    <nav className="bg-white dark:bg-dark-800 shadow-md border-b border-gray-200 dark:border-dark-700">
      <div className="max-w-full mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <Link to="/" className="flex-shrink-0 text-primary-600 dark:text-primary-500 text-2xl font-bold">
              Judify
            </Link>
            <div className="hidden md:ml-10 md:flex space-x-8">
              {userType === 'tutor' ? (
                <>
                  <Link to="/tutor" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Dashboard
                  </Link>
                  <Link to="/tutor/sessions" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Sessions
                  </Link>
                  <Link to="/tutor/payments" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Payments
                  </Link>
                  <Link to="/tutor/earnings" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Earnings
                  </Link>
                  <Link to="/tutor/messages" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Messages
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/student" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Dashboard
                  </Link>
                  <Link to="/student/find-tutors" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Find Tutors
                  </Link>
                  <Link to="/student/sessions" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    All Subjects
                  </Link>
                  <Link to="/student/payments" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Payments
                  </Link>
                  <Link to="/student/messages" className="text-gray-700 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 px-3 py-2">
                    Messages
                  </Link>
                </>
              )}
            </div>
          </div>
          <div className="flex items-center">
            <DarkModeToggle />
            <div className="ml-3 relative">
              <button
                onClick={toggleNotificationsMenu}
                className="p-2 text-gray-600 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-500 relative"
              >
                <FaBell size={20} />
                <span className="absolute top-0 right-0 bg-red-500 text-white rounded-full w-4 h-4 flex items-center justify-center text-xs">
                  5
                </span>
              </button>
              {showNotificationsMenu && (
                <div className="origin-top-right absolute right-0 mt-2 w-80 rounded-md shadow-lg py-1 bg-white dark:bg-dark-800 ring-1 ring-black ring-opacity-5 dark:ring-dark-600 z-50">
                  <div className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 border-b dark:border-dark-600">
                    <p className="font-semibold">Notifications</p>
                  </div>
                  <div className="max-h-64 overflow-y-auto">
                    {[1, 2, 3, 4, 5].map((item) => (
                      <div key={item} className="px-4 py-3 hover:bg-gray-100 dark:hover:bg-dark-700 border-b dark:border-dark-600">
                        <p className="text-sm font-medium text-gray-900 dark:text-white">New session request</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                          You received a new session request for next Tuesday.
                        </p>
                        <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">2 hours ago</p>
                      </div>
                    ))}
                  </div>
                  <Link
                    to={userType === 'tutor' ? "/tutor/notifications" : "/student/notifications"}
                    className="block px-4 py-2 text-sm text-center text-primary-600 dark:text-primary-500 hover:bg-gray-100 dark:hover:bg-dark-700"
                  >
                    View all notifications
                  </Link>
                </div>
              )}
            </div>
            <div className="ml-3 relative">
              <button
                onClick={toggleProfileMenu}
                className="flex items-center space-x-2 focus:outline-none"
              >
                <div className="w-10 h-10 rounded-full bg-gray-300 dark:bg-dark-600 flex items-center justify-center text-gray-700 dark:text-gray-300 overflow-hidden">
                  {user?.profileImage ? (
                    <img
                      src={user.profileImage}
                      alt="Profile"
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <FaUser />
                  )}
                </div>
              </button>
              {showProfileMenu && (
                <div className="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg py-1 bg-white dark:bg-dark-800 ring-1 ring-black ring-opacity-5 dark:ring-dark-600 z-50">
                  <div className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 border-b dark:border-dark-600">
                    <p className="font-semibold">{user?.firstName ? `${user.firstName} ${user.lastName || ''}` : displayName}</p>
                    <p className="text-gray-500 dark:text-gray-400">
                      @{displayName}
                    </p>
                    <p className="text-gray-500 dark:text-gray-400">{user?.email}</p>
                  </div>
                  <Link
                    to="/profile"
                    className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700"
                  >
                    Profile
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-dark-700"
                  >
                    <div className="flex items-center">
                      <FaSignOutAlt className="mr-2" />
                      Sign out
                    </div>
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar; 