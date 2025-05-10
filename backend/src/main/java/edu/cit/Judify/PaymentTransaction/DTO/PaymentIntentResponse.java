package edu.cit.Judify.PaymentTransaction.DTO;

public class PaymentIntentResponse {
    private String clientSecret;
    private Long transactionId;
    private String publishableKey;

    public PaymentIntentResponse(String clientSecret, Long transactionId, String publishableKey) {
        this.clientSecret = clientSecret;
        this.transactionId = transactionId;
        this.publishableKey = publishableKey;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }
} 