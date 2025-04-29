import { Navigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../common/LoadingSpinner';

/**
 * A route guard component to protect routes that are accessible only to students
 */
const StudentRoute = ({ children }) => {
  const { user, loading, isStudent } = useUser();
  const { currentUser, loading: authLoading, isStudent: authIsStudent } = useAuth();

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
        return role.includes('STUDENT');
      } catch (parseErr) {
        console.error("StudentRoute: Error parsing user data:", parseErr);
        return false;
      }
    } catch (err) {
      console.error("StudentRoute: Error checking localStorage:", err);
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
  if (currentUser && authIsStudent()) {
    return children;
  }
  
  // Then check User context
  if (user && isStudent()) {
    return children;
  }
  
  // If no user in context, try localStorage
  if (!currentUser && !user && checkLocalStorage()) {
    return children;
  }
  
  // User exists but is not a student, redirect to home
  if (currentUser || user) {
    console.log("StudentRoute: User is not a student, redirecting to home");
    return <Navigate to="/" replace />;
  }
  
  // No authenticated user, redirect to login
  console.log("StudentRoute: No authenticated user, redirecting to login");
  return <Navigate to="/auth/login" replace />;
};

export default StudentRoute; 