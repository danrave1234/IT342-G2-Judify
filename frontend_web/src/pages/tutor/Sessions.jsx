import { useState } from 'react';

const Sessions = () => {
  return (
    <div className="container mx-auto py-8">
      <h1 className="text-2xl font-bold mb-6">Your Tutoring Sessions</h1>
      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="p-6 border-b">
          <h2 className="text-lg font-semibold">Sessions</h2>
          <p className="text-gray-500 mt-2">Manage your upcoming and past tutoring sessions</p>
        </div>
        <div className="p-6 text-center">
          <p className="text-gray-500">No sessions found. Your scheduled sessions will appear here.</p>
        </div>
      </div>
    </div>
  );
};

export default Sessions; 