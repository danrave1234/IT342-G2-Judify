import { createContext, useContext, useState, useEffect } from 'react';
import { paymentApi } from '../api/api';
import { useUser } from './UserContext';
import { toast } from 'react-toastify';

const PaymentContext = createContext();

export const usePayment = () => useContext(PaymentContext);

// No mock transactions - we'll fetch real data from the API

export const PaymentProvider = ({ children }) => {
  const { user } = useUser();
  // Initialize with empty array - we'll fetch real data from API
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentTransaction, setCurrentTransaction] = useState(null);

  // Fetch transactions when user is available
  useEffect(() => {
    if (user?.userId) {
      fetchUserTransactions();
    }
  }, [user]);

  // Fetch transactions for the current user from the API
  const fetchUserTransactions = async () => {
    if (!user?.userId) {
      console.error('Cannot fetch transactions: No user ID available');
      return Promise.reject(new Error('User ID is required'));
    }

    setLoading(true);
    setError(null);

    try {
      // Use the role-agnostic getAllTransactions method that tries multiple endpoints
      const params = {};
      
      // Add role-specific parameters
      if (user?.role === 'TUTOR') {
        params.payeeId = user.userId;
      } else {
        params.payerId = user.userId;
      }
      
      console.log('Fetching transactions with params:', params);
      const response = await paymentApi.getAllTransactions(params);

      if (response && response.data) {
        console.log(`Successfully loaded ${response.data.length} transactions`);
        setTransactions(response.data);
        return response.data;
      }

      // If we get here with no data, set empty array
      console.log('No transactions found');
      setTransactions([]);
      return [];
    } catch (err) {
      console.error('Error fetching transactions:', err);
      setError('Failed to load transactions. Please try again.');
      // Don't show error toast for 404s which are expected for new users
      if (!err.response || err.response.status !== 404) {
        toast.error('Failed to load transactions');
      }
      // Return empty array instead of rejecting, to prevent app from breaking
      setTransactions([]);
      return [];
    } finally {
      setLoading(false);
    }
  };

  // Create a new payment transaction using the API
  const createTransaction = async (paymentData) => {
    if (!user?.userId) {
      console.error('Cannot create transaction: No user ID available');
      toast.error('User authentication required to process payment');
      return Promise.reject(new Error('User ID is required'));
    }

    setLoading(true);
    setError(null);

    try {
      // Ensure required fields are present
      if (!paymentData.sessionId) {
        throw new Error('Session ID is required for payment');
      }

      if (!paymentData.amount || paymentData.amount <= 0) {
        throw new Error('Valid payment amount is required');
      }

      // Create transaction data with user ID
      const transactionData = {
        ...paymentData,
        payerId: paymentData.payerId || user.userId,
      };

      // Call the API to create the transaction
      const response = await paymentApi.createTransaction(transactionData);

      if (response && response.data) {
        // Add to transactions list and set as current
        setTransactions(prev => [response.data, ...prev]);
        setCurrentTransaction(response.data);

        toast.success('Payment processed successfully');
        return response.data;
      } else {
        throw new Error('Invalid response from payment service');
      }
    } catch (err) {
      console.error('Error creating transaction:', err);
      setError('Failed to process payment. Please try again.');
      toast.error(err.message || 'Payment processing failed');
      return Promise.reject(err);
    } finally {
      setLoading(false);
    }
  };

  // Get transaction details by ID using the API
  const getTransactionById = async (transactionId) => {
    if (!transactionId) {
      console.error('Cannot get transaction: No transaction ID provided');
      return Promise.reject(new Error('Transaction ID is required'));
    }

    setLoading(true);
    setError(null);

    try {
      // First check if we already have it in state
      const cachedTransaction = transactions.find(t => t.transactionId === transactionId);

      if (cachedTransaction) {
        setCurrentTransaction(cachedTransaction);
        return cachedTransaction;
      }

      // Otherwise fetch from API
      const response = await paymentApi.getTransactionById(transactionId);

      if (response && response.data) {
        setCurrentTransaction(response.data);
        return response.data;
      } else {
        throw new Error('Transaction not found');
      }
    } catch (err) {
      console.error('Error fetching transaction details:', err);
      setError('Failed to load transaction details.');
      return Promise.reject(err);
    } finally {
      setLoading(false);
    }
  };

  // Process a payment using the API
  const processPayment = async (paymentDetails) => {
    if (!user?.userId) {
      console.error('Cannot process payment: No user ID available');
      toast.error('User authentication required to process payment');
      return { success: false, error: 'User authentication required' };
    }

    setLoading(true);
    try {
      // Validate payment details
      if (!paymentDetails.sessionId) {
        throw new Error('Session ID is required for payment');
      }

      if (!paymentDetails.amount || paymentDetails.amount <= 0) {
        throw new Error('Valid payment amount is required');
      }

      if (!paymentDetails.tutorId) {
        throw new Error('Tutor ID is required for payment');
      }

      // Create transaction data
      const transactionData = {
        sessionId: paymentDetails.sessionId,
        payerId: user.userId,
        payeeId: paymentDetails.tutorId,
        amount: paymentDetails.amount,
        status: 'COMPLETED',
        paymentStatus: 'COMPLETED',
        description: paymentDetails.description || 'Tutoring Session',
      };

      // Create the transaction using the API
      const result = await createTransaction(transactionData);

      return {
        success: true,
        transactionId: result.transactionId,
        transactionReference: result.transactionReference,
        amount: result.amount,
        createdAt: result.createdAt
      };
    } catch (error) {
      console.error('Payment processing error:', error);
      toast.error(error.message || 'Payment processing failed');
      return { success: false, error: error.message };
    } finally {
      setLoading(false);
    }
  };

  const value = {
    transactions,
    loading,
    error,
    currentTransaction,
    fetchUserTransactions,
    createTransaction,
    getTransactionById,
    processPayment
  };

  return (
    <PaymentContext.Provider value={value}>
      {children}
    </PaymentContext.Provider>
  );
};

export default PaymentContext; 
