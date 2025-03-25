import { Outlet, Navigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';

// Auth layout with a centered form design
const AuthLayout = () => {
  const { user, loading } = useUser();

  // If still loading, show loading spinner
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <div className="w-16 h-16 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  // If user is logged in, redirect to dashboard
  if (user) {
    if (user.roles.includes('TUTOR')) {
      return <Navigate to="/tutor" />;
    }
    return <Navigate to="/student" />;
  }

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="max-w-md w-full p-8 bg-white rounded-lg shadow-lg">
        <Outlet />
      </div>
    </div>
  );
};

export default AuthLayout; 