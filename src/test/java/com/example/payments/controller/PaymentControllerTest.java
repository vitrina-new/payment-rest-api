package com.example.payments.controller;

import com.example.payments.exception.PaymentException;
import com.example.payments.model.Payment;
import com.example.payments.model.PaymentRequest;
import com.example.payments.model.PaymentResponse;
import com.example.payments.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(com.example.payments.config.SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Nested
    @DisplayName("POST /api/v1/payments")
    class CreatePaymentEndpoint {

        @Test
        @DisplayName("should create payment and return 201")
        void shouldCreatePaymentAndReturn201() throws Exception {
            // Given
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .description("Test payment")
                    .build();

            PaymentResponse response = PaymentResponse.builder()
                    .id(UUID.randomUUID())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(Payment.PaymentStatus.PENDING)
                    .merchantId(request.getMerchantId())
                    .customerId(request.getCustomerId())
                    .createdAt(Instant.now())
                    .build();

            when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.amount").value(100.00))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("-10.00")) // Invalid: negative amount
                    .currency("US") // Invalid: only 2 chars
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when required fields missing")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            // Given - empty request
            String emptyRequest = "{}";

            // When/Then
            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{id}")
    class GetPaymentEndpoint {

        @Test
        @DisplayName("should return payment when found")
        void shouldReturnPaymentWhenFound() throws Exception {
            // Given
            UUID paymentId = UUID.randomUUID();
            PaymentResponse response = PaymentResponse.builder()
                    .id(paymentId)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(Payment.PaymentStatus.PENDING)
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .createdAt(Instant.now())
                    .build();

            when(paymentService.getPayment(paymentId)).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(paymentId.toString()))
                    .andExpect(jsonPath("$.amount").value(100.00));
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() throws Exception {
            // Given
            UUID paymentId = UUID.randomUUID();
            when(paymentService.getPayment(paymentId))
                    .thenThrow(new PaymentException.PaymentNotFoundException(paymentId));

            // When/Then
            mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/payments/{id}")
    class CancelPaymentEndpoint {

        @Test
        @DisplayName("should cancel payment and return 204")
        void shouldCancelPaymentAndReturn204() throws Exception {
            // Given
            UUID paymentId = UUID.randomUUID();

            // When/Then
            mockMvc.perform(delete("/api/v1/payments/{id}", paymentId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 409 when payment cannot be cancelled")
        void shouldReturn409WhenPaymentCannotBeCancelled() throws Exception {
            // Given
            UUID paymentId = UUID.randomUUID();
            org.mockito.Mockito.doThrow(new PaymentException.PaymentNotModifiableException(paymentId, Payment.PaymentStatus.COMPLETED))
                    .when(paymentService).cancelPayment(paymentId);

            // When/Then
            mockMvc.perform(delete("/api/v1/payments/{id}", paymentId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/refund")
    class RefundPaymentEndpoint {

        @Test
        @DisplayName("should refund payment and return 200")
        void shouldRefundPaymentAndReturn200() throws Exception {
            // Given
            UUID paymentId = UUID.randomUUID();
            PaymentResponse response = PaymentResponse.builder()
                    .id(paymentId)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(Payment.PaymentStatus.REFUNDED)
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .createdAt(Instant.now())
                    .build();

            when(paymentService.refundPayment(paymentId)).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));
        }
    }
}
