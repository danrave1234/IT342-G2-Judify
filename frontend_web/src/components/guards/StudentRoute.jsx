import { Navigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';

/**
 * A route guard component to protect routes that are accessible only to students
 */
const StudentRoute = ({ children }) => {
  const { user, loading, isStudent } = useUser();

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-light-900 dark:bg-dark-900">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  // Check if user is a student
  if (!user || !isStudent()) {
    return <Navigate to="/" />;
  }

  return children;
};

export default StudentRoute; 