import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaArrowLeft } from 'react-icons/fa';
import axios from 'axios';

const ForgotPassword = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      // In a real app, this would send a request to the backend
      await axios.post('/api/auth/forgot-password', { email: data.email });
      setSubmitted(true);
      toast.success('Password reset link sent to your email');
    } catch (error) {
      toast.error(
        error.response?.data?.message || 'Failed to send reset link. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-6">
        <Link to="/" className="text-blue-600 text-2xl font-bold">Judify</Link>
        <h2 className="mt-6 text-xl font-bold text-gray-800">Reset your password</h2>
        <p className="mt-2 text-sm text-gray-500">
          Enter your email address and we'll send you a link to reset your password.
        </p>
      </div>

      {!submitted ? (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700">
              Email address
            </label>
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
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {isSubmitting ? 'Sending...' : 'Send reset link'}
            </button>
          </div>
        </form>
      ) : (
        <div className="text-center py-8">
          <div className="mb-4 text-green-500">
            <svg
              className="mx-auto h-12 w-12"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M5 13l4 4L19 7"
              ></path>
            </svg>
          </div>
          <h3 className="text-lg font-medium text-gray-900">Check your email</h3>
          <p className="mt-2 text-sm text-gray-500">
            We've sent a password reset link to your email address. Please check your inbox.
          </p>
        </div>
      )}

      <div className="mt-6">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-300"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-white text-gray-500">Or</span>
          </div>
        </div>

        <div className="mt-6">
          <Link
            to="/login"
            className="flex items-center justify-center text-sm text-blue-600 hover:text-blue-500"
          >
            <FaArrowLeft className="mr-2" />
            Back to login
          </Link>
        </div>
      </div>

      <div className="text-center mt-6">
        <p className="text-sm text-gray-600">
          Need help?{' '}
          <a href="#" className="font-medium text-blue-600 hover:text-blue-500">
            Contact support
          </a>
        </p>
      </div>
    </div>
  );
};

export default ForgotPassword; 