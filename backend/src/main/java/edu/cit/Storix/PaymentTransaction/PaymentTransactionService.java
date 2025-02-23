package edu.cit.Storix.PaymentTransaction;

import edu.cit.Storix.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public PaymentTransactionService(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public PaymentTransactionEntity createTransaction(PaymentTransactionEntity transaction) {
        // Add payment gateway integration logic here
        return paymentTransactionRepository.save(transaction);
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

    // Additional methods for payment processing could be added here
    @Transactional
    public PaymentTransactionEntity processRefund(Long transactionId) {
        PaymentTransactionEntity transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        // Add refund processing logic here
        transaction.setPaymentStatus("REFUNDED");
        return paymentTransactionRepository.save(transaction);
    }
} 