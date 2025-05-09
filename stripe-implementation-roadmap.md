# Stripe Implementation Roadmap - Tutoring Platform

## Summary
This document outlines the implementation roadmap for integrating Stripe as the payment gateway for our tutoring platform. The payment flow will handle transactions between students and tutors, using Stripe's PaymentIntent API for secure processing, with webhooks to update our database after payment completion.

## Core Payment Flow
1. **Create a PaymentIntent on the Backend**
   - Backend receives request with tutoring session details
   - Calculate amount based on tutoring rate and session duration
   - Create PaymentIntent via Stripe API with secret key
   - Return client_secret to client application

2. **Client-Side Payment Processing**
   - Client receives client_secret
   - Display payment form using Stripe Elements
   - Collect payment method details
   - Confirm payment with Stripe SDK

3. **Webhook Processing**
   - Stripe sends event to webhook endpoint
   - Verify webhook signature
   - Process payment_intent.succeeded or payment_intent.payment_failed events
   - Update transaction status in database
   - Trigger appropriate business logic (notify tutor/student, update session status)

## Detailed Implementation Steps

### 1. Backend Setup (Cloud Run)

#### A. Environment Configuration
- Configure environment variables:
  - `STRIPE_SECRET_KEY`: Secret key for backend API calls
  - `STRIPE_WEBHOOK_SECRET`: Secret for webhook verification
  - `STRIPE_PUBLISHABLE_KEY`: Public key for client-side SDK

#### B. API Endpoints
- Implement `/api/payments/create-intent`:
  - Authenticate user request
  - Validate tutoring session details
  - Calculate payment amount
  - Create PaymentIntent with Stripe
  - Return client_secret to client

- Implement `/webhook`:
  - Verify Stripe signature
  - Parse event type
  - Handle payment_intent.succeeded
  - Handle payment_intent.payment_failed
  - Return appropriate HTTP response

#### C. Database Integration (Neon)
- Design PaymentTransactionEntity:
  - Basic fields: id, amount, currency, status
  - Stripe-specific fields: paymentIntentId, paymentMethodId
  - Relation fields: userId, tutorId, sessionId
  - Audit fields: createdAt, updatedAt, completedAt

### 2. Frontend Implementation

#### A. Web Client (React/Vite)
- Install required packages:
  - @stripe/react-stripe-js
  - @stripe/stripe-js

- Set up Stripe provider with publishable key
- Create PaymentForm component:
  - Integrate Stripe Elements (Card, Payment Element)
  - Handle form submission
  - Manage payment states (processing, success, error)
  - Display appropriate UI feedback

#### B. Android Client
- Add Stripe SDK to Gradle dependencies
- Initialize Stripe with publishable key
- Create payment UI:
  - Implement CardInputWidget or PaymentSheet
  - Handle payment confirmation
  - Manage payment results and UI states

### 3. Testing & Validation

#### A. Stripe Testing Tools
- Use Stripe CLI for webhook testing:
  ```
  stripe listen --forward-to localhost:8080/webhook
  ```
- Test with various test cards:
  - 4242 4242 4242 4242 (successful payment)
  - 4000 0000 0000 9995 (insufficient funds)
  - 4000 0025 0000 3155 (requires authentication)

#### B. End-to-End Testing
- Test complete payment flow:
  1. Schedule tutoring session
  2. Initiate payment
  3. Complete payment
  4. Verify webhook processing
  5. Check database updates
  6. Confirm notifications

## Data Flow Description

1. **Session Completion Flow**:
   - Tutoring session ends → Payment calculation → PaymentIntent creation → Client confirmation → Webhook handling → Database update

2. **Payment State Management**:
   - Initial state (created) → Processing → Success/Failure → Notification

3. **Database Transaction Updates**:
   - Create transaction record when PaymentIntent is created
   - Update transaction status based on webhook events
   - Store payment references for reconciliation

## Security Considerations

1. **API Key Management**:
   - Keep secret key on server only
   - Use environment variables for all keys
   - Rotate keys periodically

2. **Webhook Security**:
   - Always verify webhook signatures
   - Use HTTPS for all endpoints
   - Implement proper error handling

3. **PCI Compliance**:
   - Never store card details on our servers
   - Use Stripe Elements to securely collect payment information
   - Follow Stripe's security recommendations

## Going Live Checklist

