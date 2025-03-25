import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  return (
    <div className="container mx-auto py-8">
      <h1 className="text-2xl font-bold mb-6">Student Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Upcoming Sessions</h2>
          <p className="text-gray-500">You have no upcoming tutoring sessions.</p>
          <Link to="/student/find-tutors" className="text-blue-600 font-medium mt-4 inline-block">
            Find a tutor
          </Link>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Recent Tutors</h2>
          <p className="text-gray-500">No recent tutors to display.</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Account Status</h2>
          <p className="text-gray-500">Complete your profile to get better tutor matches.</p>
          <Link to="/student/profile" className="text-blue-600 font-medium mt-4 inline-block">
            Edit profile
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 