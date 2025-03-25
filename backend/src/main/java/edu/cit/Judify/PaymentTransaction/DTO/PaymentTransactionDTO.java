package edu.cit.Judify.PaymentTransaction.DTO;

import java.util.Date;

public class PaymentTransactionDTO {
    private Long transactionId;
    private Long sessionId;
    private Long payerId;
    private Long payeeId;
    private Double amount;
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED
    private String paymentStatus; // PENDING, COMPLETED, FAILED, REFUNDED
    private String paymentGatewayReference; // External reference (e.g., Stripe)
    private String transactionReference;
    private Date createdAt;
    private Date updatedAt;

    // Default constructor
    public PaymentTransactionDTO() {
    }

    // Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getPayerId() {
        return payerId;
    }

    public void setPayerId(Long payerId) {
        this.payerId = payerId;
    }

    public Long getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(Long payeeId) {
        this.payeeId = payeeId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentGatewayReference() {
        return paymentGatewayReference;
    }

    public void setPaymentGatewayReference(String paymentGatewayReference) {
        this.paymentGatewayReference = paymentGatewayReference;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
} 