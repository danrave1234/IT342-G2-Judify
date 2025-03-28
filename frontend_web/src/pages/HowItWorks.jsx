import { Link } from 'react-router-dom';

const HowItWorks = () => {
  return (
    <div className="page-container">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white">How Judify Works</h1>
          <p className="mt-4 text-xl text-gray-600 dark:text-gray-400">
            Connecting students with expert tutors in a few simple steps
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8 mb-16">
          {/* Step 1 */}
          <div className="card p-6 text-center">
            <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/20 rounded-full flex items-center justify-center text-primary-600 dark:text-primary-500 mx-auto mb-4">
              1
            </div>
            <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Find a Tutor</h3>
            <p className="mt-2 text-gray-600 dark:text-gray-400">
              Search for tutors based on subject, availability, and location preferences.
            </p>
          </div>

          {/* Step 2 */}
          <div className="card p-6 text-center">
            <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/20 rounded-full flex items-center justify-center text-primary-600 dark:text-primary-500 mx-auto mb-4">
              2
            </div>
            <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Book a Session</h3>
            <p className="mt-2 text-gray-600 dark:text-gray-400">
              Schedule an online or in-person session at a time that works for you.
            </p>
          </div>

          {/* Step 3 */}
          <div className="card p-6 text-center">
            <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/20 rounded-full flex items-center justify-center text-primary-600 dark:text-primary-500 mx-auto mb-4">
              3
            </div>
            <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Learn and Excel</h3>
            <p className="mt-2 text-gray-600 dark:text-gray-400">
              Connect with your tutor and improve your understanding of difficult subjects.
            </p>
          </div>
        </div>

        <div className="text-center mt-8">
          <Link to="/find-tutors" className="btn-primary">
            Find a Tutor Now
          </Link>
        </div>
      </div>
    </div>
  );
};

export default HowItWorks; 