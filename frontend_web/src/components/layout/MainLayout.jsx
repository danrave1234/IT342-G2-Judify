import { useEffect } from 'react';
import { Outlet, Navigate, useNavigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

const MainLayout = ({ userType }) => {
  const { user, loading, isTutor, isStudent } = useUser();
  const navigate = useNavigate();

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
    <div className="min-h-screen bg-gray-50">
      <Navbar userType={userType} />
      <div className="flex">
        <Sidebar userType={userType} />
        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default MainLayout; 