1. **Account Setup**:
   - Complete Stripe account verification
   - Set up banking details for payouts
   - Configure webhook endpoints for production

2. **Environment Transition**:
   - Replace test keys with live keys
   - Update webhook URLs
   - Configure proper error handling

3. **Monitoring & Alerts**:
   - Set up logging for payment events
   - Configure alerts for failed payments
   - Monitor webhook delivery and processing

## Next Steps

1. Testing:
   - Set up Stripe CLI for local webhook testing
   - Test the complete payment flow from session creation to payment completion
2. Deployment & Monitoring:
   - Configure production environment variables 
   - Set up webhook endpoints in production
   - Implement logging and monitoring for production
   - Set up alerts for failed payments

### Completed Frontend Implementation

The frontend integration with Stripe has been successfully implemented with the following components:

#### 1. Frontend Stripe Dependencies
- Added @stripe/react-stripe-js and @stripe/stripe-js packages to the frontend
- Configured Stripe provider with publishable key from environment

#### 2. Payment Components
- Created SessionPayment.jsx component that:
  - Integrates with Stripe Elements to securely collect card information
  - Handles the payment flow after a tutoring session is confirmed
  - Provides appropriate UI states (loading, success, error)
  - Processes the payment intent received from the backend
  - Updates the UI based on payment status

#### 3. Payment Flow Integration
- Implemented payment confirmation after session completion
- Added styling for payment form using custom CSS
- Created container components for the payment flow
- Integrated with session management to trigger payments at appropriate times

#### 4. User Experience Enhancements
- Added loading indicators during payment processing
- Implemented success and error handling with user feedback
- Created receipt display after successful payment
- Added navigation flows after payment completion

#### 5. Date and Time Handling Improvements
- Created utility functions in dateUtils.js to handle dates consistently 
- Implemented a reusable SessionTimeDisplay component
- Fixed timezone issues that were affecting payment scheduling
- Ensured consistent time display across all components

By following this roadmap, we'll implement a secure, reliable payment system for our tutoring platform that handles the complete transaction lifecycle between students and tutors. 

## Implementation Status

### Completed Backend Implementation

The backend integration with Stripe has been successfully implemented with the following components:

#### 1. Core Infrastructure
- Added Stripe Java SDK dependency (version 24.6.0) to the project
- Created StripeConfig class to manage Stripe initialization and configuration
- Added environment variables for Stripe API keys and webhook secrets

#### 2. Database Schema Enhancement
- Extended PaymentTransactionEntity with Stripe-specific fields:
  - paymentIntentId: Stores the Stripe Payment Intent ID
  - paymentMethodId: Tracks the payment method used
  - clientSecret: Stores the client secret for frontend confirmation
  - receiptUrl: Links to payment receipt
  - isRefunded: Tracks refund status
  - Added appropriate timestamps (createdAt, updatedAt, completedAt)

#### 3. API Endpoints Implementation
- Created `/api/payments/create-payment-intent` endpoint:
  - Authenticates requesting user
  - Validates tutoring session details
  - Calculates payment amount in the appropriate currency
  - Creates PaymentIntent with Stripe
  - Returns client_secret and publishable key to client
- Implemented user-specific transaction endpoints:
  - `/api/payments/my-payments`: Retrieves current user's payments
  - `/api/payments/my-earnings`: Retrieves current user's earnings as a tutor
- Added refund processing endpoint at `/api/payments/processRefund/{id}`
  - Includes permission validation
  - Processes refunds through Stripe API
  - Updates transaction status

#### 4. Webhook Processing
- Implemented `/api/webhooks/stripe` endpoint for handling Stripe events:
  - Verifies webhook signature for security
  - Processes payment_intent.succeeded events
  - Processes payment_intent.payment_failed events
  - Updates transaction records based on event data

#### 5. Transaction Management Service
- Enhanced PaymentTransactionService with Stripe integration:
  - PaymentIntent creation with appropriate metadata
  - Transaction status management based on webhook events
  - Refund processing through Stripe API

### Next Steps
1. Frontend implementation:
   - Implement client-side payment form using Stripe Elements
   - Create payment confirmation flow
2. Testing:
   - Set up Stripe CLI for local webhook testing
   - Test the complete payment flow from session creation to payment completion
3. Deployment:
   - Configure production environment variables
   - Set up webhook endpoints in production
   - Implement logging and monitoring 