import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { userApi } from '../../api/api';
import axios from 'axios';

const OAuth2Callback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState(null);
  
  useEffect(() => {
    const handleOAuth2Callback = async () => {
      try {
        const token = searchParams.get('token');
        const userId = searchParams.get('userId');
        const error = searchParams.get('error');
        
        console.log('OAuth2 callback received:', { token, userId, error });
        
        if (error) {
          setErrorMessage('Authentication failed. Please try again.');
          toast.error('Authentication failed. Please try again.');
          setTimeout(() => navigate('/auth/login'), 2000);
          return;
        }
        
        if (!token || !userId) {
          setErrorMessage('Invalid authentication response. Please try again.');
          toast.error('Invalid authentication response. Please try again.');
          setTimeout(() => navigate('/auth/login'), 2000);
          return;
        }
        
        // Store the token in localStorage
        localStorage.setItem('token', token);
        
        // Create basic user object
        const user = {
          userId: parseInt(userId),
          email: '',  // Will be populated on profile load
          username: '',
          firstName: '',
          lastName: '',
          role: 'STUDENT',  // Default role, will be updated from profile
          isAuthenticated: true
        };
        
        // Store user in localStorage
        localStorage.setItem('user', JSON.stringify(user));
        
        // Always redirect to OAuth2Register page to let the user select their role
        // and complete their profile information
        console.log('Redirecting to complete registration');
        navigate(`/oauth2-register?userId=${userId}&token=${token}`, { replace: true });
        return;
      } catch (err) {
        console.error('Error processing OAuth response:', err);
        setErrorMessage('Error completing authentication. Please try again.');
        toast.error('Error completing authentication. Please try again.');
        setTimeout(() => navigate('/auth/login'), 2000);
      } finally {
        setIsLoading(false);
      }
    };
    
    handleOAuth2Callback();
  }, [searchParams, navigate]);
  
  return (
    <div className="flex justify-center items-center h-screen">
      <div className="text-center">
        {isLoading ? (
          <>
            <h2 className="text-xl font-bold mb-4">Completing authentication...</h2>
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600 mx-auto"></div>
          </>
        ) : errorMessage ? (
          <>
            <h2 className="text-xl font-bold mb-4 text-red-500">Authentication Error</h2>
            <p className="text-gray-600 mb-4">{errorMessage}</p>
            <p className="text-sm">Redirecting to login page...</p>
          </>
        ) : (
          <>
            <h2 className="text-xl font-bold mb-4 text-green-500">Authentication Successful</h2>
            <p className="text-gray-600">Redirecting to registration page...</p>
          </>
        )}
      </div>
    </div>
  );
};

export default OAuth2Callback; 