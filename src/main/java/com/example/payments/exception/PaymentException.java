package com.example.payments.exception;

import com.example.payments.model.Payment;

import java.util.UUID;

public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class PaymentNotFoundException extends PaymentException {
        private final UUID paymentId;

        public PaymentNotFoundException(UUID paymentId) {
            super("Payment not found with ID: " + paymentId);
            this.paymentId = paymentId;
        }

        public UUID getPaymentId() {
            return paymentId;
        }
    }

    public static class PaymentNotModifiableException extends PaymentException {
        private final UUID paymentId;
        private final Payment.PaymentStatus currentStatus;

        public PaymentNotModifiableException(UUID paymentId, Payment.PaymentStatus currentStatus) {
            super("Payment " + paymentId + " cannot be modified in status: " + currentStatus);
            this.paymentId = paymentId;
            this.currentStatus = currentStatus;
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public Payment.PaymentStatus getCurrentStatus() {
            return currentStatus;
        }
    }

    public static class InvalidPaymentStateException extends PaymentException {
        private final UUID paymentId;
        private final Payment.PaymentStatus currentStatus;
        private final Payment.PaymentStatus requiredStatus;

        public InvalidPaymentStateException(UUID paymentId, Payment.PaymentStatus currentStatus, Payment.PaymentStatus requiredStatus) {
            super("Payment " + paymentId + " is in status " + currentStatus + " but requires status " + requiredStatus);
            this.paymentId = paymentId;
            this.currentStatus = currentStatus;
            this.requiredStatus = requiredStatus;
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public Payment.PaymentStatus getCurrentStatus() {
            return currentStatus;
        }

        public Payment.PaymentStatus getRequiredStatus() {
            return requiredStatus;
        }
    }
}
