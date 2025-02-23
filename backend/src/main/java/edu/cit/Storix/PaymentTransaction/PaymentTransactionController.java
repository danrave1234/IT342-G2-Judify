package edu.cit.Storix.PaymentTransaction;

import edu.cit.Storix.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentTransactionController {

    private final PaymentTransactionService paymentService;

    @Autowired
    public PaymentTransactionController(PaymentTransactionService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentTransactionEntity> createTransaction(
            @RequestBody PaymentTransactionEntity transaction) {
        return ResponseEntity.ok(paymentService.createTransaction(transaction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentTransactionEntity> getTransactionById(@PathVariable Long id) {
        return paymentService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/payer/{payerId}")
    public ResponseEntity<List<PaymentTransactionEntity>> getPayerTransactions(
            @PathVariable UserEntity payer) {
        return ResponseEntity.ok(paymentService.getPayerTransactions(payer));
    }

    @GetMapping("/payee/{payeeId}")
    public ResponseEntity<List<PaymentTransactionEntity>> getPayeeTransactions(
            @PathVariable UserEntity payee) {
        return ResponseEntity.ok(paymentService.getPayeeTransactions(payee));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentTransactionEntity>> getTransactionsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(paymentService.getTransactionsByStatus(status));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<List<PaymentTransactionEntity>> getTransactionsByReference(
            @PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getTransactionsByReference(reference));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PaymentTransactionEntity> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(paymentService.updateTransactionStatus(id, status));
    }

    @PutMapping("/{id}/reference")
    public ResponseEntity<PaymentTransactionEntity> updatePaymentReference(
            @PathVariable Long id,
            @RequestParam String reference) {
        return ResponseEntity.ok(paymentService.updatePaymentReference(id, reference));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentTransactionEntity> processRefund(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.processRefund(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        paymentService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }
} 