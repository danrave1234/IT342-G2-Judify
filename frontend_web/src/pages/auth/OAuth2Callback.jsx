import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';

const OAuth2Callback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  useEffect(() => {
    const token = searchParams.get('token');
    const userId = searchParams.get('userId');
    const error = searchParams.get('error');
    
    if (error) {
      toast.error('Authentication failed. Please try again.');
      navigate('/auth/login');
      return;
    }
    
    if (!token || !userId) {
      toast.error('Invalid authentication response. Please try again.');
      navigate('/auth/login');
      return;
    }
    
    // Handle successful authentication
    try {
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
      
      // Show success message
      toast.success('Login successful');
      
      // Redirect to appropriate dashboard (will fetch full profile data)
      navigate('/student', { replace: true });
    } catch (err) {
      console.error('Error processing OAuth response:', err);
      toast.error('Error completing authentication. Please try again.');
      navigate('/auth/login');
    }
  }, [searchParams, navigate]);
  
  return (
    <div className="flex justify-center items-center h-screen">
      <div className="text-center">
        <h2 className="text-xl font-bold mb-4">Completing authentication...</h2>
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600 mx-auto"></div>
      </div>
    </div>
  );
};

export default OAuth2Callback; 