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
        
        // Check if this is a new OAuth2 user
        try {
          const response = await axios.get(`/api/users/check-oauth2-status/${userId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
          });
          
          if (response.data && response.data.isNewOAuth2User) {
            // Redirect to complete registration
            console.log('New OAuth2 user - redirecting to complete registration');
            navigate(`/oauth2-register?userId=${userId}&token=${token}`, { replace: true });
            return;
          }
        } catch (checkErr) {
          console.warn('Error checking OAuth2 status:', checkErr);
          // Continue with normal flow if check fails
        }
        
        // Try to fetch complete user profile data
        try {
          console.log('Fetching user profile data for ID:', userId);
          const response = await userApi.getCurrentUser();
          
          if (response && response.data) {
            const userData = response.data;
            console.log('User profile data received:', userData);
            
            // Update user object with profile data
            const updatedUser = {
              ...user,
              email: userData.email || user.email,
              username: userData.username || user.username,
              firstName: userData.firstName || user.firstName,
              lastName: userData.lastName || user.lastName,
              role: userData.role || user.role,
              profileImage: userData.profilePicture || userData.profileImage || user.profileImage
            };
            
            // Update localStorage with complete user data
            localStorage.setItem('user', JSON.stringify(updatedUser));
          }
        } catch (profileErr) {
          console.warn('Could not fetch complete profile, continuing with basic user data:', profileErr);
          // Non-fatal error, we'll continue with the basic user info
        }
        
        // Show success message
        toast.success('Login successful');
        
        // Determine destination based on user role
        const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
        const role = (storedUser.role || 'STUDENT').toUpperCase();
        
        // Redirect to appropriate dashboard
        if (role.includes('TUTOR')) {
          navigate('/tutor', { replace: true });
        } else {
          navigate('/student', { replace: true });
        }
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
            <p className="text-gray-600">Redirecting to dashboard...</p>
          </>
        )}
      </div>
    </div>
  );
};

export default OAuth2Callback; 