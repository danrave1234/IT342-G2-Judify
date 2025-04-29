import { Link, useLocation } from 'react-router-dom';
import { 
  FaHome, FaCalendarAlt, FaMoneyBillWave, FaChartLine, 
  FaComments, FaUserCircle, FaCog 
} from 'react-icons/fa';

const TutorNavigation = () => {
  const location = useLocation();
  
  // Check if the current path matches the nav item path
  const isActivePath = (path) => {
    return location.pathname.startsWith(path);
  };
  
  // Navigation items with icons and paths
  const navItems = [
    { icon: <FaHome />, text: 'Dashboard', path: '/tutor/dashboard' },
    { icon: <FaCalendarAlt />, text: 'Sessions', path: '/tutor/sessions' },
    { icon: <FaComments />, text: 'Messages', path: '/tutor/messages' },
    { icon: <FaMoneyBillWave />, text: 'Payments', path: '/tutor/payments' },
    { icon: <FaChartLine />, text: 'Earnings', path: '/tutor/earnings' },
    { icon: <FaUserCircle />, text: 'Profile', path: '/tutor/profile' },
    { icon: <FaCog />, text: 'Settings', path: '/tutor/settings' }
  ];
  
  return (
    <div className="bg-white dark:bg-dark-800 shadow-sm border-b border-gray-200 dark:border-dark-700 mb-6">
      <div className="max-w-7xl mx-auto">
        <nav className="flex items-center justify-between overflow-x-auto">
          {navItems.map((item, index) => (
            <Link
              key={index}
              to={item.path}
              className={`flex flex-col items-center py-4 px-6 text-sm font-medium transition-colors
                ${isActivePath(item.path) 
                  ? 'text-primary-600 border-b-2 border-primary-600 dark:text-primary-500 dark:border-primary-500' 
                  : 'text-gray-500 hover:text-primary-600 dark:text-gray-400 dark:hover:text-primary-500'
                }`}
            >
              <span className="text-lg mb-1">{item.icon}</span>
              <span>{item.text}</span>
            </Link>
          ))}
        </nav>
      </div>
    </div>
  );
};

export default TutorNavigation; 