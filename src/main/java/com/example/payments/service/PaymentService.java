package com.example.payments.service;

import com.example.payments.exception.PaymentException;
import com.example.payments.model.Payment;
import com.example.payments.model.PaymentRequest;
import com.example.payments.model.PaymentResponse;
import com.example.payments.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final Counter paymentsCreatedCounter;
    private final Counter paymentsProcessedSuccessCounter;
    private final Counter paymentsProcessedFailureCounter;
    private final Timer paymentProcessingTimer;

    public PaymentService(PaymentRepository paymentRepository, MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;

        this.paymentsCreatedCounter = Counter.builder("payments.created.total")
                .description("Total number of payments created")
                .register(meterRegistry);

        this.paymentsProcessedSuccessCounter = Counter.builder("payments.processed.total")
                .tag("status", "success")
                .description("Total number of successfully processed payments")
                .register(meterRegistry);

        this.paymentsProcessedFailureCounter = Counter.builder("payments.processed.total")
                .tag("status", "failure")
                .description("Total number of failed payments")
                .register(meterRegistry);

        this.paymentProcessingTimer = Timer.builder("payments.processing.duration")
                .description("Time taken to process payments")
                .register(meterRegistry);
    }

    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment",
                kv("merchantId", request.getMerchantId()),
                kv("customerId", request.getCustomerId()),
                kv("amount", request.getAmount()),
                kv("currency", request.getCurrency()));

        Payment payment = Payment.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(Payment.PaymentStatus.PENDING)
                .description(request.getDescription())
                .merchantId(request.getMerchantId())
                .customerId(request.getCustomerId())
                .cardLastFour(request.getCardLastFour())
                .paymentMethod(request.getPaymentMethod())
                .referenceId(request.getReferenceId())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        paymentsCreatedCounter.increment();

        log.info("Payment created successfully",
                kv("paymentId", savedPayment.getId()),
                kv("status", savedPayment.getStatus()));

        return PaymentResponse.fromPayment(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        log.debug("Retrieving payment", kv("paymentId", id));

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(id));

        return PaymentResponse.fromPayment(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(Pageable pageable) {
        log.debug("Listing payments", kv("page", pageable.getPageNumber()), kv("size", pageable.getPageSize()));

        return paymentRepository.findAll(pageable)
                .map(PaymentResponse::fromPayment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPaymentsByMerchant(String merchantId, Pageable pageable) {
        log.debug("Listing payments for merchant",
                kv("merchantId", merchantId),
                kv("page", pageable.getPageNumber()));

        return paymentRepository.findByMerchantId(merchantId, pageable)
                .map(PaymentResponse::fromPayment);
    }

    public PaymentResponse updatePayment(UUID id, PaymentRequest request) {
        log.info("Updating payment", kv("paymentId", id));

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(id));

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED ||
                payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            throw new PaymentException.PaymentNotModifiableException(id, payment.getStatus());
        }

        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount());
        }
        if (request.getCurrency() != null) {
            payment.setCurrency(request.getCurrency().toUpperCase());
        }
        if (request.getDescription() != null) {
            payment.setDescription(request.getDescription());
        }
        if (request.getCardLastFour() != null) {
            payment.setCardLastFour(request.getCardLastFour());
        }
        if (request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getReferenceId() != null) {
            payment.setReferenceId(request.getReferenceId());
        }

        Payment updatedPayment = paymentRepository.save(payment);

        log.info("Payment updated successfully",
                kv("paymentId", updatedPayment.getId()),
                kv("status", updatedPayment.getStatus()));

        return PaymentResponse.fromPayment(updatedPayment);
    }

    public PaymentResponse processPayment(UUID id) {
        log.info("Processing payment", kv("paymentId", id));

        return paymentProcessingTimer.record(() -> {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new PaymentException.PaymentNotFoundException(id));

            if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
                throw new PaymentException.InvalidPaymentStateException(id, payment.getStatus(), Payment.PaymentStatus.PENDING);
            }

            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            try {
                // Simulate payment processing
                simulatePaymentProcessing(payment);

                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setProcessedAt(Instant.now());
                paymentsProcessedSuccessCounter.increment();

                log.info("Payment processed successfully",
                        kv("paymentId", payment.getId()),
                        kv("status", payment.getStatus()));

            } catch (Exception e) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentsProcessedFailureCounter.increment();

                log.error("Payment processing failed",
                        kv("paymentId", payment.getId()),
                        kv("error", e.getMessage()));
            }

            return PaymentResponse.fromPayment(paymentRepository.save(payment));
        });
    }

    public void cancelPayment(UUID id) {
        log.info("Cancelling payment", kv("paymentId", id));

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(id));

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED ||
                payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            throw new PaymentException.PaymentNotModifiableException(id, payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        log.info("Payment cancelled successfully", kv("paymentId", id));
    }

    public PaymentResponse refundPayment(UUID id) {
        log.info("Processing refund", kv("paymentId", id));

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException.PaymentNotFoundException(id));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new PaymentException.InvalidPaymentStateException(id, payment.getStatus(), Payment.PaymentStatus.COMPLETED);
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment refundedPayment = paymentRepository.save(payment);

        log.info("Refund processed successfully",
                kv("paymentId", refundedPayment.getId()),
                kv("status", refundedPayment.getStatus()));

        return PaymentResponse.fromPayment(refundedPayment);
    }

    private void simulatePaymentProcessing(Payment payment) {
        // In a real implementation, this would integrate with payment processors
        // For now, we simulate a successful processing
        log.debug("Simulating payment processing",
                kv("paymentId", payment.getId()),
                kv("amount", payment.getAmount()));
    }
}
