import { Outlet, Navigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import DarkModeToggle from './DarkModeToggle';
import { useEffect, useState } from 'react';

// Auth layout with a centered form design
const AuthLayout = () => {
  const { user, loading, isTutor, isStudent } = useUser();
  const [redirectPath, setRedirectPath] = useState(null);

  // Check localStorage directly as a fallback for authentication
  useEffect(() => {
    const checkLocalStorageAuth = () => {
      try {
        // If we have a token and user in localStorage but no user in context
        // this could mean the context hasn't loaded yet but the user is authenticated
        const token = localStorage.getItem('token');
        const storedUser = localStorage.getItem('user');
        
        if (!token || !storedUser) {
          return;
        }
        
        try {
          const userData = JSON.parse(storedUser);
          console.log("AuthLayout: Detected user in localStorage:", userData);
          
          if (!userData || !userData.userId) {
            console.warn("AuthLayout: Invalid user data in localStorage");
            return;
          }
          
          // Determine where to redirect based on role
          const role = (userData.role || '').toUpperCase();
          if (role.includes('TUTOR')) {
            setRedirectPath('/tutor');
          } else if (role.includes('STUDENT')) {
            setRedirectPath('/student');
          } else {
            console.warn(`AuthLayout: Unknown role in localStorage: ${role}`);
          }
        } catch (parseErr) {
          console.error("AuthLayout: Error parsing user data:", parseErr);
        }
      } catch (err) {
        console.error("AuthLayout: Error checking localStorage:", err);
      }
    };
    
    // Only check localStorage if user context is not yet available
    if (!user && !loading) {
      checkLocalStorageAuth();
    }
  }, [user, loading]);

  // If still loading, show loading spinner
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-light-900 dark:bg-dark-900">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  // If we detected a user in localStorage but not context, redirect
  if (redirectPath) {
    console.log("AuthLayout: Redirecting to", redirectPath);
    return <Navigate to={redirectPath} replace />;
  }

  // If user is logged in from context, redirect to dashboard based on role
  if (user) {
    console.log("AuthLayout: User in context, redirecting based on role:", user.role);
    
    if (isTutor()) {
      return <Navigate to="/tutor" replace />;
    } else if (isStudent()) {
      return <Navigate to="/student" replace />;
    } else {
      // Default fallback - shouldn't normally happen
      return <Navigate to="/" replace />;
    }
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