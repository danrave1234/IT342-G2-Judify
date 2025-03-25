import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaGoogle, FaApple, FaFacebook, FaUser, FaChalkboardTeacher } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';

const Register = () => {
  const { register: registerUser } = useUser();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [userType, setUserType] = useState('');
  const [currentStep, setCurrentStep] = useState(1);
  const { register, handleSubmit, watch, formState: { errors } } = useForm();
  const password = watch('password', '');

  const handleUserTypeSelection = (type) => {
    setUserType(type);
    setCurrentStep(2);
  };

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      const userData = {
        ...data,
        roles: [userType.toUpperCase()],
      };
      
      const result = await registerUser(userData);
      if (result.success) {
        toast.success('Registration successful');
      } else {
        toast.error(result.message || 'Registration failed');
      }
    } catch (error) {
      toast.error('An error occurred during registration');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-6">
        <Link to="/" className="text-blue-600 text-2xl font-bold">Judify</Link>
        <h2 className="mt-6 text-xl font-bold text-gray-800">Create your account</h2>
        <p className="mt-2 text-sm text-gray-500">Join our community of learners and educators</p>
      </div>

      {currentStep === 1 ? (
        <div>
          <div className="flex flex-col space-y-4">
            <button
              type="button"
              onClick={() => handleUserTypeSelection('student')}
              className="relative flex items-center w-full p-4 border border-gray-300 rounded-md shadow-sm hover:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <div className="mr-4 text-blue-600">
                <FaUser size={24} />
              </div>
              <div className="text-left">
                <h3 className="font-medium text-gray-700">Student</h3>
                <p className="text-sm text-gray-500">I'm looking for tutoring</p>
              </div>
            </button>
            
            <button
              type="button"
              onClick={() => handleUserTypeSelection('tutor')}
              className="relative flex items-center w-full p-4 border border-gray-300 rounded-md shadow-sm hover:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <div className="mr-4 text-blue-600">
                <FaChalkboardTeacher size={24} />
              </div>
              <div className="text-left">
                <h3 className="font-medium text-gray-700">Tutor</h3>
                <p className="text-sm text-gray-500">I want to teach</p>
              </div>
            </button>
          </div>

          <div className="text-center mt-6">
            <p className="text-sm text-gray-500">or continue with email</p>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium text-gray-700">First Name</label>
              <input
                id="firstName"
                type="text"
                className={`mt-1 block w-full px-3 py-2 border ${
                  errors.firstName ? 'border-red-500' : 'border-gray-300'
                } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                {...register('firstName', {
                  required: 'First name is required',
                })}
              />
              {errors.firstName && (
                <p className="mt-1 text-sm text-red-600">{errors.firstName.message}</p>
              )}
            </div>
            <div>
              <label htmlFor="lastName" className="block text-sm font-medium text-gray-700">Last Name</label>
              <input
                id="lastName"
                type="text"
                className={`mt-1 block w-full px-3 py-2 border ${
                  errors.lastName ? 'border-red-500' : 'border-gray-300'
                } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                {...register('lastName', {
                  required: 'Last name is required',
                })}
              />
              {errors.lastName && (
                <p className="mt-1 text-sm text-red-600">{errors.lastName.message}</p>
              )}
            </div>
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700">Email Address</label>
            <input
              id="email"
              type="email"
              className={`mt-1 block w-full px-3 py-2 border ${
                errors.email ? 'border-red-500' : 'border-gray-300'
              } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
              {...register('email', {
                required: 'Email is required',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message: 'Invalid email address',
                },
              })}
            />
            {errors.email && (
              <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700">Password</label>
            <input
              id="password"
              type="password"
              className={`mt-1 block w-full px-3 py-2 border ${
                errors.password ? 'border-red-500' : 'border-gray-300'
              } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
              {...register('password', {
                required: 'Password is required',
                minLength: {
                  value: 8,
                  message: 'Password must be at least 8 characters long',
                },
              })}
            />
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>
            )}
            <p className="mt-1 text-sm text-gray-500">Must be at least 8 characters long</p>
          </div>

          <div className="flex items-center">
            <input
              id="terms"
              name="terms"
              type="checkbox"
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              {...register('terms', {
                required: 'You must agree to the terms of service',
              })}
            />
            <label htmlFor="terms" className="ml-2 block text-sm text-gray-700">
              I agree to Judify's <Link to="/terms" className="text-blue-600">Terms of Service</Link> and <Link to="/privacy" className="text-blue-600">Privacy Policy</Link>
            </label>
          </div>
          {errors.terms && (
            <p className="text-sm text-red-600">{errors.terms.message}</p>
          )}

          <div>
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {isSubmitting ? 'Creating account...' : 'Continue'}
            </button>
          </div>
        </form>
      )}

      {currentStep === 2 && (
        <div className="mt-6">
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white text-gray-500">Or sign up with</span>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-3">
            <button
              type="button"
              className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              <FaGoogle className="text-red-500" />
            </button>
            <button
              type="button"
              className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              <FaApple className="text-gray-800" />
            </button>
            <button
              type="button"
              className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              <FaFacebook className="text-blue-800" />
            </button>
          </div>
        </div>
      )}

      <div className="text-center mt-6">
        <p className="text-sm text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-blue-600 hover:text-blue-500">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
};

export default Register; 