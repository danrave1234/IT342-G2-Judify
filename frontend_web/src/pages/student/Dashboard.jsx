import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaSearch, FaBook, FaCalendarAlt, FaUser } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';

const Dashboard = () => {
  const { user } = useUser();
  const [featuredSubjects, setFeaturedSubjects] = useState([
    { id: 1, name: 'Mathematics', tutors: 15 },
    { id: 2, name: 'Computer Science', tutors: 8 },
    { id: 3, name: 'Physics', tutors: 10 },
    { id: 4, name: 'English', tutors: 12 }
  ]);

  return (
    <div className="container mx-auto py-8">
      {/* Welcome Section */}
      <div className="bg-white dark:bg-dark-800 p-8 rounded-lg shadow-md mb-8">
        <h1 className="text-3xl font-bold mb-4">Welcome back, {user?.firstName || 'Student'}!</h1>
        <p className="text-gray-600 dark:text-gray-300 mb-6">Find expert tutors, schedule sessions, and improve your skills with Judify.</p>
        
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Link to="/student/find-tutors" className="flex flex-col items-center p-4 bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500 rounded-lg hover:bg-primary-100 dark:hover:bg-primary-900/30 transition-colors">
            <FaSearch className="text-3xl mb-2" />
            <span className="font-medium">Find Tutors</span>
          </Link>
          
          <Link to="/student/sessions" className="flex flex-col items-center p-4 bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-500 rounded-lg hover:bg-green-100 dark:hover:bg-green-900/30 transition-colors">
            <FaBook className="text-3xl mb-2" />
            <span className="font-medium">Browse Subjects</span>
          </Link>

          <Link to="/student/find-tutors" className="flex flex-col items-center p-4 bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-500 rounded-lg hover:bg-purple-100 dark:hover:bg-purple-900/30 transition-colors">
            <FaCalendarAlt className="text-3xl mb-2" />
            <span className="font-medium">Find More Tutors</span>
          </Link>
          
          <Link to="/student/profile" className="flex flex-col items-center p-4 bg-orange-50 dark:bg-orange-900/20 text-orange-600 dark:text-orange-500 rounded-lg hover:bg-orange-100 dark:hover:bg-orange-900/30 transition-colors">
            <FaUser className="text-3xl mb-2" />
            <span className="font-medium">My Profile</span>
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Upcoming Sessions */}
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-semibold mb-4 flex items-center">
            <FaCalendarAlt className="mr-2 text-primary-600 dark:text-primary-500" />
            Upcoming Sessions
          </h2>
          <p className="text-gray-500 dark:text-gray-400">You have no upcoming tutoring sessions.</p>
          <Link to="/student/find-tutors" className="text-primary-600 dark:text-primary-500 font-medium mt-4 inline-block hover:underline">
            Find a tutor
          </Link>
        </div>

        {/* Featured Subjects */}
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-semibold mb-4 flex items-center">
            <FaBook className="mr-2 text-primary-600 dark:text-primary-500" />
            Featured Subjects
          </h2>
          {featuredSubjects.length > 0 ? (
            <ul className="space-y-2">
              {featuredSubjects.map(subject => (
                <li key={subject.id} className="flex justify-between items-center p-2 hover:bg-gray-50 dark:hover:bg-dark-700 rounded">
                  <span>{subject.name}</span>
                  <span className="text-sm text-gray-500 dark:text-gray-400">{subject.tutors} tutors</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-gray-500 dark:text-gray-400">No featured subjects to display.</p>
          )}
          <Link to="/student/sessions" className="text-primary-600 dark:text-primary-500 font-medium mt-4 inline-block hover:underline">
            View all subjects
          </Link>
        </div>

        {/* Account Status */}
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-semibold mb-4 flex items-center">
            <FaUser className="mr-2 text-primary-600 dark:text-primary-500" />
            Account Status
          </h2>
          <div className="space-y-4">
            <div className="h-2 bg-gray-200 dark:bg-dark-600 rounded-full">
              <div className="h-full bg-primary-600 dark:bg-primary-500 rounded-full" style={{ width: "40%" }}></div>
            </div>
            <p className="text-gray-500 dark:text-gray-400">Complete your profile to get better tutor matches.</p>
          </div>
          <Link to="/student/profile" className="text-primary-600 dark:text-primary-500 font-medium mt-4 inline-block hover:underline">
            Complete profile
          </Link>
        </div>
      </div>

      {/* Platform Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-8">
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-md text-center">
          <div className="text-3xl font-bold text-primary-600 dark:text-primary-500">50+</div>
          <div className="text-gray-500 dark:text-gray-400">Expert Tutors</div>
        </div>
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-md text-center">
          <div className="text-3xl font-bold text-primary-600 dark:text-primary-500">20+</div>
          <div className="text-gray-500 dark:text-gray-400">Subjects</div>
        </div>
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-md text-center">
          <div className="text-3xl font-bold text-primary-600 dark:text-primary-500">100+</div>
          <div className="text-gray-500 dark:text-gray-400">Sessions Completed</div>
        </div>
        <div className="bg-white dark:bg-dark-800 p-6 rounded-lg shadow-md text-center">
          <div className="text-3xl font-bold text-primary-600 dark:text-primary-500">4.8</div>
          <div className="text-gray-500 dark:text-gray-400">Average Rating</div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 