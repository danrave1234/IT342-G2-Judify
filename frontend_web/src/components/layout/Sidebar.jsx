import { NavLink } from 'react-router-dom';
import { 
  FaHome, 
  FaUserCircle, 
  FaCalendarAlt, 
  FaDollarSign, 
  FaUsers, 
  FaComments, 
  FaSearch, 
  FaClipboardList
} from 'react-icons/fa';

const Sidebar = ({ userType }) => {
  const tutorLinks = [
    { name: 'Dashboard', path: '/tutor', icon: <FaHome /> },
    { name: 'Profile', path: '/tutor/profile', icon: <FaUserCircle /> },
    { name: 'Sessions', path: '/tutor/sessions', icon: <FaCalendarAlt /> },
    { name: 'Students', path: '/tutor/students', icon: <FaUsers /> },
    { name: 'Messages', path: '/tutor/messages', icon: <FaComments /> },
    { name: 'Earnings', path: '/tutor/earnings', icon: <FaDollarSign /> },
  ];
  
  const studentLinks = [
    { name: 'Dashboard', path: '/student', icon: <FaHome /> },
    { name: 'Find Tutors', path: '/student/find-tutors', icon: <FaSearch /> },
    { name: 'My Sessions', path: '/student/sessions', icon: <FaCalendarAlt /> },
    { name: 'Messages', path: '/student/messages', icon: <FaComments /> },
    { name: 'Profile', path: '/student/profile', icon: <FaUserCircle /> },
    { name: 'Payments', path: '/student/payments', icon: <FaDollarSign /> },
  ];
  
  const links = userType === 'tutor' ? tutorLinks : studentLinks;

  return (
    <aside className="hidden md:block w-64 bg-white shadow-md h-screen sticky top-0">
      <div className="p-6">
        <h3 className="text-lg font-semibold text-gray-700">
          {userType === 'tutor' ? 'Tutor Portal' : 'Student Portal'}
        </h3>
      </div>
      <nav className="mt-2">
        <ul>
          {links.map((link) => (
            <li key={link.path}>
              <NavLink
                to={link.path}
                className={({ isActive }) =>
                  `flex items-center px-6 py-3 text-gray-700 hover:bg-blue-50 hover:text-blue-600 transition-colors ${
                    isActive ? 'bg-blue-50 text-blue-600 border-r-4 border-blue-600' : ''
                  }`
                }
              >
                <span className="mr-3 text-lg">{link.icon}</span>
                {link.name}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      {userType === 'tutor' && (
        <div className="absolute bottom-0 left-0 right-0 p-6 border-t">
          <div className="flex flex-col space-y-2">
            <h4 className="font-medium text-gray-700">Availability</h4>
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600">Status:</span>
              <div className="relative inline-block w-10 mr-2 align-middle select-none">
                <input 
                  type="checkbox" 
                  id="availability-toggle" 
                  className="toggle-checkbox absolute block w-6 h-6 rounded-full bg-white border-4 appearance-none cursor-pointer"
                />
                <label 
                  htmlFor="availability-toggle" 
                  className="toggle-label block overflow-hidden h-6 rounded-full bg-gray-300 cursor-pointer"
                ></label>
              </div>
              <span className="text-sm font-medium text-green-600">Available</span>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
};

export default Sidebar; 