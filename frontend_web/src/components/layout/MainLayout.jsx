import { useState, useEffect } from 'react';
import { Outlet, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import Navbar from './Navbar';

const MainLayout = () => {
  const { user, loading, isTutor, isStudent } = useUser();
  const navigate = useNavigate();
  const location = useLocation();
  const [userType, setUserType] = useState('student'); // Default to student

  useEffect(() => {
    // Check if user has the right role for this section
    if (user) {
      const hasCorrectRole = 
        (userType === 'tutor' && isTutor()) || 
        (userType === 'student' && isStudent());
      
      if (!hasCorrectRole) {
        if (isTutor()) {
          navigate('/tutor');
        } else if (isStudent()) {
          navigate('/student');
        } else {
          navigate('/');
        }
      }
    }
  }, [user, userType, isTutor, isStudent, navigate]);

  // Determine if the user is a tutor or student based on the URL
  useEffect(() => {
    if (location.pathname.includes('/tutor')) {
      setUserType('tutor');
    } else if (location.pathname.includes('/student')) {
      setUserType('student');
    }
  }, [location]);

  // If still loading, show loading spinner
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <div className="w-16 h-16 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  // If user is not logged in, redirect to login
  if (!user) {
    return <Navigate to="/login" />;
  }

  return (
    <div className="flex flex-col h-screen bg-light-900 dark:bg-dark-900 text-gray-900 dark:text-white">
      <Navbar userType={userType} />
      <main className="flex-1 overflow-y-auto p-4 bg-light-900 dark:bg-dark-900">
        <Outlet />
      </main>
    </div>
  );
};

export default MainLayout; 