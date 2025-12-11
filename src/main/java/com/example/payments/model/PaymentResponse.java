package com.example.payments.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private String description;
    private String merchantId;
    private String customerId;
    private String cardLastFour;
    private String paymentMethod;
    private String referenceId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant processedAt;

    public static PaymentResponse fromPayment(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .merchantId(payment.getMerchantId())
                .customerId(payment.getCustomerId())
                .cardLastFour(payment.getCardLastFour())
                .paymentMethod(payment.getPaymentMethod())
                .referenceId(payment.getReferenceId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .processedAt(payment.getProcessedAt())
                .build();
    }
}
