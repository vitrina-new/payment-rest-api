package com.example.payments.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @Size(min = 4, max = 4, message = "Card last four must be exactly 4 digits")
    @Pattern(regexp = "\\d{4}", message = "Card last four must contain only digits")
    private String cardLastFour;

    private String paymentMethod;

    private String referenceId;
}
