import { Navigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';

/**
 * A route guard component to protect routes that require authentication
 */
const PrivateRoute = ({ children }) => {
  const { user, loading } = useUser();

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-light-900 dark:bg-dark-900">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" />;
  }

  return children;
};

export default PrivateRoute; 