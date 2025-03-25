package edu.cit.Judify.PaymentTransaction.DTO;

import edu.cit.Judify.PaymentTransaction.PaymentTransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentTransactionDTOMapper {

    public PaymentTransactionDTO toDTO(PaymentTransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        PaymentTransactionDTO dto = new PaymentTransactionDTO();
        dto.setTransactionId(entity.getTransactionId());
        dto.setSessionId(entity.getSession().getSessionId());
        dto.setPayerId(entity.getPayer().getUserId());
        dto.setPayeeId(entity.getPayee().getUserId());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setPaymentGatewayReference(entity.getPaymentGatewayReference());
        dto.setTransactionReference(entity.getTransactionReference());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public PaymentTransactionEntity toEntity(PaymentTransactionDTO dto) {
        if (dto == null) {
            return null;
        }

        PaymentTransactionEntity entity = new PaymentTransactionEntity();
        entity.setTransactionId(dto.getTransactionId());
        // Note: Session, Payer, and Payee should be set separately as they require their respective entities
        entity.setAmount(dto.getAmount());
        entity.setStatus(dto.getStatus());
        entity.setPaymentStatus(dto.getPaymentStatus());
        entity.setPaymentGatewayReference(dto.getPaymentGatewayReference());
        entity.setTransactionReference(dto.getTransactionReference());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
} 