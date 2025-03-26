import { Outlet, Navigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import DarkModeToggle from './DarkModeToggle';

// Auth layout with a centered form design
const AuthLayout = () => {
  const { user, loading } = useUser();

  // If still loading, show loading spinner
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-light-900 dark:bg-dark-900">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
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
    <div className="min-h-screen bg-light-900 dark:bg-dark-900 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="absolute top-4 right-4">
        <DarkModeToggle />
      </div>
      <div className="max-w-md w-full bg-white dark:bg-dark-800 rounded-xl shadow-card p-8 space-y-6 border border-light-700 dark:border-dark-700">
        <Outlet />
      </div>
    </div>
  );
};

export default AuthLayout; 