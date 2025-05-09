package edu.cit.Judify.PaymentTransaction;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import edu.cit.Judify.PaymentTransaction.DTO.CreatePaymentIntentRequest;
import edu.cit.Judify.PaymentTransaction.DTO.PaymentIntentResponse;
import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.TutoringSession.TutoringSessionService;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TutoringSessionService tutoringSessionService;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Autowired
    public PaymentTransactionService(PaymentTransactionRepository paymentTransactionRepository,
                                     TutoringSessionService tutoringSessionService) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.tutoringSessionService = tutoringSessionService;
    }

    @Transactional
    public PaymentTransactionEntity createTransaction(PaymentTransactionEntity transaction) {
        return paymentTransactionRepository.save(transaction);
    }

    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request, UserEntity currentUser) throws StripeException {
        // Get the tutoring session
        TutoringSessionEntity session = tutoringSessionService.getTutoringSessionById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Tutoring session not found"));

        // Validate that the current user is the student/payer
        if (!session.getStudent().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("You can only pay for your own sessions");
        }

        // Convert amount to cents (Stripe uses smallest currency unit)
        long amountInCents = Math.round(request.getAmount() * 100);

        // Create a payment intent using Stripe API
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setCurrency(request.getCurrency())
                .setAmount(amountInCents)
                .setDescription("Payment for tutoring session #" + session.getSessionId())
                .putMetadata("sessionId", session.getSessionId().toString())
                .putMetadata("studentId", session.getStudent().getUserId().toString())
                .putMetadata("tutorId", session.getTutor().getUserId().toString())
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        // Create a transaction record in the database
        PaymentTransactionEntity transaction = new PaymentTransactionEntity();
        transaction.setSession(session);
        transaction.setPayer(session.getStudent());
        transaction.setPayee(session.getTutor());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus("PENDING");
        transaction.setPaymentStatus("PENDING");
        transaction.setPaymentGatewayReference("stripe");
        transaction.setTransactionReference(UUID.randomUUID().toString());
        transaction.setPaymentIntentId(paymentIntent.getId());
        transaction.setClientSecret(paymentIntent.getClientSecret());

        PaymentTransactionEntity savedTransaction = paymentTransactionRepository.save(transaction);

        return new PaymentIntentResponse(
                paymentIntent.getClientSecret(),
                savedTransaction.getTransactionId(),
                stripePublishableKey
        );
    }

    @Transactional
    public void handlePaymentIntentSucceeded(String paymentIntentId, String paymentMethodId, String receiptUrl) {
        // Find transaction by payment intent ID
        List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByPaymentIntentId(paymentIntentId);
        
        if (transactions.isEmpty()) {
            throw new RuntimeException("Transaction not found for payment intent ID: " + paymentIntentId);
        }
        
        PaymentTransactionEntity transaction = transactions.get(0);
        
        // Update transaction status
        transaction.setPaymentStatus("COMPLETED");
        transaction.setStatus("COMPLETED");
        transaction.setPaymentMethodId(paymentMethodId);
        transaction.setReceiptUrl(receiptUrl);
        transaction.setCompletedAt(new Date());
        
        paymentTransactionRepository.save(transaction);
        
        // Update the session status as needed
        TutoringSessionEntity session = transaction.getSession();
        // You might want to update session status based on payment completion
        // tutoringSessionService.updateSessionAfterPayment(session.getSessionId());
    }

    @Transactional
    public void handlePaymentIntentFailed(String paymentIntentId, String failureMessage) {
        // Find transaction by payment intent ID
        List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByPaymentIntentId(paymentIntentId);
        
        if (transactions.isEmpty()) {
            throw new RuntimeException("Transaction not found for payment intent ID: " + paymentIntentId);
        }
        
        PaymentTransactionEntity transaction = transactions.get(0);
        
        // Update transaction status
        transaction.setPaymentStatus("FAILED");
        transaction.setStatus("FAILED");
        
        paymentTransactionRepository.save(transaction);
    }

    public Optional<PaymentTransactionEntity> getTransactionById(Long id) {
        return paymentTransactionRepository.findById(id);
    }

    public List<PaymentTransactionEntity> getPayerTransactions(UserEntity payer) {
        return paymentTransactionRepository.findByPayerOrderByCreatedAtDesc(payer);
    }

    public List<PaymentTransactionEntity> getPayeeTransactions(UserEntity payee) {
        return paymentTransactionRepository.findByPayeeOrderByCreatedAtDesc(payee);
    }

    public List<PaymentTransactionEntity> getTransactionsByStatus(String status) {
        return paymentTransactionRepository.findByPaymentStatus(status);
    }

    public List<PaymentTransactionEntity> getTransactionsByReference(String reference) {
        return paymentTransactionRepository.findByPaymentGatewayReference(reference);
    }

    public List<PaymentTransactionEntity> getTransactionsByPaymentIntentId(String paymentIntentId) {
        return paymentTransactionRepository.findByPaymentIntentId(paymentIntentId);
    }

    @Transactional
    public PaymentTransactionEntity updateTransactionStatus(Long id, String status) {
        PaymentTransactionEntity transaction = paymentTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        transaction.setPaymentStatus(status);
        return paymentTransactionRepository.save(transaction);
    }

    @Transactional
    public PaymentTransactionEntity updatePaymentReference(Long id, String reference) {
        PaymentTransactionEntity transaction = paymentTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        transaction.setPaymentGatewayReference(reference);
        return paymentTransactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        paymentTransactionRepository.deleteById(id);
    }

    @Transactional
    public PaymentTransactionEntity processRefund(Long transactionId) throws StripeException {
        PaymentTransactionEntity transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!"COMPLETED".equals(transaction.getPaymentStatus())) {
            throw new RuntimeException("Can only refund completed transactions");
        }
        
        if (transaction.getIsRefunded()) {
            throw new RuntimeException("Transaction has already been refunded");
        }
        
        // Process refund via Stripe
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(transaction.getPaymentIntentId())
                .build();
                
        Refund refund = Refund.create(params);
        
        // Update transaction status
        transaction.setPaymentStatus("REFUNDED");
        transaction.setStatus("REFUNDED");
        transaction.setIsRefunded(true);
        
        return paymentTransactionRepository.save(transaction);
    }
} 