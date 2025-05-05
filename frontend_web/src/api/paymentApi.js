import api from './baseApi';

// Payment Transactions API endpoints
export const paymentApi = {
  getTransactions: (params) => {
    // Ensure we're using the correct endpoint structure based on backend implementation
    // Remove userId parameter if it's not part of the API specification
    const cleanParams = { ...params };
    // Create a new copy of params without userId if both payerId and payeeId exist
    if (cleanParams.userId && (cleanParams.payerId || cleanParams.payeeId)) {
      delete cleanParams.userId;
    }
    console.log('Fetching payment transactions with params:', cleanParams);
    // Use more standard endpoint pattern
    return api.get('/payment-transactions', { params: cleanParams });
  },
  getTransactionById: (transactionId) => api.get(`/payment-transactions/${transactionId}`),
  createTransaction: (paymentData) => api.post('/payment-transactions', paymentData),
  updateTransaction: (transactionId, paymentData) => 
    api.put(`/payment-transactions/${transactionId}`, paymentData),
  // Add fallback methods that try both endpoint patterns
  getAllTransactions: (params) => {
    // Try the standard endpoint first
    return api.get('/payment-transactions', { params })
      .catch(error => {
        console.log('Error with /payment-transactions, trying /payments fallback');
        // Fall back to the old endpoint if the first one fails
        return api.get('/payments', { params });
      });
  }
}; 