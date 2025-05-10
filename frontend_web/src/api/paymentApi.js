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
  },
  
  // Stripe-specific methods
  createPaymentIntent: (sessionData) => {
    console.log('Creating payment intent for session:', sessionData);
    return api.post('/payments/create-payment-intent', sessionData);
  },
  
  // Method to confirm payment after Stripe checkout
  confirmPayment: (sessionId, paymentIntentId) => {
    console.log(`Confirming payment for session ${sessionId} with paymentIntentId ${paymentIntentId}`);
    return api.post(`/payments/confirm-payment`, { 
      sessionId, 
      paymentIntentId 
    });
  },
  
  // Get payment status
  getPaymentStatus: (sessionId) => {
    console.log(`Checking payment status for session ${sessionId}`);
    return api.get(`/payments/status/${sessionId}`);
  },
  
  // Process refund
  processRefund: (transactionId, reason) => {
    console.log(`Processing refund for transaction ${transactionId}`);
    return api.post(`/payments/processRefund/${transactionId}`, { reason });
  }
}; 