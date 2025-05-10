package edu.cit.Judify.PaymentTransaction;

import com.stripe.exception.StripeException;
import edu.cit.Judify.PaymentTransaction.DTO.CreatePaymentIntentRequest;
import edu.cit.Judify.PaymentTransaction.DTO.PaymentIntentResponse;
import edu.cit.Judify.PaymentTransaction.DTO.PaymentTransactionDTO;
import edu.cit.Judify.PaymentTransaction.DTO.PaymentTransactionDTOMapper;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@Tag(name = "Payment Transactions", description = "Payment transaction management endpoints")
public class PaymentTransactionController {

    private final PaymentTransactionService paymentTransactionService;
    private final UserRepository userRepository;
    private final PaymentTransactionDTOMapper dtoMapper;

    @Autowired
    public PaymentTransactionController(
            PaymentTransactionService paymentTransactionService,
            UserRepository userRepository,
            PaymentTransactionDTOMapper dtoMapper) {
        this.paymentTransactionService = paymentTransactionService;
        this.userRepository = userRepository;
        this.dtoMapper = dtoMapper;
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
        PaymentTransactionEntity transaction = dtoMapper.toEntity(transactionDTO);
        return ResponseEntity.ok(dtoMapper.toDTO(paymentTransactionService.createTransaction(transaction)));
    }

    @Operation(summary = "Get transaction by ID", description = "Returns a payment transaction by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the transaction"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<PaymentTransactionDTO> getTransactionById(
            @Parameter(description = "Transaction ID") @PathVariable Long id) {
        return paymentTransactionService.getTransactionById(id)
                .map(dtoMapper::toDTO)
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
        return ResponseEntity.ok(paymentTransactionService.getPayerTransactions(payer)
                .stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get transactions by payee", description = "Returns all payment transactions received by a specific payee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved payee's transactions")
    })
    @GetMapping("/findByPayee/{payeeId}")
    public ResponseEntity<List<PaymentTransactionDTO>> getPayeeTransactions(
            @Parameter(description = "Payee ID") @PathVariable UserEntity payee) {
        return ResponseEntity.ok(paymentTransactionService.getPayeeTransactions(payee)
                .stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get transactions by status", description = "Returns all payment transactions with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions by status")
    })
    @GetMapping("/findByStatus/{status}")
    public ResponseEntity<List<PaymentTransactionDTO>> getTransactionsByStatus(
            @Parameter(description = "Transaction status (e.g., COMPLETED, PENDING, REFUNDED)") @PathVariable String status) {
        return ResponseEntity.ok(paymentTransactionService.getTransactionsByStatus(status)
                .stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get transactions by reference", description = "Returns all payment transactions matching a reference ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions by reference")
    })
    @GetMapping("/findByReference/{reference}")
    public ResponseEntity<List<PaymentTransactionDTO>> getTransactionsByReference(
            @Parameter(description = "Reference ID (e.g., tutoring session ID)") @PathVariable String reference) {
        return ResponseEntity.ok(paymentTransactionService.getTransactionsByReference(reference)
                .stream()
                .map(dtoMapper::toDTO)
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
        return ResponseEntity.ok(dtoMapper.toDTO(
                paymentTransactionService.updateTransactionStatus(id, status)));
    }

    @Operation(summary = "Process a refund", description = "Processes a refund for an existing payment transaction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refund successfully processed"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "400", description = "Transaction cannot be refunded"),
        @ApiResponse(responseCode = "500", description = "Error processing refund")
    })
    @PostMapping("/processRefund/{id}")
    public ResponseEntity<?> processRefund(
            @Parameter(description = "Transaction ID to refund") @PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            UserEntity currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the transaction
            PaymentTransactionEntity transaction = paymentTransactionService.getTransactionById(id)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Verify that the current user is either the payer or has admin rights
            if (!transaction.getPayer().getUserId().equals(currentUser.getUserId()) &&
                !currentUser.getRole().equals("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to refund this transaction");
            }

            PaymentTransactionEntity refundedTransaction = paymentTransactionService.processRefund(id);
            return ResponseEntity.ok(dtoMapper.toDTO(refundedTransaction));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing refund: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete a transaction", description = "Deletes a payment transaction by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @DeleteMapping("/deleteTransaction/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Transaction ID") @PathVariable Long id) {
        paymentTransactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody CreatePaymentIntentRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            UserEntity currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PaymentIntentResponse response = paymentTransactionService.createPaymentIntent(request, currentUser);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating payment intent: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentTransactionDTO>> getMyPayments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PaymentTransactionEntity> transactions = paymentTransactionService.getPayerTransactions(currentUser);
        List<PaymentTransactionDTO> dtos = transactions.stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/my-earnings")
    public ResponseEntity<List<PaymentTransactionDTO>> getMyEarnings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PaymentTransactionEntity> transactions = paymentTransactionService.getPayeeTransactions(currentUser);
        List<PaymentTransactionDTO> dtos = transactions.stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
} 