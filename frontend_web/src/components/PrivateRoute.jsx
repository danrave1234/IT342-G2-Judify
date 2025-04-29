import { Navigate, useLocation } from 'react-router-dom';
import { useUser } from '../context/UserContext';
import { Spinner } from 'flowbite-react';

const PrivateRoute = ({ children, requiredRole }) => {
  const { user, loading } = useUser();
  const location = useLocation();

  // Direct localStorage check as a fallback
  const checkLocalStorage = () => {
    try {
      const token = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');
      
      if (!token || !storedUser) {
        console.log("PrivateRoute: No token or user in localStorage");
        return false;
      }
      
      try {
        const userData = JSON.parse(storedUser);
        console.log("PrivateRoute: Found user in localStorage:", userData);
        
        // If requiredRole is specified, check if user has that role
        if (requiredRole) {
          const userRole = userData.role?.toUpperCase() || '';
          return userRole.includes(requiredRole.toUpperCase());
        }
        
        return true;
      } catch (parseErr) {
        console.error("PrivateRoute: Error parsing user data:", parseErr);
        return false;
      }
    } catch (err) {
      console.error("PrivateRoute: Error checking localStorage:", err);
      return false;
    }
  };

  // If we're still loading the user data, show a loading spinner
  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <Spinner size="xl" />
      </div>
    );
  }

  // First check context, then fallback to localStorage
  const isAuthenticated = !!user || checkLocalStorage();
  
  // If user is not authenticated, redirect to login
  if (!isAuthenticated) {
    console.log("PrivateRoute: Not authenticated, redirecting to /auth/login");
    return <Navigate to="/auth/login" state={{ from: location }} replace />;
  }

  // If role check is required
  if (requiredRole) {
    // Check context first
    if (user) {
      const userRole = user.role?.toUpperCase() || '';
      if (!userRole.includes(requiredRole.toUpperCase())) {
        console.log(`PrivateRoute: User role ${userRole} does not match required role ${requiredRole}`);
        return <Navigate to="/" replace />;
      }
    } else {
      // Fallback to localStorage check
      const hasRole = checkLocalStorage();
      if (!hasRole) {
        console.log(`PrivateRoute: User does not have required role ${requiredRole} based on localStorage`);
        return <Navigate to="/" replace />;
      }
    }
  }

  return children;
};

export default PrivateRoute; 