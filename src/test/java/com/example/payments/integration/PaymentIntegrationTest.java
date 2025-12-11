package com.example.payments.integration;

import com.example.payments.model.Payment;
import com.example.payments.model.PaymentRequest;
import com.example.payments.model.PaymentResponse;
import com.example.payments.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("should complete full payment lifecycle")
    void shouldCompleteFullPaymentLifecycle() throws Exception {
        // Create payment
        PaymentRequest createRequest = PaymentRequest.builder()
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .merchantId("merchant-integration-test")
                .customerId("customer-integration-test")
                .description("Integration test payment")
                .cardLastFour("4242")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        PaymentResponse createdPayment = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        // Get payment
        mockMvc.perform(get("/api/v1/payments/{id}", createdPayment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdPayment.getId().toString()))
                .andExpect(jsonPath("$.amount").value(150.00));

        // Process payment
        mockMvc.perform(post("/api/v1/payments/{id}/process", createdPayment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Refund payment
        mockMvc.perform(post("/api/v1/payments/{id}/refund", createdPayment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        // Verify final state in database
        Payment finalPayment = paymentRepository.findById(createdPayment.getId()).orElseThrow();
        assertThat(finalPayment.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("should handle payment cancellation")
    void shouldHandlePaymentCancellation() throws Exception {
        // Create payment
        PaymentRequest createRequest = PaymentRequest.builder()
                .amount(new BigDecimal("75.50"))
                .currency("EUR")
                .merchantId("merchant-cancel-test")
                .customerId("customer-cancel-test")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse createdPayment = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        // Cancel payment
        mockMvc.perform(delete("/api/v1/payments/{id}", createdPayment.getId()))
                .andExpect(status().isNoContent());

        // Verify cancelled state
        Payment cancelledPayment = paymentRepository.findById(createdPayment.getId()).orElseThrow();
        assertThat(cancelledPayment.getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("should list payments with pagination")
    void shouldListPaymentsWithPagination() throws Exception {
        // Create multiple payments
        for (int i = 0; i < 5; i++) {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("10.00").multiply(BigDecimal.valueOf(i + 1)))
                    .currency("USD")
                    .merchantId("merchant-list-test")
                    .customerId("customer-" + i)
                    .build();

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // List payments with pagination
        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("should update payment")
    void shouldUpdatePayment() throws Exception {
        // Create payment
        PaymentRequest createRequest = PaymentRequest.builder()
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .merchantId("merchant-update-test")
                .customerId("customer-update-test")
                .description("Original description")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse createdPayment = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        // Update payment
        PaymentRequest updateRequest = PaymentRequest.builder()
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .merchantId("merchant-update-test")
                .customerId("customer-update-test")
                .description("Updated description")
                .build();

        mockMvc.perform(put("/api/v1/payments/{id}", createdPayment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @DisplayName("health endpoint should be accessible")
    void healthEndpointShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
