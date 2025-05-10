import { useState, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Navbar from './Navbar';
import { useUser } from '../../context/UserContext';

const GlobalLayout = () => {
  const location = useLocation();
  const { loading } = useUser();
  const [userType, setUserType] = useState('student'); // Default to student

  // Pages that should not have the navbar
  const excludedPaths = [
    '/login',
    '/auth/login',
    '/register',
    '/auth/register',
    '/forgot-password',
    '/auth/forgot-password',
    '/oauth2-register',
    '/auth/oauth2-callback',
    '/oauth2-callback'
  ];

  // Check if current path should have navbar
  const shouldShowNavbar = !excludedPaths.includes(location.pathname);

  // Determine if the user is a tutor or student based on the URL
  useEffect(() => {
    if (location.pathname.includes('/tutor')) {
      setUserType('tutor');
    } else if (location.pathname.includes('/student')) {
      setUserType('student');
    }
  }, [location]);

  return (
    <div className="flex flex-col h-screen bg-light-900 dark:bg-dark-900 text-gray-900 dark:text-white">
      {shouldShowNavbar && <Navbar userType={userType} />}
      <main className="flex-1 overflow-y-auto bg-light-900 dark:bg-dark-900">
        {loading ? (
          <div className="flex items-center justify-center h-full">
            <div className="w-16 h-16 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
          </div>
        ) : (
          <Outlet />
        )}
      </main>
    </div>
  );
};

export default GlobalLayout;
