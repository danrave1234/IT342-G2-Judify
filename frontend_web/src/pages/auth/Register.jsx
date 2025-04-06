import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaGoogle, FaApple, FaFacebook } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';

const Register = () => {
  const { register: registerUser } = useUser();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [step, setStep] = useState(1);
  const { register, handleSubmit, watch, formState: { errors }, setValue } = useForm();
  const [userType, setUserType] = useState('student');
  
  const watchPassword = watch('password');

  const validatePasswordMatch = (value) => {
    return value === watchPassword || 'Passwords do not match';
  };

  const handleUserTypeSelection = (type) => {
    setUserType(type);
    setValue('userType', type);
  };

  const onSubmit = async (data) => {
    if (step === 1) {
      setStep(2);
      return;
    }
    
    setIsSubmitting(true);
    try {
      // IMPORTANT: Ensure password is correctly set
      if (!data.password || data.password.trim() === '') {
        toast.error('Password is required for registration');
        setIsSubmitting(false);
        return;
      }
      
      // Validate password length
      if (data.password.length < 8) {
        toast.error('Password must be at least 8 characters long');
        setIsSubmitting(false);
        return;
      }
      
      // Construct the complete user data object
      const userData = {
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        username: data.username,
        password: data.password, // Make sure this is set and not empty
        role: data.userType === 'student' ? 'STUDENT' : 'TUTOR',
        profilePicture: '',
        contactDetails: ''
      };
      
      console.log('Sending registration data:', JSON.stringify(userData));
      
      // Try direct API call first (for debugging)
      try {
        console.log('Trying direct API call to /api/users/register');
        const directResponse = await fetch('http://localhost:8080/api/users/register', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(userData)
        });
        
        if (directResponse.ok) {
          const result = await directResponse.json();
          console.log('Direct API call successful:', result);
          toast.success('Registration successful! You can now log in.');
          setTimeout(() => {
            navigate('/auth/login');
          }, 2000);
          setIsSubmitting(false);
          return;
        } else {
          const errorText = await directResponse.text();
          console.error('Direct API call failed:', errorText);
          toast.error('Registration failed: ' + errorText);
          // Fall back to regular registration
        }
      } catch (directError) {
        console.error('Direct API call error:', directError);
        // Fall back to regular registration
      }
      
      // Regular registration through UserContext
      const result = await registerUser(userData);
      
      if (result.success) {
        toast.success('Registration successful! You can now log in.');
        setTimeout(() => {
          navigate('/auth/login');
        }, 2000);
      } else {
        toast.error(result.message || 'Registration failed. Please try again.');
      }
    } catch (error) {
      console.error('Registration error:', error);
      
      // Detailed error logging
      if (error.response) {
        console.error('Error response:', {
          status: error.response.status,
          data: error.response.data,
          headers: error.response.headers
        });
      }
      
      toast.error('Registration failed. Please check your information and try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-6">
        <Link to="/" className="auth-title">Judify</Link>
        <h2 className="mt-4 text-xl font-bold text-gray-800 dark:text-white">Create your account</h2>
        <p className="auth-subtitle">Join our community of learners and educators</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {step === 1 ? (
          <>
            <div className="space-y-4">
              <div>
                <label className="auth-form-label">I want to join as a</label>
                <div className="mt-2 grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => handleUserTypeSelection('student')}
                    className={`flex items-center justify-center py-2 px-4 border rounded-lg transition-colors ${
                      userType === 'student'
                        ? 'bg-primary-600/10 border-primary-600 text-primary-600 dark:bg-primary-900/20 dark:border-primary-500 dark:text-primary-500'
                        : 'border-gray-300 dark:border-dark-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-dark-700'
                    }`}
                  >
                    Student
                  </button>
                  <button
                    type="button"
                    onClick={() => handleUserTypeSelection('tutor')}
                    className={`flex items-center justify-center py-2 px-4 border rounded-lg transition-colors ${
                      userType === 'tutor'
                        ? 'bg-primary-600/10 border-primary-600 text-primary-600 dark:bg-primary-900/20 dark:border-primary-500 dark:text-primary-500'
                        : 'border-gray-300 dark:border-dark-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-dark-700'
                    }`}
                  >
                    Tutor
                  </button>
                </div>
                <input 
                  type="hidden" 
                  {...register('userType')} 
                  value={userType} 
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="firstName" className="auth-form-label">First Name</label>
                  <input
                    id="firstName"
                    type="text"
                    placeholder="John"
                    className={`auth-form-input ${
                      errors.firstName ? 'border-red-500 dark:border-red-400' : ''
                    }`}
                    {...register('firstName', {
                      required: 'First name is required',
                    })}
                  />
                  {errors.firstName && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.firstName.message}</p>
                  )}
                </div>
                <div>
                  <label htmlFor="lastName" className="auth-form-label">Last Name</label>
                  <input
                    id="lastName"
                    type="text"
                    placeholder="Doe"
                    className={`auth-form-input ${
                      errors.lastName ? 'border-red-500 dark:border-red-400' : ''
                    }`}
                    {...register('lastName', {
                      required: 'Last name is required',
                    })}
                  />
                  {errors.lastName && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.lastName.message}</p>
                  )}
                </div>
              </div>

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
                <label htmlFor="username" className="auth-form-label">Username</label>
                <input
                  id="username"
                  type="text"
                  placeholder="YourUsername"
                  className={`auth-form-input ${
                    errors.username ? 'border-red-500 dark:border-red-400' : ''
                  }`}
                  {...register('username', {
                    required: 'Username is required',
                    minLength: {
                      value: 3,
                      message: 'Username must be at least 3 characters'
                    },
                    pattern: {
                      value: /^[a-zA-Z0-9_-]+$/,
                      message: 'Username can only contain letters, numbers, underscores and hyphens'
                    }
                  })}
                />
                {errors.username && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.username.message}</p>
                )}
              </div>

              <div className="flex justify-end">
                <button type="submit" className="auth-form-button">
                  Continue
                </button>
              </div>
            </div>
          </>
        ) : (
          <>
            <div className="space-y-4">
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
                      value: 8,
                      message: 'Password must be at least 8 characters',
                    },
                  })}
                />
                {errors.password && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.password.message}</p>
                )}
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">Must be at least 8 characters long</p>
              </div>

              <div>
                <label htmlFor="confirmPassword" className="auth-form-label">Confirm Password</label>
                <input
                  id="confirmPassword"
                  type="password"
                  placeholder="••••••••"
                  className={`auth-form-input ${
                    errors.confirmPassword ? 'border-red-500 dark:border-red-400' : ''
                  }`}
                  {...register('confirmPassword', {
                    required: 'Please confirm your password',
                    validate: validatePasswordMatch,
                  })}
                />
                {errors.confirmPassword && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.confirmPassword.message}</p>
                )}
              </div>

              <div className="flex items-center">
                <input
                  id="terms"
                  type="checkbox"
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 dark:border-dark-600 rounded"
                  {...register('terms', {
                    required: 'You must agree to the terms',
                  })}
                />
                <label htmlFor="terms" className="ml-2 block text-sm text-gray-700 dark:text-gray-300">
                  I agree to Judify's{' '}
                  <Link to="/terms" className="auth-form-link">
                    Terms of Service
                  </Link>{' '}
                  and{' '}
                  <Link to="/privacy" className="auth-form-link">
                    Privacy Policy
                  </Link>
                </label>
              </div>
              {errors.terms && (
                <p className="text-sm text-red-600 dark:text-red-400">{errors.terms.message}</p>
              )}

              <div className="flex justify-between">
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="px-4 py-2 border border-gray-300 dark:border-dark-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-dark-700 transition-colors"
                >
                  Back
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="auth-form-button max-w-[200px]"
                >
                  {isSubmitting ? (
                    <span className="flex items-center justify-center">
                      <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      Creating account...
                    </span>
                  ) : 'Create account'}
                </button>
              </div>
            </div>
          </>
        )}
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

        <div className="mt-6 grid grid-cols-3 gap-3">
          <button type="button" className="auth-social-button">
            <FaGoogle className="text-red-500" />
          </button>
          <button type="button" className="auth-social-button">
            <FaApple className="text-gray-800 dark:text-white" />
          </button>
          <button type="button" className="auth-social-button">
            <FaFacebook className="text-primary-700" />
          </button>
        </div>
      </div>

      <div className="text-center mt-6">
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Already have an account?{' '}
          <Link to="/auth/login" className="auth-form-link">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
};

export default Register; 