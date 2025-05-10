import { useState, useEffect } from 'react';
import { useStripe, useElements, CardElement } from '@stripe/react-stripe-js';
import { paymentApi, tutoringSessionApi } from '../../api/api';
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
      },
      // Dark mode specific styles
      '.dark &': {
        color: '#ffffff',
        '::placeholder': {
          color: '#cccccc'
        }
      }
    },
    invalid: {
      color: '#fa755a',
      iconColor: '#fa755a'
    }
  },
  hidePostalCode: true
};

const CARD_ELEMENT_DARK_OPTIONS = {
  style: {
    base: {
      color: '#ffffff',
      fontFamily: '"Helvetica Neue", Helvetica, sans-serif',
      fontSmoothing: 'antialiased',
      fontSize: '16px',
      '::placeholder': {
        color: '#cccccc'
      }
    },
    invalid: {
      color: '#fa755a',
      iconColor: '#fa755a'
    }
  },
  hidePostalCode: true
};

const SessionPayment = ({ sessionId, amount, onSuccess, onError, sessionData: initialSessionData }) => {
  const [loading, setLoading] = useState(false);
  const [clientSecret, setClientSecret] = useState('');
  const [error, setError] = useState(null);
  const [sessionData, setSessionData] = useState(initialSessionData || null);
  const [transactionComplete, setTransactionComplete] = useState(false);
  const [receipt, setReceipt] = useState(null);
  const [isDarkMode, setIsDarkMode] = useState(false);
  
  const stripe = useStripe();
  const elements = useElements();

  // Detect if dark mode is enabled
  useEffect(() => {
    const isDark = document.documentElement.classList.contains('dark') || 
                   document.body.classList.contains('dark') ||
                   window.matchMedia('(prefers-color-scheme: dark)').matches;
    setIsDarkMode(isDark);
  }, []);

  useEffect(() => {
    // Create payment intent using passed session data or fetch if needed
    const initializePayment = async () => {
      if (!sessionId) return;
      
      try {
        setLoading(true);
        
        // Use the sessionData directly if provided from parent component
        if (sessionData) {
          console.log('Using provided session data for payment:', sessionData);
          await createPaymentIntent(sessionData);
          return;
        }
        
        // Fall back to fetching session details if not provided
        try {
          const response = await tutoringSessionApi.getSessionById(sessionId);
          
          if (response.data) {
            // Normalize session data to handle snake_case to camelCase
            const fetchedSessionData = {
              ...response.data,
              id: response.data.id || response.data.sessionId || response.data.session_id,
              sessionId: response.data.sessionId || response.data.session_id || response.data.id,
              tutorId: response.data.tutorId || response.data.tutor_id,
              studentId: response.data.studentId || response.data.student_id,
              startTime: response.data.startTime || response.data.start_time,
              endTime: response.data.endTime || response.data.end_time,
              tutorName: response.data.tutorName || response.data.tutor_name,
              subject: response.data.subject,
              price: response.data.price
            };
            
            // Handle payment status
            if (response.data.paymentStatus || response.data.payment_status) {
              fetchedSessionData.paymentStatus = response.data.paymentStatus || response.data.payment_status;
            }
            
            console.log('Normalized session data for payment:', fetchedSessionData);
            setSessionData(fetchedSessionData);
            
            // Create payment intent
            await createPaymentIntent(fetchedSessionData);
          } else {
            setError('Could not load session details');
          }
        } catch (err) {
          console.error('Error fetching session details:', err);
          setError('Could not load session information');
        }
      } finally {
        setLoading(false);
      }
    };
    
    initializePayment();
  }, [sessionId, sessionData]);

  const createPaymentIntent = async (session) => {
    try {
      setLoading(true);
      setError(null);
      
      // Use the provided amount or fallback to session price
      const paymentAmount = amount || (session?.price ? parseFloat(session.price) * 100 : 0);
      
      // Format session data for payment intent creation
      const paymentData = {
        sessionId: sessionId,
        amount: paymentAmount, // Amount in cents
        currency: 'usd',
        description: session?.subject ? 
          `Tutoring session for ${session.subject}` : 
          'Tutoring session payment',
        tutorId: session?.tutorId,
        studentId: session?.studentId
      };
      
      console.log('Creating payment intent with data:', paymentData);
      
      // Call API to create payment intent
      const response = await paymentApi.createPaymentIntent(paymentData);
      
      if (response.data?.clientSecret) {
        setClientSecret(response.data.clientSecret);
      } else {
        setError('Could not initialize payment. Please try again.');
      }
    } catch (err) {
      console.error('Error creating payment intent:', err);
      setError('Payment initialization failed. Please try again later.');
      if (onError) onError(err);
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
        toast.error('Payment failed: ' + error.message);
        if (onError) {
          onError(error);
        }
      } else if (paymentIntent.status === 'succeeded') {
        console.log('Payment successful:', paymentIntent);
        
        // Notify backend about successful payment
        try {
          const confirmResponse = await paymentApi.confirmPayment(
            sessionId,
            paymentIntent.id
          );
          
          if (confirmResponse.data?.success) {
            setTransactionComplete(true);
            setReceipt({
              amount: (paymentIntent.amount / 100).toFixed(2),
              date: new Date().toLocaleString(),
              id: paymentIntent.id,
              subject: sessionData?.subject || 'Tutoring Session',
              tutor: sessionData?.tutorName || 'Tutor'
            });
            
            // Notify parent component
            if (onSuccess) {
              onSuccess(paymentIntent);
            }
            
            toast.success('Payment successful! Your session is now confirmed.');
          }
        } catch (confirmError) {
          console.error('Error confirming payment with backend:', confirmError);
          // Still consider payment successful since Stripe confirmed it
          setTransactionComplete(true);
          toast.success('Payment successful! Your session is now confirmed.');
          if (onSuccess) {
            onSuccess(paymentIntent);
          }
        }
      } else {
        console.warn('Payment not completed:', paymentIntent);
        setError(`Payment status: ${paymentIntent.status}. Please try again.`);
        toast.warning(`Payment status: ${paymentIntent.status}. Please try again.`);
      }
    } catch (err) {
      console.error('Error processing payment:', err);
      setError('Payment processing failed. Please try again.');
      toast.error('Payment processing failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (transactionComplete && receipt) {
    return (
      <div className="bg-white dark:bg-dark-800 rounded-xl p-6 border border-gray-200 dark:border-dark-700">
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 dark:bg-green-900/30 rounded-full mb-4">
            <svg className="w-8 h-8 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Payment Successful!</h2>
          <p className="text-gray-600 dark:text-gray-400 mt-1">Your session is now confirmed.</p>
        </div>
        
        <div className="bg-gray-50 dark:bg-dark-700 rounded-lg p-4 mb-6">
          <h3 className="font-medium text-gray-900 dark:text-white mb-3">Receipt</h3>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600 dark:text-gray-400">Amount paid:</span>
              <span className="font-medium text-gray-900 dark:text-white">${receipt.amount}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600 dark:text-gray-400">Date:</span>
              <span className="font-medium text-gray-900 dark:text-white">{receipt.date}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600 dark:text-gray-400">Payment ID:</span>
              <span className="font-medium text-gray-900 dark:text-white">{receipt.id}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600 dark:text-gray-400">Session:</span>
              <span className="font-medium text-gray-900 dark:text-white">{receipt.subject}</span>
            </div>
            {receipt.tutor && (
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Tutor:</span>
                <span className="font-medium text-gray-900 dark:text-white">{receipt.tutor}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-dark-800 rounded-xl p-6 border border-gray-200 dark:border-dark-700">
      <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Complete Your Payment</h2>
      
      <div className="mb-6">
        {sessionData && (
          <>
            <div className="flex justify-between mb-2">
              <span className="text-gray-500 dark:text-gray-300">Session:</span>
              <span className="text-gray-900 dark:text-white">{sessionData.subject || 'Tutoring Session'}</span>
            </div>
            {sessionData.tutorName && (
              <div className="flex justify-between mb-2">
                <span className="text-gray-500 dark:text-gray-300">Tutor:</span>
                <span className="text-gray-900 dark:text-white">{sessionData.tutorName}</span>
              </div>
            )}
          </>
        )}
        <div className="flex justify-between mb-2">
          <span className="text-gray-500 dark:text-gray-300">Amount:</span>
          <span className="text-gray-900 dark:text-white font-medium">
            ${((amount || 0) / 100).toFixed(2)}
          </span>
        </div>
      </div>
      
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label className="block text-gray-700 dark:text-gray-300 mb-2">Card Information</label>
          <div className="p-3 border border-gray-300 dark:border-dark-600 rounded-lg bg-white dark:bg-dark-700">
            <CardElement options={isDarkMode ? CARD_ELEMENT_DARK_OPTIONS : CARD_ELEMENT_OPTIONS} />
          </div>
          <p className="mt-2 text-xs text-gray-500 dark:text-gray-300">
            For testing, you can use card number: 4242 4242 4242 4242, any future date, any CVC.
          </p>
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
            `Pay $${((amount || 0) / 100).toFixed(2)}`
          )}
        </button>
      </form>
    </div>
  );
};

export default SessionPayment; 