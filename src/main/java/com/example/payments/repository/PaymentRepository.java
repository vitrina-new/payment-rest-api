package com.example.payments.repository;

import com.example.payments.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByMerchantId(String merchantId, Pageable pageable);

    Page<Payment> findByCustomerId(String customerId, Pageable pageable);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    Page<Payment> findByMerchantIdAndStatus(String merchantId, Payment.PaymentStatus status, Pageable pageable);
}
