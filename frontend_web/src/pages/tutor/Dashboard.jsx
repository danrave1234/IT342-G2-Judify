import { useState, useEffect } from 'react';

const Dashboard = () => {
  return (
    <div className="container mx-auto py-8">
      <h1 className="text-2xl font-bold mb-6">Tutor Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Upcoming Sessions</h2>
          <p className="text-gray-500">You have no upcoming sessions.</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Recent Earnings</h2>
          <p className="text-gray-500">No recent earnings to display.</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Profile Completion</h2>
          <p className="text-gray-500">Complete your profile to attract more students.</p>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 