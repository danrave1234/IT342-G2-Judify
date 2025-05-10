import { useState, useEffect } from 'react';
import { useStripe, useElements, CardElement } from '@stripe/react-stripe-js';
import { paymentApi } from '../../api/api';
import { toast } from 'react-toastify';

const CARD_ELEMENT_OPTIONS = {
  style: {
    base: {
      color: '#32325d',
      fontFamily: '"Helvetica Neue", Helvetica, sans-serif',
      fontSmoothing: 'antialiased',
      fontSize: '16px',
      '::placeholder': {
        color: '#aab7c4'
      }
    },
    invalid: {
      color: '#fa755a',
      iconColor: '#fa755a'
    }
  }
};

const SessionPayment = ({ session, onPaymentSuccess, onPaymentError }) => {
  const [loading, setLoading] = useState(false);
  const [clientSecret, setClientSecret] = useState('');
  const [error, setError] = useState(null);
  const [transactionComplete, setTransactionComplete] = useState(false);
  const [receipt, setReceipt] = useState(null);
  
  const stripe = useStripe();
  const elements = useElements();

  useEffect(() => {
    // Create a payment intent as soon as the page loads
    if (session?.sessionId && !clientSecret) {
      createPaymentIntent();
    }
  }, [session]);

  const createPaymentIntent = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Format session data for payment intent creation
      const sessionData = {
        sessionId: session.sessionId,
        amount: parseFloat(session.price || '0') * 100, // Convert to cents
        currency: 'usd',
        description: `Tutoring session with ${session.tutorName} for ${session.subject}`,
        tutorId: session.tutorId,
        studentId: session.studentId
      };
      
      // Call API to create payment intent
      const response = await paymentApi.createPaymentIntent(sessionData);
      
      if (response.data?.clientSecret) {
        setClientSecret(response.data.clientSecret);
      } else {
        setError('Could not initialize payment. Please try again.');
      }
    } catch (err) {
      console.error('Error creating payment intent:', err);
      setError('Payment initialization failed. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!stripe || !elements || !clientSecret) {
      // Stripe.js has not yet loaded or client secret not available
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // Get card element
      const cardElement = elements.getElement(CardElement);
      
      // Confirm payment with Stripe
      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: {
          card: cardElement
        }
      });

      if (error) {
        console.error('Payment error:', error);
        setError(error.message);
        if (onPaymentError) {
          onPaymentError(error);
        }
      } else if (paymentIntent.status === 'succeeded') {
        console.log('Payment successful:', paymentIntent);
        
        // Notify backend about successful payment
        try {
          const confirmResponse = await paymentApi.confirmPayment(
            session.sessionId,
            paymentIntent.id
          );
          
          if (confirmResponse.data?.success) {
            setTransactionComplete(true);
            setReceipt({
              amount: (paymentIntent.amount / 100).toFixed(2),
              date: new Date().toLocaleString(),
              id: paymentIntent.id,
              subject: session.subject,
              tutor: session.tutorName
            });
            
            // Notify parent component
            if (onPaymentSuccess) {
              onPaymentSuccess(paymentIntent);
            }
            
            toast.success('Payment successful!');
          }
        } catch (confirmError) {
          console.error('Error confirming payment with backend:', confirmError);
          // Still consider payment successful since Stripe confirmed it
          setTransactionComplete(true);
          if (onPaymentSuccess) {
            onPaymentSuccess(paymentIntent);
          }
        }
      } else {
        console.warn('Payment not completed:', paymentIntent);
        setError(`Payment status: ${paymentIntent.status}. Please try again.`);
      }
    } catch (err) {
      console.error('Error processing payment:', err);
      setError('Payment processing failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (transactionComplete && receipt) {
    return (
      <div className="bg-white dark:bg-dark-800 rounded-xl p-6 border border-green-200 dark:border-green-800">
        <div className="flex items-center justify-center mb-4">
          <div className="w-12 h-12 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center">
            <svg className="w-6 h-6 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
            </svg>
          </div>
        </div>
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white text-center mb-4">Payment Successful!</h2>
        
        <div className="bg-gray-50 dark:bg-dark-700 rounded-lg p-4 mb-4">
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">Receipt</h3>
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-gray-500 dark:text-gray-400">Amount:</span>
              <span className="text-gray-900 dark:text-white font-medium">${receipt.amount}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500 dark:text-gray-400">Date:</span>
              <span className="text-gray-900 dark:text-white">{receipt.date}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500 dark:text-gray-400">Transaction ID:</span>
              <span className="text-gray-900 dark:text-white">{receipt.id.substring(0, 12)}...</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500 dark:text-gray-400">Subject:</span>
              <span className="text-gray-900 dark:text-white">{receipt.subject}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500 dark:text-gray-400">Tutor:</span>
              <span className="text-gray-900 dark:text-white">{receipt.tutor}</span>
            </div>
          </div>
        </div>
        
        <div className="text-center">
          <p className="text-green-600 dark:text-green-400 mb-4">Thank you for your payment. Your session is now confirmed!</p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-md"
          >
            Return to Session Details
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-dark-800 rounded-xl p-6 border border-gray-200 dark:border-dark-700">
      <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Complete Your Payment</h2>
      
      <div className="mb-6">
        <div className="flex justify-between mb-2">
          <span className="text-gray-500 dark:text-gray-400">Session:</span>
          <span className="text-gray-900 dark:text-white">{session.subject}</span>
        </div>
        <div className="flex justify-between mb-2">
          <span className="text-gray-500 dark:text-gray-400">Tutor:</span>
          <span className="text-gray-900 dark:text-white">{session.tutorName}</span>
        </div>
        <div className="flex justify-between mb-2">
          <span className="text-gray-500 dark:text-gray-400">Amount:</span>
          <span className="text-gray-900 dark:text-white font-medium">${session.price || '0.00'}</span>
        </div>
      </div>
      
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label className="block text-gray-700 dark:text-gray-300 mb-2">Card Information</label>
          <div className="p-3 border border-gray-300 dark:border-dark-600 rounded-lg bg-white dark:bg-dark-700">
            <CardElement options={CARD_ELEMENT_OPTIONS} />
          </div>
        </div>
        
        {error && (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-lg">
            {error}
          </div>
        )}
        
        <button
          type="submit"
          disabled={!stripe || loading || !clientSecret}
          className="w-full py-2 px-4 bg-primary-600 hover:bg-primary-700 disabled:bg-gray-400 dark:disabled:bg-gray-700 text-white rounded-md transition-colors"
        >
          {loading ? (
            <div className="flex items-center justify-center">
              <div className="w-5 h-5 border-t-2 border-b-2 border-white rounded-full animate-spin mr-2"></div>
              Processing...
            </div>
          ) : (
            `Pay $${session.price || '0.00'}`
          )}
        </button>
      </form>
    </div>
  );
};

export default SessionPayment; 