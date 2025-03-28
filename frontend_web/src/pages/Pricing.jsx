import { Link } from 'react-router-dom';

const Pricing = () => {
  return (
    <div className="page-container">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white">Transparent Pricing</h1>
          <p className="mt-4 text-xl text-gray-600 dark:text-gray-400">
            Affordable tutoring with no hidden fees
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8 mb-16">
          {/* Student Plan */}
          <div className="card p-6">
            <div className="text-center mb-6">
              <h3 className="text-2xl font-bold text-gray-900 dark:text-white">Student Plan</h3>
              <div className="mt-4 flex items-center justify-center">
                <span className="text-5xl font-bold text-gray-900 dark:text-white">$0</span>
                <span className="ml-2 text-gray-500 dark:text-gray-400">/month</span>
              </div>
              <p className="mt-2 text-gray-600 dark:text-gray-400">No subscription required</p>
            </div>

            <ul className="space-y-3 mb-6">
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Browse all tutors</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Book sessions on-demand</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Pay only for the sessions you book</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Message tutors directly</span>
              </li>
            </ul>

            <Link
              to="/register"
              className="btn-primary w-full block text-center"
            >
              Sign Up
            </Link>
          </div>

          {/* Tutor Earnings */}
          <div className="card p-6 border-primary-600 dark:border-primary-500 border-2">
            <div className="text-center mb-6">
              <div className="inline-block bg-primary-100 dark:bg-primary-900/20 text-primary-800 dark:text-primary-300 px-3 py-1 rounded-full text-sm mb-4">
                Most Popular
              </div>
              <h3 className="text-2xl font-bold text-gray-900 dark:text-white">Tutor Earnings</h3>
              <div className="mt-4 flex items-center justify-center">
                <span className="text-5xl font-bold text-gray-900 dark:text-white">85%</span>
                <span className="ml-2 text-gray-500 dark:text-gray-400">of session fee</span>
              </div>
              <p className="mt-2 text-gray-600 dark:text-gray-400">15% platform fee</p>
            </div>

            <ul className="space-y-3 mb-6">
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Set your own hourly rate</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Get paid promptly after sessions</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Build your reputation with reviews</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Access to all platform tools</span>
              </li>
            </ul>

            <Link
              to="/register?type=tutor"
              className="btn-primary w-full block text-center"
            >
              Become a Tutor
            </Link>
          </div>

          {/* Premium Student */}
          <div className="card p-6">
            <div className="text-center mb-6">
              <h3 className="text-2xl font-bold text-gray-900 dark:text-white">Premium Student</h3>
              <div className="mt-4 flex items-center justify-center">
                <span className="text-5xl font-bold text-gray-900 dark:text-white">$19</span>
                <span className="ml-2 text-gray-500 dark:text-gray-400">/month</span>
              </div>
              <p className="mt-2 text-gray-600 dark:text-gray-400">Billed monthly</p>
            </div>

            <ul className="space-y-3 mb-6">
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">All Student Plan features</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">10% discount on all sessions</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Priority booking with top tutors</span>
              </li>
              <li className="flex items-start">
                <svg className="h-6 w-6 text-green-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span className="text-gray-600 dark:text-gray-400">Exclusive study resources</span>
              </li>
            </ul>

            <Link
              to="/register?plan=premium"
              className="bg-white text-primary-600 border border-primary-600 hover:bg-primary-50 dark:bg-dark-800 dark:text-primary-500 dark:border-primary-500 dark:hover:bg-dark-700 py-2 px-4 rounded-lg transition-colors w-full block text-center"
            >
              Go Premium
            </Link>
          </div>
        </div>

        {/* FAQs */}
        <div className="mt-16">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white text-center mb-8">
            Frequently Asked Questions
          </h2>

          <div className="grid md:grid-cols-2 gap-8">
            <div className="card p-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                How do payments work?
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                Students pay directly through our secure platform. Tutors receive 85% of the session fee, and payments are processed within 2-3 business days.
              </p>
            </div>

            <div className="card p-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                Can I cancel a session?
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                Yes, sessions can be canceled up to 12 hours before the scheduled time with no fee. Late cancellations may incur a 50% charge.
              </p>
            </div>

            <div className="card p-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                How are tutors vetted?
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                All tutors go through a comprehensive verification process, including education checks, subject knowledge tests, and a background check.
              </p>
            </div>

            <div className="card p-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                Is there a minimum session length?
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                Session minimums are set by tutors, but typically start at 30 minutes. Most sessions are 60 minutes in length.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Pricing; 