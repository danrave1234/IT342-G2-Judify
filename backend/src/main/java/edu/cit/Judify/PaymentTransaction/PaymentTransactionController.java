package edu.cit.Judify.PaymentTransaction;

import edu.cit.Judify.PaymentTransaction.DTO.PaymentTransactionDTO;
import edu.cit.Judify.PaymentTransaction.DTO.PaymentTransactionDTOMapper;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentTransactionController {

    private final PaymentTransactionService paymentService;
    private final PaymentTransactionDTOMapper paymentDTOMapper;

    @Autowired
    public PaymentTransactionController(PaymentTransactionService paymentService, 
                                       PaymentTransactionDTOMapper paymentDTOMapper) {
        this.paymentService = paymentService;
        this.paymentDTOMapper = paymentDTOMapper;
    }

    @PostMapping("/createTransaction")
    public ResponseEntity<PaymentTransactionDTO> createTransaction(
            @RequestBody PaymentTransactionDTO transactionDTO) {
        PaymentTransactionEntity transaction = paymentDTOMapper.toEntity(transactionDTO);
        return ResponseEntity.ok(paymentDTOMapper.toDTO(paymentService.createTransaction(transaction)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<PaymentTransactionDTO> getTransactionById(@PathVariable Long id) {
        return paymentService.getTransactionById(id)
                .map(paymentDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByPayer/{payerId}")
    public ResponseEntity<List<PaymentTransactionDTO>> getPayerTransactions(
            @PathVariable UserEntity payer) {
        return ResponseEntity.ok(paymentService.getPayerTransactions(payer)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByPayee/{payeeId}")
    public ResponseEntity<List<PaymentTransactionDTO>> getPayeeTransactions(
            @PathVariable UserEntity payee) {
        return ResponseEntity.ok(paymentService.getPayeeTransactions(payee)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByStatus/{status}")
    public ResponseEntity<List<PaymentTransactionDTO>> getTransactionsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(paymentService.getTransactionsByStatus(status)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByReference/{reference}")
    public ResponseEntity<List<PaymentTransactionDTO>> getTransactionsByReference(
            @PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getTransactionsByReference(reference)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<PaymentTransactionDTO> updateTransactionStatus(
            @PathVariable Long id,
            @RequestBody String status) {
        return ResponseEntity.ok(paymentDTOMapper.toDTO(
                paymentService.updateTransactionStatus(id, status)));
    }

    @PostMapping("/processRefund/{id}")
    public ResponseEntity<PaymentTransactionDTO> processRefund(@PathVariable Long id) {
        return ResponseEntity.ok(paymentDTOMapper.toDTO(paymentService.processRefund(id)));
    }

    @DeleteMapping("/deleteTransaction/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        paymentService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }
} 