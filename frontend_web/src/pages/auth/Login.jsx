import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaGoogle, FaApple, FaFacebook } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';

const Login = () => {
  const { login } = useUser();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      const result = await login(data.email, data.password);
      if (result.success) {
        toast.success('Login successful');
      } else {
        toast.error(result.message || 'Login failed');
      }
    } catch (error) {
      toast.error('An error occurred during login');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-6">
        <Link to="/" className="auth-title">Judify</Link>
        <h2 className="mt-6 text-xl font-bold text-gray-800 dark:text-white">Sign in to your account</h2>
        <p className="auth-subtitle">Access your personalized tutoring dashboard</p>
      </div>

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
                value: 8,
                message: 'Password must be at least 8 characters',
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
            {isSubmitting ? 'Signing in...' : 'Sign in'}
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