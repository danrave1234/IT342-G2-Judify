import { Elements } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';

// Initialize Stripe with publishable key (put your actual key in env vars)
// For development, use Stripe test mode key
const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLISHABLE_KEY || 'pk_test_51JHuQmIdxRSHLKCIEZ9gHn5hTL2SpbAuErhUKzDn1sU2CJaMQPTpdjdW4HvGb8JbXbPeP8U3i2PpVl6QI3Vnd4IW00C9SIXfTM');

const StripeWrapper = ({ children }) => {
  return (
    <Elements stripe={stripePromise}>
      {children}
    </Elements>
  );
};

export default StripeWrapper; 