package edu.cit.Judify.PaymentTransaction;

import edu.cit.Judify.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {
    List<PaymentTransactionEntity> findByPayerOrderByCreatedAtDesc(UserEntity payer);
    List<PaymentTransactionEntity> findByPayeeOrderByCreatedAtDesc(UserEntity payee);
    List<PaymentTransactionEntity> findByPaymentStatus(String status);
    List<PaymentTransactionEntity> findByPaymentGatewayReference(String reference);
} 