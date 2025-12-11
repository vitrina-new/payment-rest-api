package com.example.payments.service;

import com.example.payments.exception.PaymentException;
import com.example.payments.model.Payment;
import com.example.payments.model.PaymentRequest;
import com.example.payments.model.PaymentResponse;
import com.example.payments.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private MeterRegistry meterRegistry;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        paymentService = new PaymentService(paymentRepository, meterRegistry);
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("should create payment successfully")
        void shouldCreatePaymentSuccessfully() {
            // Given
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .description("Test payment")
                    .cardLastFour("1234")
                    .build();

            Payment savedPayment = Payment.builder()
                    .id(UUID.randomUUID())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(Payment.PaymentStatus.PENDING)
                    .merchantId(request.getMerchantId())
                    .customerId(request.getCustomerId())
                    .description(request.getDescription())
                    .cardLastFour(request.getCardLastFour())
                    .createdAt(Instant.now())
                    .build();

            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

            // When
            PaymentResponse response = paymentService.createPayment(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(savedPayment.getId());
            assertThat(response.getAmount()).isEqualTo(request.getAmount());
            assertThat(response.getCurrency()).isEqualTo(request.getCurrency().toUpperCase());
            assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment capturedPayment = paymentCaptor.getValue();
            assertThat(capturedPayment.getMerchantId()).isEqualTo(request.getMerchantId());
        }

        @Test
        @DisplayName("should increment created counter")
        void shouldIncrementCreatedCounter() {
            // Given
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new BigDecimal("50.00"))
                    .currency("eur")
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .build();

            Payment savedPayment = Payment.builder()
                    .id(UUID.randomUUID())
                    .amount(request.getAmount())
                    .currency("EUR")
                    .status(Payment.PaymentStatus.PENDING)
                    .merchantId(request.getMerchantId())
                    .customerId(request.getCustomerId())
                    .createdAt(Instant.now())
                    .build();

            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

            // When
            paymentService.createPayment(request);

            // Then
            Counter counter = meterRegistry.find("payments.created.total").counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("getPayment")
    class GetPayment {

        @Test
        @DisplayName("should return payment when found")
        void shouldReturnPaymentWhenFound() {
            // Given
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(Payment.PaymentStatus.PENDING)
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .createdAt(Instant.now())
                    .build();

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            // When
            PaymentResponse response = paymentService.getPayment(paymentId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(paymentId);
        }

        @Test
        @DisplayName("should throw exception when payment not found")
        void shouldThrowExceptionWhenPaymentNotFound() {
            // Given
            UUID paymentId = UUID.randomUUID();
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                    .isInstanceOf(PaymentException.PaymentNotFoundException.class)
                    .hasMessageContaining(paymentId.toString());
        }
    }

    @Nested
    @DisplayName("cancelPayment")
    class CancelPayment {

        @Test
        @DisplayName("should cancel pending payment")
        void shouldCancelPendingPayment() {
            // Given
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(Payment.PaymentStatus.PENDING)
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .createdAt(Instant.now())
                    .build();

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            // When
            paymentService.cancelPayment(paymentId);

            // Then
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(Payment.PaymentStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw exception when cancelling completed payment")
        void shouldThrowExceptionWhenCancellingCompletedPayment() {
            // Given
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .status(Payment.PaymentStatus.COMPLETED)
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .createdAt(Instant.now())
                    .build();

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            // When/Then
            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
                    .isInstanceOf(PaymentException.PaymentNotModifiableException.class);
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("should refund completed payment")
        void shouldRefundCompletedPayment() {
            // Given
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(Payment.PaymentStatus.COMPLETED)
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .createdAt(Instant.now())
                    .build();

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

            // When
            PaymentResponse response = paymentService.refundPayment(paymentId);

            // Then
            assertThat(response.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("should throw exception when refunding non-completed payment")
        void shouldThrowExceptionWhenRefundingNonCompletedPayment() {
            // Given
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .status(Payment.PaymentStatus.PENDING)
                    .merchantId("merchant-123")
                    .customerId("customer-456")
                    .createdAt(Instant.now())
                    .build();

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            // When/Then
            assertThatThrownBy(() -> paymentService.refundPayment(paymentId))
                    .isInstanceOf(PaymentException.InvalidPaymentStateException.class);
        }
    }
}
