package com.example.payments.controller;

import com.example.payments.model.PaymentRequest;
import com.example.payments.model.PaymentResponse;
import com.example.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment management API")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Create a new payment", description = "Creates a new payment with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieves a specific payment by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all payments", description = "Retrieves a paginated list of all payments")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    public ResponseEntity<Page<PaymentResponse>> listPayments(
            @Parameter(description = "Merchant ID to filter by") @RequestParam(required = false) String merchantId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PaymentResponse> payments;
        if (merchantId != null) {
            payments = paymentService.listPaymentsByMerchant(merchantId, pageable);
        } else {
            payments = paymentService.listPayments(pageable);
        }
        return ResponseEntity.ok(payments);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payment", description = "Updates an existing payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment cannot be modified")
    })
    public ResponseEntity<PaymentResponse> updatePayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id,
            @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.updatePayment(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel payment", description = "Cancels a payment that has not been completed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment cannot be cancelled")
    })
    public ResponseEntity<Void> cancelPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id) {
        paymentService.cancelPayment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process payment", description = "Processes a pending payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment cannot be processed")
    })
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id) {
        PaymentResponse response = paymentService.processPayment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund payment", description = "Refunds a completed payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund processed successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment cannot be refunded")
    })
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "Payment ID") @PathVariable UUID id) {
        PaymentResponse response = paymentService.refundPayment(id);
        return ResponseEntity.ok(response);
    }
}
