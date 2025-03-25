import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { useUser } from '../../context/UserContext';
import { FaCheck } from 'react-icons/fa';

const ForgotPassword = () => {
  const { requestPasswordReset } = useUser();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [emailSent, setEmailSent] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      const result = await requestPasswordReset(data.email);
      if (result.success) {
        setEmailSent(true);
        toast.success('Password reset instructions sent to your email');
      } else {
        toast.error(result.message || 'Failed to send reset instructions');
      }
    } catch (error) {
      toast.error('An error occurred while processing your request');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-6">
        <Link to="/" className="auth-title">Judify</Link>
        <h2 className="mt-4 text-xl font-bold text-gray-800 dark:text-white">Reset your password</h2>
        <p className="auth-subtitle">
          Enter your email and we'll send you instructions to reset your password
        </p>
      </div>

      {emailSent ? (
        <div className="text-center py-4">
          <div className="mb-4 text-green-600 dark:text-green-400">
            <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-full bg-green-100 dark:bg-green-900/20">
              <FaCheck className="h-6 w-6" />
            </div>
          </div>
          <h3 className="text-lg font-medium text-gray-900 dark:text-white">Check your email</h3>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
            We've sent password reset instructions to your email.
            Please check your inbox and spam folder.
          </p>
          <div className="mt-6">
            <Link to="/login" className="auth-form-button block w-full text-center">
              Return to login
            </Link>
          </div>
        </div>
      ) : (
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

          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="auth-form-button disabled:opacity-50"
            >
              {isSubmitting ? 'Sending...' : 'Send reset instructions'}
            </button>
          </div>
        </form>
      )}

      <div className="text-center mt-6">
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Remembered your password?{' '}
          <Link to="/login" className="auth-form-link">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
};

export default ForgotPassword; 