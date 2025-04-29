import { Navigate, useLocation } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import LoadingSpinner from '../common/LoadingSpinner';

/**
 * A route guard component to protect routes that require authentication
 */
const PrivateRoute = ({ children }) => {
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
        return !!(userData && userData.userId);
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
        <LoadingSpinner size="xl" />
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

  return children;
};

export default PrivateRoute; 