package edu.cit.Judify.PaymentTransaction;

import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    // One-to-one mapping with a tutoring session
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private TutoringSessionEntity session;

    // Payer (usually the learner) and payee (usually the tutor)
    @ManyToOne
    @JoinColumn(name = "payer_id", nullable = false)
    private UserEntity payer;

    @ManyToOne
    @JoinColumn(name = "payee_id", nullable = false)
    private UserEntity payee;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency = "PHP"; // Default currency

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String paymentStatus; // e.g., pending, completed, failed, refunded

    @Column(nullable = false)
    private String paymentGatewayReference; // External reference (e.g., Stripe)

    @Column(nullable = false)
    private String transactionReference;

    // Stripe-specific fields
    @Column
    private String paymentIntentId; // Stripe PaymentIntent ID

    @Column
    private String paymentMethodId; // Stripe PaymentMethod ID
    
    @Column
    private String clientSecret; // Client secret for frontend confirmation

    @Column
    private Boolean isRefunded = false;
    
    @Column
    private String receiptUrl; // URL to payment receipt

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    // Constructors
    public PaymentTransactionEntity() {
    }

    // Getters and Setters

    public Long getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public TutoringSessionEntity getSession() {
        return session;
    }
    public void setSession(TutoringSessionEntity session) {
        this.session = session;
    }

    public UserEntity getPayer() {
        return payer;
    }
    public void setPayer(UserEntity payer) {
        this.payer = payer;
    }

    public UserEntity getPayee() {
        return payee;
    }
    public void setPayee(UserEntity payee) {
        this.payee = payee;
    }

    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getPaymentIntentId() {
        return paymentIntentId;
    }
    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }
    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Boolean getIsRefunded() {
        return isRefunded;
    }
    public void setIsRefunded(Boolean isRefunded) {
        this.isRefunded = isRefunded;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }
    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
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

    public Date getCompletedAt() {
        return completedAt;
    }
    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
