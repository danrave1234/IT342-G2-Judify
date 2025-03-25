import { useState } from 'react';

const Earnings = () => {
  return (
    <div className="container mx-auto py-8">
      <h1 className="text-2xl font-bold mb-6">Earnings Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-sm text-gray-500 uppercase">Total Earnings</h2>
          <p className="text-3xl font-bold mt-2">$0.00</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-sm text-gray-500 uppercase">This Month</h2>
          <p className="text-3xl font-bold mt-2">$0.00</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-sm text-gray-500 uppercase">Pending Payments</h2>
          <p className="text-3xl font-bold mt-2">$0.00</p>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="p-6 border-b">
          <h2 className="text-lg font-semibold">Payment History</h2>
          <p className="text-gray-500 mt-2">Your recent payment transactions</p>
        </div>
        <div className="p-6 text-center">
          <p className="text-gray-500">No transaction history found.</p>
        </div>
      </div>
    </div>
  );
};

export default Earnings; 