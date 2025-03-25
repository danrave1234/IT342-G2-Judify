import { Link } from 'react-router-dom';
import { FaMapMarkerAlt, FaVideo, FaCreditCard, FaChevronRight } from 'react-icons/fa';

const LandingPage = () => {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header/Navbar */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <Link to="/" className="text-blue-600 text-2xl font-bold">
                Judify
              </Link>
            </div>
            <nav className="hidden md:flex space-x-10">
              <Link to="/how-it-works" className="text-gray-600 hover:text-blue-600">
                How it Works
              </Link>
              <Link to="/find-tutors" className="text-gray-600 hover:text-blue-600">
                Find Tutors
              </Link>
              <Link to="/become-a-tutor" className="text-gray-600 hover:text-blue-600">
                Become a Tutor
              </Link>
              <Link to="/pricing" className="text-gray-600 hover:text-blue-600">
                Pricing
              </Link>
            </nav>
            <div className="flex items-center space-x-4">
              <Link
                to="/login"
                className="text-gray-600 hover:text-blue-600 px-3 py-2"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
              >
                Sign Up
              </Link>
            </div>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="py-12 bg-blue-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="lg:flex lg:items-center lg:justify-between">
            <div className="lg:w-1/2">
              <h1 className="text-4xl font-bold text-gray-900 sm:text-5xl">
                Find Your Perfect Tutor, Online or Nearby
              </h1>
              <p className="mt-3 text-lg text-gray-500">
                Connect with expert tutors for in-person or virtual sessions. Smart matching based on location, expertise, and availability.
              </p>
              <div className="mt-8 flex gap-4">
                <Link
                  to="/register"
                  className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 font-medium"
                >
                  Find a Tutor
                </Link>
                <Link
                  to="/register?type=tutor"
                  className="border border-blue-600 text-blue-600 px-6 py-3 rounded-lg hover:bg-blue-50 font-medium"
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
          <h2 className="text-center text-3xl font-bold text-gray-900">
            Why Choose Judify?
          </h2>
          <div className="mt-12 grid gap-8 md:grid-cols-3">
            {/* Feature 1 */}
            <div className="bg-white p-6 rounded-lg shadow-md">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 mb-4">
                <FaMapMarkerAlt size={24} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900">Smart Location Matching</h3>
              <p className="mt-2 text-gray-500">
                Find tutors near you or connect remotely. GPS-based matching for in-person sessions.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="bg-white p-6 rounded-lg shadow-md">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 mb-4">
                <FaVideo size={24} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900">Virtual Sessions</h3>
              <p className="mt-2 text-gray-500">
                High-quality video conferencing with document sharing and interactive tools.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="bg-white p-6 rounded-lg shadow-md">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 mb-4">
                <FaCreditCard size={24} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900">Secure Payments</h3>
              <p className="mt-2 text-gray-500">
                Automated billing and secure payment processing for all sessions.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section className="py-16 bg-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-center text-3xl font-bold text-gray-900 mb-12">
            Success Stories
          </h2>
          <div className="grid gap-8 md:grid-cols-3">
            {/* Testimonial 1 */}
            <div className="bg-white p-6 rounded-lg shadow-md">
              <div className="flex items-center mb-4">
                <img
                  className="h-12 w-12 rounded-full object-cover mr-4"
                  src="https://randomuser.me/api/portraits/women/32.jpg"
                  alt="Sarah Johnson"
                />
                <div>
                  <h4 className="text-lg font-semibold">Sarah Johnson</h4>
                  <p className="text-gray-500 text-sm">Mathematics Student</p>
                </div>
              </div>
              <p className="text-gray-600">
                "Found an amazing math tutor just a few clicks. I've improved my grades significantly!"
              </p>
            </div>

            {/* Testimonial 2 */}
            <div className="bg-white p-6 rounded-lg shadow-md">
              <div className="flex items-center mb-4">
                <img
                  className="h-12 w-12 rounded-full object-cover mr-4"
                  src="https://randomuser.me/api/portraits/men/46.jpg"
                  alt="David Chen"
                />
                <div>
                  <h4 className="text-lg font-semibold">David Chen</h4>
                  <p className="text-gray-500 text-sm">Physics Tutor</p>
                </div>
              </div>
              <p className="text-gray-600">
                "The platform makes it easy to manage my schedule and connect with students. The video conferencing tools are top-notch!"
              </p>
            </div>

            {/* Testimonial 3 */}
            <div className="bg-white p-6 rounded-lg shadow-md">
              <div className="flex items-center mb-4">
                <img
                  className="h-12 w-12 rounded-full object-cover mr-4"
                  src="https://randomuser.me/api/portraits/women/68.jpg"
                  alt="Emma Wilson"
                />
                <div>
                  <h4 className="text-lg font-semibold">Emma Wilson</h4>
                  <p className="text-gray-500 text-sm">Parent</p>
                </div>
              </div>
              <p className="text-gray-600">
                "As a parent, I love the safety features and the ability to track my child's progress. The payment system is also very convenient."
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 bg-blue-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl font-bold text-white mb-4">
            Ready to Start Learning?
          </h2>
          <p className="text-lg text-blue-100 mb-8">
            Join thousands of students who are already improving their grades with Judify
          </p>
          <div className="flex justify-center space-x-4">
            <Link
              to="/register"
              className="bg-white text-blue-600 px-6 py-3 rounded-lg hover:bg-blue-50 font-medium"
            >
              Get Started Now
            </Link>
            <Link
              to="/how-it-works"
              className="border border-white text-white px-6 py-3 rounded-lg hover:bg-blue-700 font-medium flex items-center"
            >
              Learn More
              <FaChevronRight className="ml-2" />
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div>
              <h3 className="text-lg font-semibold mb-4">Judify</h3>
              <p className="text-gray-400 text-sm">
                Connecting students with expert tutors for better learning outcomes.
              </p>
            </div>
            <div>
              <h4 className="text-md font-semibold mb-4">Company</h4>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><Link to="/about" className="hover:text-white">About Us</Link></li>
                <li><Link to="/careers" className="hover:text-white">Careers</Link></li>
                <li><Link to="/blog" className="hover:text-white">Blog</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="text-md font-semibold mb-4">Support</h4>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><Link to="/help" className="hover:text-white">Help Center</Link></li>
                <li><Link to="/privacy" className="hover:text-white">Privacy Policy</Link></li>
                <li><Link to="/terms" className="hover:text-white">Terms of Service</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="text-md font-semibold mb-4">Connect</h4>
              <div className="flex space-x-4">
                <a href="#" className="text-gray-400 hover:text-white">
                  <span className="sr-only">Facebook</span>
                  <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                    <path fillRule="evenodd" d="M22 12c0-5.523-4.477-10-10-10S2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.878v-6.987h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.988C18.343 21.128 22 16.991 22 12z" clipRule="evenodd" />
                  </svg>
                </a>
                <a href="#" className="text-gray-400 hover:text-white">
                  <span className="sr-only">Twitter</span>
                  <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M8.29 20.251c7.547 0 11.675-6.253 11.675-11.675 0-.178 0-.355-.012-.53A8.348 8.348 0 0022 5.92a8.19 8.19 0 01-2.357.646 4.118 4.118 0 001.804-2.27 8.224 8.224 0 01-2.605.996 4.107 4.107 0 00-6.993 3.743 11.65 11.65 0 01-8.457-4.287 4.106 4.106 0 001.27 5.477A4.072 4.072 0 012.8 9.713v.052a4.105 4.105 0 003.292 4.022 4.095 4.095 0 01-1.853.07 4.108 4.108 0 003.834 2.85A8.233 8.233 0 012 18.407a11.616 11.616 0 006.29 1.84" />
                  </svg>
                </a>
                <a href="#" className="text-gray-400 hover:text-white">
                  <span className="sr-only">Instagram</span>
                  <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                    <path fillRule="evenodd" d="M12.315 2c2.43 0 2.784.013 3.808.06 1.064.049 1.791.218 2.427.465a4.902 4.902 0 011.772 1.153 4.902 4.902 0 011.153 1.772c.247.636.416 1.363.465 2.427.048 1.067.06 1.407.06 4.123v.08c0 2.643-.012 2.987-.06 4.043-.049 1.064-.218 1.791-.465 2.427a4.902 4.902 0 01-1.153 1.772 4.902 4.902 0 01-1.772 1.153c-.636.247-1.363.416-2.427.465-1.067.048-1.407.06-4.123.06h-.08c-2.643 0-2.987-.012-4.043-.06-1.064-.049-1.791-.218-2.427-.465a4.902 4.902 0 01-1.772-1.153 4.902 4.902 0 01-1.153-1.772c-.247-.636-.416-1.363-.465-2.427-.047-1.024-.06-1.379-.06-3.808v-.63c0-2.43.013-2.784.06-3.808.049-1.064.218-1.791.465-2.427a4.902 4.902 0 011.153-1.772A4.902 4.902 0 015.45 2.525c.636-.247 1.363-.416 2.427-.465C8.901 2.013 9.256 2 11.685 2h.63zm-.081 1.802h-.468c-2.456 0-2.784.011-3.807.058-.975.045-1.504.207-1.857.344-.467.182-.8.398-1.15.748-.35.35-.566.683-.748 1.15-.137.353-.3.882-.344 1.857-.047 1.023-.058 1.351-.058 3.807v.468c0 2.456.011 2.784.058 3.807.045.975.207 1.504.344 1.857.182.466.399.8.748 1.15.35.35.683.566 1.15.748.353.137.882.3 1.857.344 1.054.048 1.37.058 4.041.058h.08c2.597 0 2.917-.01 3.96-.058.976-.045 1.505-.207 1.858-.344.466-.182.8-.398 1.15-.748.35-.35.566-.683.748-1.15.137-.353.3-.882.344-1.857.048-1.055.058-1.37.058-4.041v-.08c0-2.597-.01-2.917-.058-3.96-.045-.976-.207-1.505-.344-1.858a3.097 3.097 0 00-.748-1.15 3.098 3.098 0 00-1.15-.748c-.353-.137-.882-.3-1.857-.344-1.023-.047-1.351-.058-3.807-.058zM12 6.865a5.135 5.135 0 110 10.27 5.135 5.135 0 010-10.27zm0 1.802a3.333 3.333 0 100 6.666 3.333 3.333 0 000-6.666zm5.338-3.205a1.2 1.2 0 110 2.4 1.2 1.2 0 010-2.4z" clipRule="evenodd" />
                  </svg>
                </a>
                <a href="#" className="text-gray-400 hover:text-white">
                  <span className="sr-only">LinkedIn</span>
                  <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z" />
                  </svg>
                </a>
              </div>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-gray-800 text-center">
            <p className="text-gray-400 text-sm">
              &copy; 2025 Judify. All rights reserved.
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage; 