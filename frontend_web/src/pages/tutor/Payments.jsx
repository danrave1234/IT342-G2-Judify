import { useState, useEffect } from 'react';
import { usePayment } from '../../context/PaymentContext';
import { useUser } from '../../context/UserContext';
import { FaDownload, FaFileInvoice, FaChartLine, FaCalendarAlt, FaHistory } from 'react-icons/fa';
import { format } from 'date-fns';

const Payments = () => {
  const { user } = useUser();
  const { transactions, loading, error, fetchUserTransactions } = usePayment();
  const [activeTab, setActiveTab] = useState('history');
  const [filterStatus, setFilterStatus] = useState('all');
  const [period, setPeriod] = useState('month');

  // No need for useEffect since transactions are initialized with mock data

  const filteredTransactions = transactions?.filter(transaction => {
    if (filterStatus === 'all') return true;
    return transaction.status.toLowerCase() === filterStatus.toLowerCase();
  }) || [];

  // Calculate total earnings based on completed transactions
  const totalEarnings = filteredTransactions
    .filter(transaction => transaction.status === 'COMPLETED')
    .reduce((sum, transaction) => sum + transaction.amount, 0);
    
  // Calculate platform fees (assuming 10% of each transaction)
  const platformFees = totalEarnings * 0.1;
  
  // Calculate net earnings after platform fees
  const netEarnings = totalEarnings - platformFees;

  const formatAmount = (amount) => {
    return `$${parseFloat(amount || 0).toFixed(2)}`;
  };

  const formatDate = (dateString) => {
    try {
      return format(new Date(dateString), 'MMM dd, yyyy');
    } catch (e) {
      return 'Invalid date';
    }
  };

  const renderTransactionHistory = () => (
    <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-800 dark:text-white">Transaction History</h2>
        <div className="flex space-x-4">
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="border border-gray-300 dark:border-dark-600 rounded-md px-3 py-1.5 text-sm bg-white dark:bg-dark-800 text-gray-700 dark:text-gray-300"
          >
            <option value="all">All Transactions</option>
            <option value="completed">Completed</option>
            <option value="pending">Pending</option>
            <option value="refunded">Refunded</option>
          </select>
          <button className="flex items-center space-x-2 bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-500 px-3 py-1.5 rounded-md text-sm">
            <FaDownload />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-8">
          <div className="w-12 h-12 border-t-4 border-primary-600 border-solid rounded-full animate-spin mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading transactions...</p>
        </div>
      ) : error ? (
        <div className="text-center py-8">
          <p className="text-red-500">{error}</p>
          <button
            onClick={fetchUserTransactions}
            className="mt-4 px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      ) : filteredTransactions.length === 0 ? (
        <div className="text-center py-8">
          <FaHistory className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600 dark:text-gray-400">No transactions found.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-dark-700">
            <thead className="bg-gray-50 dark:bg-dark-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Date</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Student</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Session</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Amount</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-dark-800 divide-y divide-gray-200 dark:divide-dark-700">
              {filteredTransactions.map((transaction) => (
                <tr key={transaction.transactionId} className="hover:bg-gray-50 dark:hover:bg-dark-700">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    {formatDate(transaction.createdAt)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    {transaction.studentName || 'Student'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    {transaction.description || 'Tutoring Session'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      transaction.status === 'COMPLETED' ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' :
                      transaction.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200' :
                      'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                    }`}>
                      {transaction.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900 dark:text-white">
                    {formatAmount(transaction.amount)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button className="text-primary-600 dark:text-primary-500 hover:text-primary-800 dark:hover:text-primary-400">
                      <FaFileInvoice />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );

  const renderEarningsAnalytics = () => (
    <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-800 dark:text-white">Earnings Analytics</h2>
        <div className="flex space-x-4">
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="border border-gray-300 dark:border-dark-600 rounded-md px-3 py-1.5 text-sm bg-white dark:bg-dark-800 text-gray-700 dark:text-gray-300"
          >
            <option value="week">This Week</option>
            <option value="month">This Month</option>
            <option value="year">This Year</option>
            <option value="all">All Time</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-gradient-to-br from-primary-50 to-primary-100 dark:from-primary-900/20 dark:to-primary-900/10 p-6 rounded-lg">
          <h3 className="text-sm font-medium text-primary-800 dark:text-primary-300 mb-2">Gross Earnings</h3>
          <p className="text-2xl font-bold text-primary-900 dark:text-primary-100">{formatAmount(totalEarnings)}</p>
          <p className="text-sm text-primary-600 dark:text-primary-400 mt-1">+12% from last period</p>
        </div>

        <div className="bg-gradient-to-br from-red-50 to-red-100 dark:from-red-900/20 dark:to-red-900/10 p-6 rounded-lg">
          <h3 className="text-sm font-medium text-red-800 dark:text-red-300 mb-2">Platform Fees</h3>
          <p className="text-2xl font-bold text-red-900 dark:text-red-100">{formatAmount(platformFees)}</p>
          <p className="text-sm text-red-600 dark:text-red-400 mt-1">10% of gross earnings</p>
        </div>

        <div className="bg-gradient-to-br from-green-50 to-green-100 dark:from-green-900/20 dark:to-green-900/10 p-6 rounded-lg">
          <h3 className="text-sm font-medium text-green-800 dark:text-green-300 mb-2">Net Earnings</h3>
          <p className="text-2xl font-bold text-green-900 dark:text-green-100">{formatAmount(netEarnings)}</p>
          <p className="text-sm text-green-600 dark:text-green-400 mt-1">Available for withdrawal</p>
        </div>
      </div>

      <div className="mb-6">
        <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">Earnings Breakdown</h3>
        <div className="h-60 bg-gray-100 dark:bg-dark-700 rounded-lg flex items-center justify-center">
          <p className="text-gray-500 dark:text-gray-400">Chart visualization would go here</p>
        </div>
      </div>

      <div>
        <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">Payment Methods</h3>
        <div className="bg-gray-50 dark:bg-dark-700 p-6 rounded-lg">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center">
              <div className="w-12 h-8 bg-blue-600 rounded mr-3 flex items-center justify-center text-white">
                Visa
              </div>
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Direct Deposit</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">Payments are sent every 14 days</p>
              </div>
            </div>
            <button className="text-primary-600 dark:text-primary-500 text-sm">Update</button>
          </div>

          <div className="pt-4 border-t border-gray-200 dark:border-dark-600">
            <button className="w-full text-center py-2 border border-primary-600 text-primary-600 dark:text-primary-500 rounded-md hover:bg-primary-50 dark:hover:bg-primary-900/10 transition-colors">
              Set Up Alternative Payment Method
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Payments & Earnings</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">Track your earnings and manage payment information</p>
      </div>
      
      <div className="mb-6">
        <div className="flex space-x-4 border-b border-gray-200 dark:border-dark-700">
          <button
            onClick={() => setActiveTab('history')}
            className={`pb-2 px-1 ${
              activeTab === 'history'
                ? 'border-b-2 border-primary-600 text-primary-600 dark:text-primary-500 font-medium'
                : 'text-gray-500 dark:text-gray-400'
            }`}
          >
            <div className="flex items-center space-x-2">
              <FaHistory />
              <span>Transaction History</span>
            </div>
          </button>
          <button
            onClick={() => setActiveTab('earnings')}
            className={`pb-2 px-1 ${
              activeTab === 'earnings'
                ? 'border-b-2 border-primary-600 text-primary-600 dark:text-primary-500 font-medium'
                : 'text-gray-500 dark:text-gray-400'
            }`}
          >
            <div className="flex items-center space-x-2">
              <FaChartLine />
              <span>Earnings Analytics</span>
            </div>
          </button>
        </div>
      </div>
      
      {activeTab === 'history' && renderTransactionHistory()}
      {activeTab === 'earnings' && renderEarningsAnalytics()}
    </div>
  );
};

export default Payments; 