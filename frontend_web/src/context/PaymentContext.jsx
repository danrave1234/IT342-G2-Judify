import { createContext, useContext, useState, useEffect } from 'react';
import { paymentApi } from '../api/api';
import { useUser } from './UserContext';
import { toast } from 'react-toastify';

const PaymentContext = createContext();

export const usePayment = () => useContext(PaymentContext);

// Mock transactions data
const MOCK_TRANSACTIONS = [
  {
    transactionId: 1,
    sessionId: 101,
    payerId: 3,
    payeeId: 2,
    amount: 130.00,
    status: 'COMPLETED',
    paymentStatus: 'COMPLETED',
    description: 'Math Tutoring Session',
    studentName: 'Alex Johnson',
    paymentGatewayReference: 'MOCK-12345',
    transactionReference: 'TRX-25789632',
    createdAt: '2025-01-15T10:30:00',
    updatedAt: '2025-01-15T10:35:00'
  },
  {
    transactionId: 2,
    sessionId: 102,
    payerId: 3,
    payeeId: 2,
    amount: 95.00,
    status: 'COMPLETED',
    paymentStatus: 'COMPLETED',
    description: 'Physics Tutoring Session',
    studentName: 'Alex Johnson',
    paymentGatewayReference: 'MOCK-12346',
    transactionReference: 'TRX-25789633',
    createdAt: '2025-01-10T14:00:00',
    updatedAt: '2025-01-10T14:05:00'
  },
  {
    transactionId: 3,
    sessionId: 103,
    payerId: 3,
    payeeId: 2,
    amount: 110.00,
    status: 'PENDING',
    paymentStatus: 'PENDING',
    description: 'Chemistry Tutoring Session',
    studentName: 'Alex Johnson',
    paymentGatewayReference: 'MOCK-12347',
    transactionReference: 'TRX-25789634',
    createdAt: '2025-01-05T09:15:00',
    updatedAt: '2025-01-05T09:20:00'
  }
];

export const PaymentProvider = ({ children }) => {
  const { user } = useUser();
  // Initialize with mock data immediately to avoid "loading" state
  const [transactions, setTransactions] = useState(MOCK_TRANSACTIONS);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentTransaction, setCurrentTransaction] = useState(null);

  // Filter transactions based on user role once user is available
  useEffect(() => {
    if (user?.userId) {
      // No need to show loading state for mock data
      filterTransactionsForUser();
    }
  }, [user]);

  // Filter mock transactions for the current user
  const filterTransactionsForUser = () => {
    try {
      // Filter mock transactions based on user role
      const userTransactions = user?.role === 'TUTOR' 
        ? MOCK_TRANSACTIONS.filter(t => t.payeeId === user.userId || t.payeeId === 2)
        : MOCK_TRANSACTIONS.filter(t => t.payerId === user.userId || t.payerId === 3);
      
      setTransactions(userTransactions);
    } catch (err) {
      console.error('Error filtering transactions:', err);
      // Set to all mock transactions as fallback
      setTransactions(MOCK_TRANSACTIONS);
    }
  };

  // Fetch transactions for the current user (using mock data for now)
  const fetchUserTransactions = () => {
    // Just refilter the transactions without loading state
    filterTransactionsForUser();
    return Promise.resolve(transactions);
  };

  // Create a new payment transaction (mock implementation)
  const createTransaction = async (paymentData) => {
    setLoading(true);
    setError(null);
    
    try {
      // Create a new mock transaction
      const newTransaction = {
        transactionId: Math.floor(Math.random() * 10000),
        sessionId: paymentData.sessionId || 100,
        payerId: paymentData.payerId || user.userId || 3,
        payeeId: paymentData.payeeId || 2,
        amount: paymentData.amount || 75.00,
        status: paymentData.status || 'COMPLETED',
        paymentStatus: paymentData.paymentStatus || 'COMPLETED',
        description: paymentData.description || 'Advanced Mathematics Session',
        studentName: 'Alex Johnson',
        paymentGatewayReference: paymentData.paymentGatewayReference || `MOCK-${Date.now()}`,
        transactionReference: paymentData.transactionReference || `TRX-${Math.floor(Math.random() * 10000000)}`,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      
      // Add to transactions list immediately
      setTransactions(prev => [newTransaction, ...prev]);
      setCurrentTransaction(newTransaction);
      
      toast.success('Payment processed successfully');
      return Promise.resolve(newTransaction);
      
    } catch (err) {
      console.error('Error creating transaction:', err);
      setError('Failed to process payment. Please try again.');
      toast.error('Payment processing failed');
      return Promise.reject(err);
    } finally {
      setLoading(false);
    }
  };

  // Get transaction details by ID (mock implementation)
  const getTransactionById = (transactionId) => {
    try {
      const transaction = transactions.find(t => t.transactionId === transactionId) || 
                        MOCK_TRANSACTIONS.find(t => t.transactionId === transactionId);
      
      if (!transaction) {
        throw new Error('Transaction not found');
      }
      
      setCurrentTransaction(transaction);
      return Promise.resolve(transaction);
      
    } catch (err) {
      console.error('Error fetching transaction details:', err);
      setError('Failed to load transaction details.');
      return Promise.reject(err);
    }
  };

  // Mock function to process a payment
  const processPayment = async (paymentDetails) => {
    setLoading(true);
    try {
      // Create a transaction reference
      const transactionRef = `TRX-${Math.floor(Math.random() * 10000000)}`;
      
      // Create transaction data
      const transactionData = {
        sessionId: paymentDetails.sessionId || 104,
        payerId: user?.userId || 3,
        payeeId: paymentDetails.tutorId || 2,
        amount: paymentDetails.amount || 75.00,
        status: 'COMPLETED',
        paymentStatus: 'COMPLETED',
        description: paymentDetails.description || 'Advanced Mathematics Session',
        studentName: 'Alex Johnson',
        paymentGatewayReference: `MOCK-${Date.now()}`,
        transactionReference: transactionRef
      };
      
      // Create the transaction
      const result = await createTransaction(transactionData);
      
      return {
        success: true,
        transactionId: result.transactionId,
        transactionReference: transactionRef,
        amount: result.amount,
        createdAt: result.createdAt
      };
    } catch (error) {
      console.error('Payment processing error:', error);
      toast.error('Payment processing failed');
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