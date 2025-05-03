import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaGoogle, FaArrowLeft } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';

const Login = () => {
  const { login, isTutor, isStudent } = useUser();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loginError, setLoginError] = useState('');
  const navigate = useNavigate();
  const { register, handleSubmit, setValue, formState: { errors } } = useForm();

  // Function to use test account for quick login
  const useTestAccount = (role) => {
    const email = role === 'tutor' ? 'tutor@example.com' : 'student@example.com';
    const password = 'password123';
    
    // Set form values
    setValue('email', email);
    setValue('password', password);
    
    // Submit the form
    handleLogin({ email, password });
  };

  const onSubmit = (data) => {
    handleLogin(data);
  };

  const handleLogin = async (data) => {
    setIsSubmitting(true);
    setLoginError('');
    
    try {
      console.log('Submitting login form...', data.email);
      const result = await login(data.email, data.password);
      
      if (result.success) {
        toast.success('Login successful');
        
        // Extra safety check - guarantee we have user data in localStorage
        let userData = null;
        try {
          const storedUser = localStorage.getItem('user');
          if (storedUser) {
            userData = JSON.parse(storedUser);
            console.log('User data retrieved from localStorage after login:', userData);
          }
        } catch (e) {
          console.error("Error parsing stored user:", e);
        }
        
        // Handle missing user data - should not happen with proper backend integration
        if (!userData) {
          console.error("No user data in localStorage after login - this should not happen");
          setLoginError('Authentication error - please try again');
          setIsSubmitting(false);
          return;
        }
        
        // Immediately navigate to appropriate dashboard
        const role = (userData.role || '').toUpperCase();
        console.log(`Login successful - navigating to ${role} dashboard`);
        
        if (role.includes('TUTOR')) {
          navigate('/tutor', { replace: true });
        } else {
          navigate('/student', { replace: true });
        }
      } else {
        // Handle failed login
        console.error('Login failed:', result.message);
        setLoginError(result.message || 'Invalid email or password');
        toast.error(result.message || 'Login failed');
      }
    } catch (error) {
      console.error("Login error:", error);
      setLoginError(error.message || 'An error occurred during login');
      toast.error('An error occurred during login');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Function to handle Google OAuth login
  const handleGoogleLogin = () => {
    // Use direct URL to the OAuth2 endpoint
    const googleAuthUrl = 'http://localhost:8080/oauth2/authorization/google';
    
    console.log('Redirecting to Google OAuth:', googleAuthUrl);
    window.location.href = googleAuthUrl;
  };

  return (
    <div className="relative">
      {/* Back button */}
      <Link to="/" className="absolute top-0 left-0 text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-100">
        <FaArrowLeft className="text-xl" />
      </Link>
      
      <div className="text-center mb-8">
        <h1 className="text-primary-600 dark:text-primary-500 text-3xl font-bold mb-2">Judify</h1>
        <h2 className="text-xl font-bold text-gray-800 dark:text-white">Sign in to your account</h2>
        <p className="auth-subtitle">Access your personalized tutoring dashboard</p>
      </div>

      {loginError && (
        <div className="mb-4 p-3 bg-red-100 border border-red-400 rounded text-red-700 dark:bg-red-900/30 dark:text-red-400 dark:border-red-700">
          {loginError}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label htmlFor="email" className="auth-form-label">Email Address</label>
          <input
            id="email"
            type="email"
            placeholder="your@email.com"
            className={`auth-form-input ${
              errors.email ? 'border-red-500 dark:border-red-400' : ''
            }`}
            {...register('email', {
              required: 'Email is required',
              pattern: {
                value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                message: 'Invalid email address',
              },
            })}
          />
          {errors.email && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.email.message}</p>
          )}
        </div>

        <div>
          <label htmlFor="password" className="auth-form-label">Password</label>
          <input
            id="password"
            type="password"
            placeholder="••••••••"
            className={`auth-form-input ${
              errors.password ? 'border-red-500 dark:border-red-400' : ''
            }`}
            {...register('password', {
              required: 'Password is required',
              minLength: {
                value: 6,
                message: 'Password must be at least 6 characters',
              },
            })}
          />
          {errors.password && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.password.message}</p>
          )}
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <input
              id="remember-me"
              name="remember-me"
              type="checkbox"
              className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 dark:border-dark-600 rounded"
            />
            <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-700 dark:text-gray-300">
              Remember me
            </label>
          </div>
          <Link
            to="/forgot-password"
            className="auth-form-link text-sm"
          >
            Forgot password?
          </Link>
        </div>

        <div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="auth-form-button disabled:opacity-50"
          >
            {isSubmitting ? (
              <span className="flex items-center justify-center">
                <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Signing in...
              </span>
            ) : 'Sign in'}
          </button>
        </div>
      </form>

      {/* Test account buttons for development */}
      {process.env.NODE_ENV !== 'production' && (
        <div className="mt-4 flex flex-col space-y-2">
          <div className="text-xs text-gray-500 dark:text-gray-400 text-center">Development test accounts:</div>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              className="text-xs py-1 px-2 border border-gray-300 dark:border-gray-700 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800"
              onClick={() => useTestAccount('student')}
            >
              Use Student Account
            </button>
            <button
              type="button"
              className="text-xs py-1 px-2 border border-gray-300 dark:border-gray-700 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800"
              onClick={() => useTestAccount('tutor')}
            >
              Use Tutor Account
            </button>
          </div>
        </div>
      )}

      <div className="mt-6">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-300 dark:border-dark-600"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-white dark:bg-dark-800 text-gray-500 dark:text-gray-400">Or continue with</span>
          </div>
        </div>

        <div className="mt-6 flex justify-center">
          <button 
            type="button" 
            className="flex items-center justify-center px-4 py-2 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-dark-800 hover:bg-gray-50 dark:hover:bg-dark-700"
            onClick={handleGoogleLogin}
          >
            <FaGoogle className="h-5 w-5 text-red-500 mr-2" />
            <span>Sign in with Google</span>
          </button>
        </div>
      </div>

      <div className="text-center mt-6">
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Don't have an account?{' '}
          <Link to="/register" className="auth-form-link">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
};

export default Login; 