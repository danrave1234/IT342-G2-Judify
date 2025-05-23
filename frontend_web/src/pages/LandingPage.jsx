import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaMapMarkerAlt, FaVideo, FaCreditCard, FaChevronRight } from 'react-icons/fa';
import DarkModeToggle from '../components/layout/DarkModeToggle';

const LandingPage = () => {
  const [darkMode, setDarkMode] = useState(
    localStorage.getItem('darkMode') === 'true' || 
    window.matchMedia('(prefers-color-scheme: dark)').matches
  );

  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [darkMode]);

  return (
    <div className="page-container">

      {/* Hero Section */}
      <section className="py-12 bg-primary-50 dark:bg-dark-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="lg:flex lg:items-center lg:justify-between">
            <div className="lg:w-1/2">
              <h1 className="text-4xl font-bold text-gray-900 dark:text-white sm:text-5xl">
                Find Your Perfect Tutor, Online or Nearby
              </h1>
              <p className="mt-3 text-lg text-gray-600 dark:text-gray-400">
                Connect with expert tutors for in-person or virtual sessions. Smart matching based on location, expertise, and availability.
              </p>
              <div className="mt-8 flex gap-4">
                <Link
                  to="/register"
                  className="btn-primary px-6 py-3 font-medium"
                >
                  Find a Tutor
                </Link>
                <Link
                  to="/register?type=tutor"
                  className="border border-primary-600 dark:border-primary-500 text-primary-600 dark:text-primary-500 px-6 py-3 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors font-medium"
                >
                  Become a Tutor
                </Link>
              </div>
            </div>
            <div className="mt-10 lg:mt-0 lg:w-1/2">
              <img
                src="/src/assets/images/hero-illustration.svg"
                alt="Tutoring session illustration"
                className="w-full"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-center text-3xl font-bold text-gray-900 dark:text-white">
            Why Choose Judify?
          </h2>
          <div className="mt-12 grid gap-8 md:grid-cols-3">
            {/* Feature 1 */}
            <div className="card p-6">
              <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/20 rounded-full flex items-center justify-center text-primary-600 dark:text-primary-500 mb-4">
                <FaMapMarkerAlt size={24} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Smart Location Matching</h3>
              <p className="mt-2 text-gray-600 dark:text-gray-400">
                Find tutors near you or connect remotely. GPS-based matching for in-person sessions.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="card p-6">
              <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/20 rounded-full flex items-center justify-center text-primary-600 dark:text-primary-500 mb-4">
                <FaVideo size={24} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Virtual Sessions</h3>
              <p className="mt-2 text-gray-600 dark:text-gray-400">
                High-quality video conferencing with document sharing and interactive tools.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="card p-6">
              <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/20 rounded-full flex items-center justify-center text-primary-600 dark:text-primary-500 mb-4">
                <FaCreditCard size={24} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Secure Payments</h3>
              <p className="mt-2 text-gray-600 dark:text-gray-400">
                Automated billing and secure payment processing for all sessions.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section className="py-16 bg-light-800 dark:bg-dark-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-center text-3xl font-bold text-gray-900 dark:text-white mb-12">
            Success Stories
          </h2>
          <div className="grid gap-8 md:grid-cols-3">
            {/* Testimonial 1 */}
            <div className="bg-white dark:bg-dark-700 p-6 rounded-lg shadow-card">
              <div className="flex items-center mb-4">
                <img
                  className="h-12 w-12 rounded-full object-cover mr-4"
                  src="https://randomuser.me/api/portraits/women/32.jpg"
                  alt="Sarah Johnson"
                />
                <div>
                  <h4 className="text-lg font-semibold text-gray-900 dark:text-white">Sarah Johnson</h4>
                  <p className="text-gray-500 dark:text-gray-400 text-sm">Mathematics Student</p>
                </div>
              </div>
              <p className="text-gray-600 dark:text-gray-300">
                "Found an amazing math tutor just a few clicks. I've improved my grades significantly!"
              </p>
            </div>

            {/* Testimonial 2 */}
            <div className="bg-white dark:bg-dark-700 p-6 rounded-lg shadow-card">
              <div className="flex items-center mb-4">
                <img
                  className="h-12 w-12 rounded-full object-cover mr-4"
                  src="https://randomuser.me/api/portraits/men/46.jpg"
                  alt="David Chen"
                />
                <div>
                  <h4 className="text-lg font-semibold text-gray-900 dark:text-white">David Chen</h4>
                  <p className="text-gray-500 dark:text-gray-400 text-sm">Physics Tutor</p>
                </div>
              </div>
              <p className="text-gray-600 dark:text-gray-300">
                "The platform makes it easy to manage my schedule and connect with students. The video conferencing tools are top-notch!"
              </p>
            </div>

            {/* Testimonial 3 */}
            <div className="bg-white dark:bg-dark-700 p-6 rounded-lg shadow-card">
              <div className="flex items-center mb-4">
                <img
                  className="h-12 w-12 rounded-full object-cover mr-4"
                  src="https://randomuser.me/api/portraits/women/68.jpg"
                  alt="Emma Wilson"
                />
                <div>
                  <h4 className="text-lg font-semibold text-gray-900 dark:text-white">Emma Wilson</h4>
                  <p className="text-gray-500 dark:text-gray-400 text-sm">Parent</p>
                </div>
              </div>
              <p className="text-gray-600 dark:text-gray-300">
                "As a parent, I love the safety features and the ability to track my child's progress. The payment system is also very convenient."
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 bg-primary-600 dark:bg-primary-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl font-bold text-white mb-4">
            Ready to Start Learning?
          </h2>
          <p className="text-lg text-primary-100 mb-8">
            Join thousands of students who are already improving their grades with Judify
          </p>
          <Link
            to="/register"
            className="inline-block bg-white text-primary-600 px-8 py-3 rounded-lg font-medium hover:bg-primary-50 transition-colors shadow-sm"
          >
            Get Started Today
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-white dark:bg-dark-800 border-t border-gray-200 dark:border-dark-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">Company</h3>
              <ul className="mt-4 space-y-2">
                <li>
                  <Link to="/about" className="nav-link">
                    About
                  </Link>
                </li>
                <li>
                  <Link to="/careers" className="nav-link">
                    Careers
                  </Link>
                </li>
                <li>
                  <Link to="/blog" className="nav-link">
                    Blog
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">Support</h3>
              <ul className="mt-4 space-y-2">
                <li>
                  <Link to="/help" className="nav-link">
                    Help Center
                  </Link>
                </li>
                <li>
                  <Link to="/safety" className="nav-link">
                    Safety
                  </Link>
                </li>
                <li>
                  <Link to="/contact" className="nav-link">
                    Contact Us
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">Legal</h3>
              <ul className="mt-4 space-y-2">
                <li>
                  <Link to="/privacy" className="nav-link">
                    Privacy
                  </Link>
                </li>
                <li>
                  <Link to="/terms" className="nav-link">
                    Terms
                  </Link>
                </li>
                <li>
                  <Link to="/accessibility" className="nav-link">
                    Accessibility
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">Download</h3>
              <ul className="mt-4 space-y-2">
                <li>
                  <Link to="#" className="nav-link">
                    iOS App
                  </Link>
                </li>
                <li>
                  <Link to="#" className="nav-link">
                    Android App
                  </Link>
                </li>
              </ul>
            </div>
          </div>
          <div className="mt-12 border-t border-gray-200 dark:border-dark-700 pt-8 flex flex-col md:flex-row justify-between items-center">
            <p className="text-gray-500 dark:text-gray-400 text-sm">
              &copy; {new Date().getFullYear()} Judify. All rights reserved.
            </p>
            <div className="mt-4 md:mt-0 flex space-x-6">
              <Link to="#" className="text-gray-400 hover:text-primary-600 dark:hover:text-primary-500">
                <span className="sr-only">Facebook</span>
                <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M22 12c0-5.523-4.477-10-10-10S2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.878v-6.987h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.988C18.343 21.128 22 16.991 22 12z" />
                </svg>
              </Link>
              <Link to="#" className="text-gray-400 hover:text-primary-600 dark:hover:text-primary-500">
                <span className="sr-only">Instagram</span>
                <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12.315 2c2.43 0 2.784.013 3.808.06 1.064.049 1.791.218 2.427.465a4.902 4.902 0 011.772 1.153 4.902 4.902 0 011.153 1.772c.247.636.416 1.363.465 2.427.048 1.067.06 1.407.06 4.123v.08c0 2.643-.012 2.987-.06 4.043-.049 1.064-.218 1.791-.465 2.427a4.902 4.902 0 01-1.153 1.772 4.902 4.902 0 01-1.772 1.153c-.636.247-1.363.416-2.427.465-1.067.048-1.407.06-4.123.06h-.08c-2.643 0-2.987-.012-4.043-.06-1.064-.049-1.791-.218-2.427-.465a4.902 4.902 0 01-1.772-1.153 4.902 4.902 0 01-1.153-1.772c-.247-.636-.416-1.363-.465-2.427-.047-1.024-.06-1.379-.06-3.808v-.63c0-2.43.013-2.784.06-3.808.049-1.064.218-1.791.465-2.427a4.902 4.902 0 011.153-1.772A4.902 4.902 0 015.45 2.525c.636-.247 1.363-.416 2.427-.465C8.901 2.013 9.256 2 11.685 2h.63zm-.081 1.802h-.468c-2.456 0-2.784.011-3.807.058-.975.045-1.504.207-1.857.344-.467.182-.8.398-1.15.748-.35.35-.566.683-.748 1.15-.137.353-.3.882-.344 1.857-.047 1.023-.058 1.351-.058 3.807v.468c0 2.456.011 2.784.058 3.807.045.975.207 1.504.344 1.857.182.466.399.8.748 1.15.35.35.683.566 1.15.748.353.137.882.3 1.857.344 1.054.048 1.37.058 4.041.058h.08c2.597 0 2.917-.01 3.96-.058.976-.045 1.505-.207 1.858-.344.466-.182.8-.398 1.15-.748.35-.35.566-.683.748-1.15.137-.353.3-.882.344-1.857.048-1.055.058-1.37.058-4.041v-.08c0-2.597-.01-2.917-.058-3.96-.045-.976-.207-1.505-.344-1.858a3.097 3.097 0 00-.748-1.15 3.098 3.098 0 00-1.15-.748c-.353-.137-.882-.3-1.857-.344-1.023-.047-1.351-.058-3.807-.058zM12 6.865a5.135 5.135 0 110 10.27 5.135 5.135 0 010-10.27zm0 1.802a3.333 3.333 0 100 6.666 3.333 3.333 0 000-6.666zm5.338-3.205a1.2 1.2 0 110 2.4 1.2 1.2 0 010-2.4z" />
                </svg>
              </Link>
              <Link to="#" className="text-gray-400 hover:text-primary-600 dark:hover:text-primary-500">
                <span className="sr-only">Twitter</span>
                <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8.29 20.251c7.547 0 11.675-6.253 11.675-11.675 0-.178 0-.355-.012-.53A8.348 8.348 0 0022 5.92a8.19 8.19 0 01-2.357.646 4.118 4.118 0 001.804-2.27 8.224 8.224 0 01-2.605.996 4.107 4.107 0 00-6.993 3.743 11.65 11.65 0 01-8.457-4.287 4.106 4.106 0 001.27 5.477A4.072 4.072 0 012.8 9.713v.052a4.105 4.105 0 003.292 4.022 4.095 4.095 0 01-1.853.07 4.108 4.108 0 003.834 2.85A8.233 8.233 0 012 18.407a11.616 11.616 0 006.29 1.84" />
                </svg>
              </Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage; 
