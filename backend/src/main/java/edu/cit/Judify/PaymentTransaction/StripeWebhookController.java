package edu.cit.Judify.PaymentTransaction;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import edu.cit.Judify.config.StripeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeConfig stripeConfig;
    private final PaymentTransactionService paymentTransactionService;

    @Autowired
    public StripeWebhookController(StripeConfig stripeConfig, PaymentTransactionService paymentTransactionService) {
        this.stripeConfig = stripeConfig;
        this.paymentTransactionService = paymentTransactionService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Received Stripe webhook");

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getStripeWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        log.info("Webhook verified successfully. Event type: {}", event.getType());

        // Process different event types
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        switch (event.getType()) {
            case "payment_intent.succeeded":
                if (dataObjectDeserializer.getObject().isPresent()) {
                    PaymentIntent paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject().get();
                    log.info("Payment succeeded for PaymentIntent: {}", paymentIntent.getId());
                    
                    // Extract payment method
                    String paymentMethodId = paymentIntent.getPaymentMethod();
                    
                    // For receipt URL, we don't have direct access in this version of the API
                    String receiptUrl = null;
                    
                    // Update transaction status
                    paymentTransactionService.handlePaymentIntentSucceeded(
                            paymentIntent.getId(), 
                            paymentMethodId, 
                            receiptUrl);
                }
                break;
                
            case "payment_intent.payment_failed":
                if (dataObjectDeserializer.getObject().isPresent()) {
                    PaymentIntent paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject().get();
                    log.info("Payment failed for PaymentIntent: {}", paymentIntent.getId());
                    
                    // Get last error message if available
                    String failureMessage = paymentIntent.getLastPaymentError() != null ? 
                            paymentIntent.getLastPaymentError().getMessage() : "Payment failed";
                            
                    // Update transaction status
                    paymentTransactionService.handlePaymentIntentFailed(paymentIntent.getId(), failureMessage);
                }
                break;
                
            default:
                log.info("Unhandled event type: {}", event.getType());
                break;
        }

        return ResponseEntity.ok("Webhook received successfully");
    }
} 