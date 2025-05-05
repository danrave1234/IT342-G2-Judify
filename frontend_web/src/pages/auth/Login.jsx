import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaGoogle, FaArrowLeft } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';

const Login = () => {
  const { login } = useUser();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loginError, setLoginError] = useState('');
  const [email, setEmail] = useState('');
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors }, setValue } = useForm();

  const onSubmit = (data) => {
    setEmail(data.email);
    handleLogin(data);
  };

  useEffect(() => {
    if (email) {
      setValue('email', email);
    }
  }, [email, setValue]);

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
    // Determine if we're in production or development
    const baseUrl = window.location.hostname === 'localhost' 
      ? 'http://localhost:8080'
      : 'https://judify-795422705086.asia-east1.run.app';

    // Use direct URL to the OAuth2 endpoint with the correct base URL
    const googleAuthUrl = `${baseUrl}/oauth2/authorization/google`;

    console.log('Redirecting to Google OAuth:', googleAuthUrl);
    window.location.href = googleAuthUrl;
  };

  // Add CSS to handle autocomplete styling
  const autocompleteStyles = `
    /* Add these styles to fix autocomplete styling */
    input:-webkit-autofill,
    input:-webkit-autofill:hover, 
    input:-webkit-autofill:focus,
    input:-webkit-autofill:active {
      -webkit-box-shadow: 0 0 0 30px white inset !important;
      transition: background-color 5000s ease-in-out 0s;
    }
    
    .dark input:-webkit-autofill,
    .dark input:-webkit-autofill:hover, 
    .dark input:-webkit-autofill:focus,
    .dark input:-webkit-autofill:active {
      -webkit-box-shadow: 0 0 0 30px #1f2937 inset !important;
      -webkit-text-fill-color: #e5e7eb !important;
    }
    
    /* Fix input size consistency */
    input.auth-form-input {
      height: 2.5rem !important; /* Fixed height */
      padding: 0.5rem 0.75rem !important; /* Fixed padding */
      min-height: 2.5rem !important;
      box-sizing: border-box !important;
      font-size: 1rem !important;
      line-height: 1.5 !important;
    }
    
    /* Ensure suggestions don't affect layout */
    input:-webkit-autofill {
      height: 2.5rem !important;
      padding: 0.5rem 0.75rem !important;
    }
  `;

  return (
    <div className="relative">
      <style>{autocompleteStyles}</style>
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
            autoComplete="username email"
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
            autoComplete="current-password"
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
          Don&apos;t have an account?{' '}
          <Link to="/register" className="auth-form-link">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
};

export default Login;
