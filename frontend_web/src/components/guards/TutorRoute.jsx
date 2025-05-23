import { Navigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../common/LoadingSpinner';

/**
 * A route guard component to protect routes that are accessible only to tutors
 */
const TutorRoute = ({ children }) => {
  const { user, loading, isTutor } = useUser();
  const { currentUser, loading: authLoading, isTutor: authIsTutor } = useAuth();

  // Direct localStorage check as a fallback
  const checkLocalStorage = () => {
    try {
      const token = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');
      
      if (!token || !storedUser) {
        return false;
      }
      
      try {
        const userData = JSON.parse(storedUser);
        const role = (userData.role || '').toUpperCase();
        return role.includes('TUTOR');
      } catch (parseErr) {
        console.error("TutorRoute: Error parsing user data:", parseErr);
        return false;
      }
    } catch (err) {
      console.error("TutorRoute: Error checking localStorage:", err);
      return false;
    }
  };

  // If still loading, show spinner
  if (loading || authLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <LoadingSpinner size="xl" />
      </div>
    );
  }

  // First check Auth context
  if (currentUser && authIsTutor()) {
    return children;
  }
  
  // Then check User context
  if (user && isTutor()) {
    return children;
  }
  
  // If no user in context, try localStorage
  if (!currentUser && !user && checkLocalStorage()) {
    return children;
  }
  
  // User exists but is not a tutor, redirect to home
  if (currentUser || user) {
    console.log("TutorRoute: User is not a tutor, redirecting to home");
    return <Navigate to="/" replace />;
  }
  
  // No authenticated user, redirect to login
  console.log("TutorRoute: No authenticated user, redirecting to login");
  return <Navigate to="/auth/login" replace />;
};

export default TutorRoute; 