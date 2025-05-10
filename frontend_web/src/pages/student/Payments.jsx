import { useState, useEffect } from 'react';
import { usePayment } from '../../context/PaymentContext';
import { FaDownload, FaFileInvoice, FaDollarSign, FaHistory } from 'react-icons/fa';
import { format } from 'date-fns';
import { useLocation } from 'react-router-dom';

const Payments = () => {
  const location = useLocation();
  // We'll use window.location.href instead of navigate for simplicity
  const { transactions, loading, error, fetchUserTransactions, processPayment } = usePayment();

  // Get tab from URL query parameter
  const queryParams = new URLSearchParams(location.search);
  const tabParam = queryParams.get('tab');

  const [activeTab, setActiveTab] = useState(tabParam === 'payment' ? 'payment' : 'history');
  const [filterStatus, setFilterStatus] = useState('all');
  const [cardNumber, setCardNumber] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [cvv, setCvv] = useState('');
  const [amount, setAmount] = useState(0);
  const [paymentResult, setPaymentResult] = useState(null);
  const [pendingSession, setPendingSession] = useState(null);

  // Load pending session from localStorage when component mounts
  useEffect(() => {
    const sessionData = localStorage.getItem('pendingSessionPayment');
    if (sessionData) {
      try {
        const parsedData = JSON.parse(sessionData);
        setPendingSession(parsedData);
        if (parsedData.price) {
          setAmount(parsedData.price);
        }
      } catch (err) {
        console.error('Error parsing pending session data:', err);
      }
    }
  }, []);

  const filteredTransactions = transactions?.filter(transaction => {
    if (filterStatus === 'all') return true;
    return transaction.status.toLowerCase() === filterStatus.toLowerCase();
  }) || [];

  const formatAmount = (amount) => {
    return `$${parseFloat(amount || 0).toFixed(2)}`;
  };

  const formatDate = (dateString) => {
    try {
      return format(new Date(dateString), 'MMM dd, yyyy');
    } catch {
      // Catch any errors without using the error parameter
      return 'Invalid date';
    }
  };

  const handleProcessPayment = async (e) => {
    e.preventDefault();

    try {
      // Process the payment using the API
      const result = await processPayment({
        amount: parseFloat(amount),
        description: pendingSession ? `${pendingSession.subject} Tutoring Session` : 'Tutoring Session Payment',
        tutorId: pendingSession?.tutorId,
        sessionId: pendingSession?.sessionId
      });

      setPaymentResult(result);

      // Clear the pending session from localStorage after successful payment
      if (pendingSession) {
        localStorage.removeItem('pendingSessionPayment');
      }

      setActiveTab('success');
    } catch (err) {
      console.error('Payment processing failed:', err);
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
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Description</th>
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

  const renderPaymentForm = () => (
    <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6">
      <h2 className="text-xl font-semibold text-gray-800 dark:text-white mb-6">Payment Processing</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div>
          <form onSubmit={handleProcessPayment}>
            <div className="mb-4">
              <label className="block text-gray-700 dark:text-gray-300 text-sm font-medium mb-2">
                Card Information
              </label>
              <input
                type="text"
                value={cardNumber}
                onChange={(e) => setCardNumber(e.target.value)}
                placeholder="**** **** **** 4242"
                className="w-full px-3 py-2 border border-gray-300 dark:border-dark-600 rounded-md"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-gray-700 dark:text-gray-300 text-sm font-medium mb-2">
                  Expiry Date
                </label>
                <input
                  type="text"
                  value={expiryDate}
                  onChange={(e) => setExpiryDate(e.target.value)}
                  placeholder="MM/YY"
                  className="w-full px-3 py-2 border border-gray-300 dark:border-dark-600 rounded-md"
                  required
                />
              </div>
              <div>
                <label className="block text-gray-700 dark:text-gray-300 text-sm font-medium mb-2">
                  CVV
                </label>
                <input
                  type="text"
                  value={cvv}
                  onChange={(e) => setCvv(e.target.value)}
                  placeholder="123"
                  className="w-full px-3 py-2 border border-gray-300 dark:border-dark-600 rounded-md"
                  required
                />
              </div>
            </div>

            <div className="mb-6">
              <label className="block text-gray-700 dark:text-gray-300 text-sm font-medium mb-2">
                Amount
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <span className="text-gray-500 dark:text-gray-400">$</span>
                </div>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  min="0"
                  step="0.01"
                  className="w-full pl-7 px-3 py-2 border border-gray-300 dark:border-dark-600 rounded-md"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-4 rounded-md transition-colors"
            >
              Process Payment
            </button>
          </form>
        </div>

        <div className="bg-gray-50 dark:bg-dark-700 p-6 rounded-lg">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">Payment Summary</h3>

          {pendingSession ? (
            <div>
              <div className="mb-4 p-3 bg-white dark:bg-dark-800 rounded-lg">
                <h4 className="font-medium text-gray-900 dark:text-white mb-2">
                  {pendingSession.subject} Session
                </h4>
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                  with {pendingSession.tutorName}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-500">
                  {formatDate(pendingSession.startTime)} • {new Date(pendingSession.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} • {pendingSession.duration} {pendingSession.duration === 1 ? 'hour' : 'hours'}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                  {pendingSession.isOnline ? 'Online Session' : 'In-person Session'}
                </p>
              </div>

              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">
                    Tutoring Session ({pendingSession.duration} {pendingSession.duration === 1 ? 'hour' : 'hours'})
                  </span>
                  <span className="text-gray-900 dark:text-white">
                    ${(pendingSession.price * 0.9).toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Platform Fee</span>
                  <span className="text-gray-900 dark:text-white">
                    ${(pendingSession.price * 0.1).toFixed(2)}
                  </span>
                </div>
                <div className="border-t border-gray-200 dark:border-dark-600 pt-2 mt-2">
                  <div className="flex justify-between font-medium">
                    <span className="text-gray-900 dark:text-white">Total</span>
                    <span className="text-gray-900 dark:text-white">${pendingSession.price.toFixed(2)}</span>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">No session selected</span>
                <span className="text-gray-900 dark:text-white">$0.00</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Platform Fee</span>
                <span className="text-gray-900 dark:text-white">$0.00</span>
              </div>
              <div className="border-t border-gray-200 dark:border-dark-600 pt-2 mt-2">
                <div className="flex justify-between font-medium">
                  <span className="text-gray-900 dark:text-white">Total</span>
                  <span className="text-gray-900 dark:text-white">${amount ? amount.toFixed(2) : '0.00'}</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  // Store session details for success message
  const [sessionDetails] = useState(() => {
    try {
      const data = localStorage.getItem('pendingSessionPayment');
      return data ? JSON.parse(data) : null;
    } catch (err) {
      console.error('Error parsing session details:', err);
      return null;
    }
  });

  const renderSuccessMessage = () => (
    <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6 text-center">
      <div className="w-20 h-20 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg className="w-10 h-10 text-green-500 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
        </svg>
      </div>

      <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-4">Payment Successful!</h2>
      <p className="text-gray-600 dark:text-gray-400 mb-6">Your transaction has been completed successfully</p>

      <div className="max-w-md mx-auto">
        <div className="bg-gray-50 dark:bg-dark-700 rounded-lg p-6 mb-6">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="text-left">
              <p className="text-gray-500 dark:text-gray-400">Transaction ID</p>
              <p className="font-medium text-gray-900 dark:text-white">#{paymentResult?.transactionReference || ''}</p>
            </div>
            <div className="text-left">
              <p className="text-gray-500 dark:text-gray-400">Date</p>
              <p className="font-medium text-gray-900 dark:text-white">{formatDate(paymentResult?.createdAt) || ''}</p>
            </div>
            <div className="text-left">
              <p className="text-gray-500 dark:text-gray-400">Amount Paid</p>
              <p className="font-medium text-gray-900 dark:text-white">{formatAmount(paymentResult?.amount) || ''}</p>
            </div>
            <div className="text-left">
              <p className="text-gray-500 dark:text-gray-400">Payment Method</p>
              <p className="font-medium text-gray-900 dark:text-white">Visa ending in {cardNumber.slice(-4)}</p>
            </div>
          </div>
        </div>

        <div className="bg-gray-50 dark:bg-dark-700 rounded-lg p-6 mb-8">
          <div className="flex items-center mb-3">
            <div className="w-10 h-10 rounded-full bg-gray-300 dark:bg-dark-600 mr-3"></div>
            <div>
              {sessionDetails ? (
                <>
                  <p className="font-medium text-gray-900 dark:text-white">{sessionDetails.subject} Session</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">with {sessionDetails.tutorName}</p>
                  <p className="text-xs text-gray-400 dark:text-gray-500">
                    {formatDate(sessionDetails.startTime)} • {new Date(sessionDetails.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} • {sessionDetails.duration} {sessionDetails.duration === 1 ? 'hour' : 'hours'}
                  </p>
                  <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                    {sessionDetails.isOnline ? 'Online Session' : 'In-person Session'}
                  </p>
                </>
              ) : (
                <>
                  <p className="font-medium text-gray-900 dark:text-white">Advanced Mathematics Session</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">with Prof. Michael Chen</p>
                  <p className="text-xs text-gray-400 dark:text-gray-500">March 18, 2025 • 2:00 PM EST • 90 minutes</p>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="flex space-x-4">
          <button 
            onClick={() => {
              setActiveTab('history');
              // Navigate to student dashboard or sessions page
              window.location.href = '/student/dashboard';
            }}
            className="flex-1 bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-4 rounded-md transition-colors"
          >
            Back to Home
          </button>
          <button 
            onClick={() => setActiveTab('history')}
            className="flex-1 bg-white dark:bg-dark-700 border border-gray-300 dark:border-dark-600 text-gray-700 dark:text-gray-300 font-medium py-2 px-4 rounded-md hover:bg-gray-50 dark:hover:bg-dark-600 transition-colors"
          >
            Payments & Transactions
          </button>
        </div>

        <p className="text-sm text-gray-500 dark:text-gray-400 mt-6">
          Need help? <a href="#" className="text-primary-600 dark:text-primary-500">Contact Support</a> or read our <a href="#" className="text-primary-600 dark:text-primary-500">FAQs</a>
        </p>

        <p className="text-xs text-gray-400 dark:text-gray-500 mt-6">
          A confirmation email has been sent to your registered email address
        </p>
      </div>
    </div>
  );

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Payment & Transactions</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">Manage your payments and view transaction history</p>
      </div>

      {activeTab !== 'success' && (
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
              onClick={() => setActiveTab('payment')}
              className={`pb-2 px-1 ${
                activeTab === 'payment'
                  ? 'border-b-2 border-primary-600 text-primary-600 dark:text-primary-500 font-medium'
                  : 'text-gray-500 dark:text-gray-400'
              }`}
            >
              <div className="flex items-center space-x-2">
                <FaDollarSign />
                <span>Payment Processing</span>
              </div>
            </button>
          </div>
        </div>
      )}

      {activeTab === 'history' && renderTransactionHistory()}
      {activeTab === 'payment' && renderPaymentForm()}
      {activeTab === 'success' && renderSuccessMessage()}
    </div>
  );
};

export default Payments; 
