package com.bakeryq.repository;

import com.bakeryq.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by the nested order.id (order is a @OneToOne relation)
    Optional<Payment> findByOrder_Id(Long orderId);

    // Keep a named alias for backward compat
    default Optional<Payment> findByOrderId(Long orderId) {
        return findByOrder_Id(orderId);
    }

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}