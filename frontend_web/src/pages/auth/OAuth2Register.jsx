import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import axios from 'axios';
import { tutorProfileApi, studentProfileApi } from '../../api/api';

const OAuth2Register = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [userData, setUserData] = useState(null);
  const [error, setError] = useState(null);
  const [userType, setUserType] = useState('student');
  const { register, handleSubmit, setValue, formState: { errors } } = useForm();

  useEffect(() => {
    const loadUserData = async () => {
      const userId = searchParams.get('userId');
      const token = searchParams.get('token');
      
      if (!userId || !token) {
        setError('Missing required parameters. Please try again.');
        setIsLoading(false);
        return;
      }
      
      try {
        // Store token in localStorage for API calls
        localStorage.setItem('token', token);
        
        // Check user status
        const response = await axios.get(`/api/users/check-oauth2-status/${userId}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (!response.data.isNewOAuth2User) {
          // User already completed registration - redirect to dashboard
          navigate('/student', { replace: true });
          return;
        }
        
        // Set form values
        setUserData(response.data);
        setValue('firstName', response.data.firstName || '');
        setValue('lastName', response.data.lastName || '');
        setValue('email', response.data.email || '');
        setValue('username', response.data.username || '');
        setValue('userId', userId);
        setValue('userType', userType);
        
        setIsLoading(false);
      } catch (err) {
        console.error('Error loading user data:', err);
        setError('Failed to load user data. Please try again.');
        setIsLoading(false);
      }
    };
    
    loadUserData();
  }, [searchParams, navigate, setValue]);

  const handleUserTypeSelection = (type) => {
    setUserType(type);
    setValue('userType', type);
  };

  const createStudentProfile = async (userId) => {
    try {
      // Create a basic student profile
      const studentProfileData = {
        userId: userId,
        bio: "I'm a new student on the platform",
        gradeLevel: "College",
        school: "To be updated",
        interests: ["General Learning"]
      };
      
      const response = await studentProfileApi.createProfile(studentProfileData);
      if (response && response.data) {
        console.log("Student profile created successfully:", response.data);
        return true;
      }
      return false;
    } catch (error) {
      console.error("Error creating student profile:", error);
      // Don't fail the whole registration process if student profile creation fails
      // User can create it later from their profile page
      return false;
    }
  };

  const createTutorProfile = async (userId) => {
    try {
      // Create a basic tutor profile
      const tutorProfileData = {
        userId: userId,
        bio: "I'm a new tutor on the platform",
        expertise: "New to tutoring",
        hourlyRate: 20.00,
        subjects: ["General"],
        rating: 0.0,
        totalReviews: 0
      };
      
      const response = await tutorProfileApi.createProfile(tutorProfileData);
      if (response && response.data) {
        console.log("Tutor profile created successfully:", response.data);
        return true;
      }
      return false;
    } catch (error) {
      console.error("Error creating tutor profile:", error);
      // Don't fail the whole registration process if tutor profile creation fails
      // User can create it later from their profile page
      return false;
    }
  };

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    
    try {
      // Construct user data for update
      const updateData = {
        firstName: data.firstName,
        lastName: data.lastName,
        username: data.username,
        role: data.userType === 'student' ? 'STUDENT' : 'TUTOR'
      };
      
      // Get token from localStorage
      const token = localStorage.getItem('token');
      
      // Complete registration
      const response = await axios.post(`/api/users/complete-oauth2-registration/${data.userId}`, updateData, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.data) {
        // Update token and user data
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data.user));
        
        // Create the appropriate profile based on role
        let profileCreated = false;
        if (data.userType === 'student') {
          profileCreated = await createStudentProfile(data.userId);
          if (profileCreated) {
            toast.info('Your student profile has been created.');
          }
        } else if (data.userType === 'tutor') {
          profileCreated = await createTutorProfile(data.userId);
          if (profileCreated) {
            toast.info('A basic tutor profile has been created. Remember to update it with your details!');
          } else {
            toast.info('Please set up your tutor profile to start teaching');
          }
        }
        
        toast.success('Registration completed successfully!');
        
        // Redirect based on role
        if (response.data.user.role === 'TUTOR') {
          navigate('/tutor', { replace: true });
        } else {
          navigate('/student', { replace: true });
        }
      }
    } catch (err) {
      console.error('Error completing registration:', err);
      toast.error('Failed to complete registration. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="text-center">
          <h2 className="text-xl font-bold mb-4">Loading user data...</h2>
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600 mx-auto"></div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="text-center">
          <h2 className="text-xl font-bold mb-4 text-red-500">Error</h2>
          <p className="text-gray-600 mb-4">{error}</p>
          <Link to="/login" className="text-primary-600 hover:text-primary-700">
            Back to Login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="text-center mb-6">
        <Link to="/" className="auth-title">Judify</Link>
        <h2 className="mt-4 text-xl font-bold text-gray-800 dark:text-white">Complete Your Registration</h2>
        <p className="auth-subtitle">You're almost there! Just a few more details...</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
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
          <input 
            type="hidden" 
            {...register('userId')} 
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
            className="auth-form-input bg-gray-100"
            readOnly 
            disabled
            {...register('email')}
          />
          <p className="mt-1 text-xs text-gray-500">Email address from your Google account (cannot be changed)</p>
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

        {userType === 'tutor' && (
          <div className="p-4 bg-blue-50 rounded-lg dark:bg-blue-900/30">
            <h3 className="text-md font-semibold text-blue-800 dark:text-blue-300 mb-2">Becoming a Tutor</h3>
            <p className="text-sm text-blue-700 dark:text-blue-400">
              After registration, you'll need to complete your tutor profile with your:
            </p>
            <ul className="text-sm text-blue-700 dark:text-blue-400 list-disc list-inside mt-2">
              <li>Teaching subjects</li>
              <li>Hourly rate</li>
              <li>Professional experience</li>
              <li>Availability</li>
            </ul>
          </div>
        )}

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
                Completing Registration...
              </span>
            ) : 'Complete Registration'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default OAuth2Register; 