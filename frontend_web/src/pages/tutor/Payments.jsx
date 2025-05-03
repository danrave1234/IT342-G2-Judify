import { useState, useEffect } from 'react';
import { usePayment } from '../../context/PaymentContext';
import { useUser } from '../../context/UserContext';
import { FaDownload, FaFileInvoice, FaChartLine, FaCalendarAlt, FaHistory, FaCreditCard, FaExchangeAlt, FaMoneyBillWave, FaSearchDollar } from 'react-icons/fa';
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
    <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden">
      <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white flex items-center">
          <FaHistory className="mr-2 text-primary-600 dark:text-primary-400" /> 
          Transaction History
        </h2>
        <div className="flex flex-wrap gap-3">
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="border border-gray-300 dark:border-dark-600 rounded-md px-3 py-2 text-sm bg-white dark:bg-dark-800 text-gray-700 dark:text-gray-300 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="all">All Transactions</option>
            <option value="completed">Completed</option>
            <option value="pending">Pending</option>
            <option value="refunded">Refunded</option>
          </select>
          <button className="flex items-center gap-2 bg-gray-100 hover:bg-gray-200 dark:bg-dark-700 dark:hover:bg-dark-600 text-gray-700 dark:text-gray-300 px-3 py-2 rounded-md text-sm transition-colors">
            <FaDownload />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-16">
          <div className="w-16 h-16 border-4 border-gray-200 border-t-primary-600 rounded-full animate-spin"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Loading transactions...</p>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center py-16">
          <div className="w-16 h-16 bg-red-100 dark:bg-red-900/20 rounded-full flex items-center justify-center text-red-500 mb-4">
            <FaExchangeAlt size={24} />
          </div>
          <p className="text-red-500 dark:text-red-400 mb-4">{error}</p>
          <button
            onClick={fetchUserTransactions}
            className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-md transition-colors"
          >
            Try Again
          </button>
        </div>
      ) : filteredTransactions.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 bg-gray-100 dark:bg-dark-700 rounded-full flex items-center justify-center text-gray-400 dark:text-gray-500 mb-4">
            <FaHistory size={24} />
          </div>
          <p className="text-gray-600 dark:text-gray-400 mb-2">No transactions found.</p>
          <p className="text-sm text-gray-500 dark:text-gray-500 max-w-md">
            Transactions will appear here after students book and pay for sessions with you.
          </p>
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
                <tr key={transaction.transactionId} className="hover:bg-gray-50 dark:hover:bg-dark-700 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    {formatDate(transaction.createdAt)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="h-8 w-8 rounded-full bg-gray-200 dark:bg-dark-600 flex items-center justify-center text-gray-600 dark:text-gray-400 mr-3">
                        {transaction.studentName?.[0] || 'S'}
                      </div>
                      <span className="text-sm text-gray-900 dark:text-white">{transaction.studentName || 'Student'}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    {transaction.description || 'Tutoring Session'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2.5 py-1 text-xs font-medium rounded-full ${
                      transaction.status === 'COMPLETED' ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300' :
                      transaction.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300' :
                      'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300'
                    }`}>
                      {transaction.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-gray-900 dark:text-white">
                    {formatAmount(transaction.amount)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                    <button className="text-primary-600 dark:text-primary-500 hover:text-primary-800 dark:hover:text-primary-400 p-1">
                      <FaFileInvoice size={16} />
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
    <div className="space-y-8">
      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden">
        <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white flex items-center">
            <FaChartLine className="mr-2 text-primary-600 dark:text-primary-400" /> 
            Earnings Analytics
          </h2>
          <div>
            <select
              value={period}
              onChange={(e) => setPeriod(e.target.value)}
              className="border border-gray-300 dark:border-dark-600 rounded-md px-3 py-2 text-sm bg-white dark:bg-dark-800 text-gray-700 dark:text-gray-300 focus:ring-primary-500 focus:border-primary-500"
            >
              <option value="week">This Week</option>
              <option value="month">This Month</option>
              <option value="year">This Year</option>
              <option value="all">All Time</option>
            </select>
          </div>
        </div>

        <div className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <div className="bg-gradient-to-br from-primary-50 to-primary-100 dark:from-primary-900/20 dark:to-primary-900/10 p-6 rounded-lg">
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-lg bg-primary-200 dark:bg-primary-800 flex items-center justify-center text-primary-700 dark:text-primary-300 mr-3">
                  <FaMoneyBillWave size={20} />
                </div>
                <div>
                  <h3 className="text-sm font-medium text-primary-800 dark:text-primary-300">Gross Earnings</h3>
                  <p className="text-2xl font-bold text-primary-900 dark:text-primary-100 mt-1">{formatAmount(totalEarnings)}</p>
                  <p className="text-xs text-primary-600 dark:text-primary-400 mt-1">+12% from last period</p>
                </div>
              </div>
            </div>

            <div className="bg-gradient-to-br from-red-50 to-red-100 dark:from-red-900/20 dark:to-red-900/10 p-6 rounded-lg">
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-lg bg-red-200 dark:bg-red-800 flex items-center justify-center text-red-700 dark:text-red-300 mr-3">
                  <FaSearchDollar size={20} />
                </div>
                <div>
                  <h3 className="text-sm font-medium text-red-800 dark:text-red-300">Platform Fees</h3>
                  <p className="text-2xl font-bold text-red-900 dark:text-red-100 mt-1">{formatAmount(platformFees)}</p>
                  <p className="text-xs text-red-600 dark:text-red-400 mt-1">10% of gross earnings</p>
                </div>
              </div>
            </div>

            <div className="bg-gradient-to-br from-green-50 to-green-100 dark:from-green-900/20 dark:to-green-900/10 p-6 rounded-lg">
              <div className="flex items-start">
                <div className="w-10 h-10 rounded-lg bg-green-200 dark:bg-green-800 flex items-center justify-center text-green-700 dark:text-green-300 mr-3">
                  <FaCreditCard size={20} />
                </div>
                <div>
                  <h3 className="text-sm font-medium text-green-800 dark:text-green-300">Net Earnings</h3>
                  <p className="text-2xl font-bold text-green-900 dark:text-green-100 mt-1">{formatAmount(netEarnings)}</p>
                  <p className="text-xs text-green-600 dark:text-green-400 mt-1">Available for withdrawal</p>
                </div>
              </div>
            </div>
          </div>

          <div className="mb-6">
            <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">Monthly Earnings</h3>
            <div className="h-60 bg-gray-50 dark:bg-dark-700 rounded-lg flex items-center justify-center">
              <div className="text-center">
                <FaChartLine className="mx-auto h-10 w-10 text-gray-400 dark:text-gray-500" />
                <p className="mt-2 text-gray-500 dark:text-gray-400">
                  No earnings data available for the selected period
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden">
        <div className="px-6 py-5 border-b border-gray-200 dark:border-dark-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
            <FaCreditCard className="mr-2 text-primary-600 dark:text-primary-400" /> 
            Payment Methods
          </h2>
        </div>
        <div className="p-6">
          <div className="mb-6 p-5 bg-gray-50 dark:bg-dark-700 rounded-lg">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
              <div className="flex items-center">
                <div className="w-12 h-8 bg-blue-600 dark:bg-blue-700 rounded-md mr-3 flex items-center justify-center text-white font-bold text-sm">
                  Visa
                </div>
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">Direct Deposit</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Payments are processed every 14 days</p>
                </div>
              </div>
              <button className="text-primary-600 dark:text-primary-500 hover:text-primary-800 dark:hover:text-primary-300 text-sm font-medium">
                Update
              </button>
            </div>
          </div>

          <div className="pt-4 border-t border-gray-200 dark:border-dark-600">
            <button className="w-full text-center py-2 border border-primary-600 dark:border-primary-500 text-primary-600 dark:text-primary-500 rounded-md hover:bg-primary-50 dark:hover:bg-primary-900/10 transition-colors font-medium">
              Set Up Alternative Payment Method
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Payments & Earnings</h1>
        <p className="mt-2 text-lg text-gray-600 dark:text-gray-400">Track your earnings and manage payment information</p>
      </div>
      
      <div className="mb-6">
        <div className="flex flex-wrap border-b border-gray-200 dark:border-dark-700">
          <button
            onClick={() => setActiveTab('history')}
            className={`pb-3 px-4 font-medium text-sm ${
              activeTab === 'history'
                ? 'border-b-2 border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            } transition-colors`}
          >
            <div className="flex items-center space-x-2">
              <FaHistory />
              <span>Transaction History</span>
            </div>
          </button>
          <button
            onClick={() => setActiveTab('earnings')}
            className={`pb-3 px-4 font-medium text-sm ${
              activeTab === 'earnings'
                ? 'border-b-2 border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            } transition-colors`}
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