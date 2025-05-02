import { useState } from 'react';
import { FaChartBar, FaMoneyBillWave, FaCreditCard, FaWallet, FaDownload, FaCalendarAlt } from 'react-icons/fa';
import { Link } from 'react-router-dom';

const Earnings = () => {
  const [selectedPeriod, setSelectedPeriod] = useState('month');

  const periods = [
    { value: 'week', label: 'This Week' },
    { value: 'month', label: 'This Month' },
    { value: 'quarter', label: 'This Quarter' },
    { value: 'year', label: 'This Year' },
  ];

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Earnings Dashboard</h1>
          <p className="mt-2 text-lg text-gray-600 dark:text-gray-400">Track and manage your tutoring income</p>
        </div>
        <div className="mt-4 md:mt-0 flex space-x-3">
          <select
            value={selectedPeriod}
            onChange={(e) => setSelectedPeriod(e.target.value)}
            className="border border-gray-300 dark:border-dark-600 rounded-md shadow-sm p-2 bg-white dark:bg-dark-800 text-gray-700 dark:text-gray-200 focus:ring-primary-500 focus:border-primary-500"
          >
            {periods.map(period => (
              <option key={period.value} value={period.value}>{period.label}</option>
            ))}
          </select>
          <button className="inline-flex items-center bg-gray-100 dark:bg-dark-700 px-4 py-2 rounded-md text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-dark-600 transition-colors">
            <FaDownload className="mr-2" /> Export
          </button>
        </div>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 p-6">
          <div className="flex items-start">
            <div className="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600 dark:text-green-400 mr-4">
              <FaMoneyBillWave size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Total Earnings</p>
              <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">$0.00</p>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                <span className="text-green-500 dark:text-green-400">+0%</span> from previous period
              </p>
            </div>
          </div>
        </div>
        
        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 p-6">
          <div className="flex items-start">
            <div className="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400 mr-4">
              <FaCalendarAlt size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">This Month</p>
              <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">$0.00</p>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                From 0 completed sessions
              </p>
            </div>
          </div>
        </div>
        
        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 p-6">
          <div className="flex items-start">
            <div className="w-12 h-12 rounded-lg bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center text-purple-600 dark:text-purple-400 mr-4">
              <FaWallet size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Pending Payments</p>
              <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">$0.00</p>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                From 0 pending sessions
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden mb-8">
        <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
            <FaChartBar className="mr-2 text-primary-600 dark:text-primary-400" /> 
            Earnings Overview
          </h2>
          <div className="text-sm text-gray-500 dark:text-gray-400">
            Showing data for: {periods.find(p => p.value === selectedPeriod)?.label}
          </div>
        </div>
        <div className="p-6">
          <div className="h-64 bg-gray-50 dark:bg-dark-700 rounded-lg flex items-center justify-center">
            <div className="text-center">
              <FaChartBar className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" />
              <p className="mt-2 text-gray-500 dark:text-gray-400">
                No earnings data available for this period
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden">
        <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
            <FaCreditCard className="mr-2 text-primary-600 dark:text-primary-400" /> 
            Payment Transactions
          </h2>
        </div>
        <div className="p-6">
          <div className="text-center py-8">
            <FaMoneyBillWave className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" />
            <p className="mt-4 text-gray-500 dark:text-gray-400">No transaction history found.</p>
            <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
              Completed sessions will appear here once payment has been processed.
            </p>
            <div className="mt-6">
              <Link 
                to="/tutor/availability" 
                className="inline-flex items-center px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white text-sm font-medium rounded-md transition-colors"
              >
                Set Your Availability
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Earnings; 