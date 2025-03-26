package edu.cit.Judify.PaymentTransaction;

import edu.cit.Judify.PaymentTransaction.DTO.PaymentTransactionDTO;
import edu.cit.Judify.PaymentTransaction.DTO.PaymentTransactionDTOMapper;
import edu.cit.Judify.User.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payment Transactions", description = "Payment transaction management endpoints")
public class PaymentTransactionController {

    private final PaymentTransactionService paymentService;
    private final PaymentTransactionDTOMapper paymentDTOMapper;

    @Autowired
    public PaymentTransactionController(PaymentTransactionService paymentService, 
                                       PaymentTransactionDTOMapper paymentDTOMapper) {
        this.paymentService = paymentService;
        this.paymentDTOMapper = paymentDTOMapper;
    }

    @Operation(summary = "Create a new payment transaction", description = "Creates a new payment transaction for a tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentTransactionDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/createTransaction")
    public ResponseEntity<PaymentTransactionDTO> createTransaction(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Transaction data to create", required = true)
            @RequestBody PaymentTransactionDTO transactionDTO) {
        PaymentTransactionEntity transaction = paymentDTOMapper.toEntity(transactionDTO);
        return ResponseEntity.ok(paymentDTOMapper.toDTO(paymentService.createTransaction(transaction)));
    }

    @Operation(summary = "Get transaction by ID", description = "Returns a payment transaction by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the transaction"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<PaymentTransactionDTO> getTransactionById(
            @Parameter(description = "Transaction ID") @PathVariable Long id) {
        return paymentService.getTransactionById(id)
                .map(paymentDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get transactions by payer", description = "Returns all payment transactions made by a specific payer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved payer's transactions")
    })
    @GetMapping("/findByPayer/{payerId}")
    public ResponseEntity<List<PaymentTransactionDTO>> getPayerTransactions(
            @Parameter(description = "Payer ID") @PathVariable UserEntity payer) {
        return ResponseEntity.ok(paymentService.getPayerTransactions(payer)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get transactions by payee", description = "Returns all payment transactions received by a specific payee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved payee's transactions")
    })
    @GetMapping("/findByPayee/{payeeId}")
    public ResponseEntity<List<PaymentTransactionDTO>> getPayeeTransactions(
            @Parameter(description = "Payee ID") @PathVariable UserEntity payee) {
        return ResponseEntity.ok(paymentService.getPayeeTransactions(payee)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get transactions by status", description = "Returns all payment transactions with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions by status")
    })
    @GetMapping("/findByStatus/{status}")
    public ResponseEntity<List<PaymentTransactionDTO>> getTransactionsByStatus(
            @Parameter(description = "Transaction status (e.g., COMPLETED, PENDING, REFUNDED)") @PathVariable String status) {
        return ResponseEntity.ok(paymentService.getTransactionsByStatus(status)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get transactions by reference", description = "Returns all payment transactions matching a reference ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions by reference")
    })
    @GetMapping("/findByReference/{reference}")
    public ResponseEntity<List<PaymentTransactionDTO>> getTransactionsByReference(
            @Parameter(description = "Reference ID (e.g., tutoring session ID)") @PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getTransactionsByReference(reference)
                .stream()
                .map(paymentDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Update transaction status", description = "Updates the status of an existing payment transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction status successfully updated"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PutMapping("/updateStatus/{id}")
    public ResponseEntity<PaymentTransactionDTO> updateTransactionStatus(
            @Parameter(description = "Transaction ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New transaction status", required = true)
            @RequestBody String status) {
        return ResponseEntity.ok(paymentDTOMapper.toDTO(
                paymentService.updateTransactionStatus(id, status)));
    }

    @Operation(summary = "Process a refund", description = "Processes a refund for an existing payment transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refund successfully processed"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "400", description = "Transaction cannot be refunded")
    })
    @PostMapping("/processRefund/{id}")
    public ResponseEntity<PaymentTransactionDTO> processRefund(
            @Parameter(description = "Transaction ID to refund") @PathVariable Long id) {
        return ResponseEntity.ok(paymentDTOMapper.toDTO(paymentService.processRefund(id)));
    }

    @Operation(summary = "Delete a transaction", description = "Deletes a payment transaction by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @DeleteMapping("/deleteTransaction/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Transaction ID") @PathVariable Long id) {
        paymentService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }
} 