package com.bakeryq.service;

import com.bakeryq.dto.request.PaymentVerifyRequest;
import com.bakeryq.dto.response.OrderResponse;
import com.bakeryq.dto.response.PaymentResponse;
import com.bakeryq.entity.Order;
import com.bakeryq.entity.Payment;
import com.bakeryq.exception.BusinessException;
import com.bakeryq.exception.PaymentFailedException;
import com.bakeryq.exception.ResourceNotFoundException;
import com.bakeryq.repository.OrderRepository;
import com.bakeryq.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final OrderRepository   orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService      orderService;
    private final EntityManager     entityManager;   // needed for flush

    // ── Step 1: Create Razorpay Order (with user check) ───────────────────────

    @Transactional
    public PaymentResponse createRazorpayOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException("You can only pay for your own orders");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BusinessException("This order has already been paid");
        }

        return createRazorpayOrderInternal(order);
    }

    // ── Step 1b: Create Razorpay Order (kiosk — no user check) ───────────────

    @Transactional
    public PaymentResponse createPaymentOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BusinessException("This order has already been paid");
        }

        return createRazorpayOrderInternal(order);
    }

    private PaymentResponse createRazorpayOrderInternal(Order order) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            int amountInPaise = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100)).intValue();

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", order.getOrderNumber());

            com.razorpay.Order rzpOrder = client.orders.create(options);
            String rzpOrderId = rzpOrder.get("id");

            // Delete stale payment record if one exists, flush before creating new one
            // to avoid @OneToOne unique constraint violation
            paymentRepository.findByOrder_Id(order.getId()).ifPresent(stale -> {
                paymentRepository.delete(stale);
                paymentRepository.flush();  // ← force DELETE before INSERT
                entityManager.clear();      // ← clear L1 cache
            });

            Payment payment = Payment.builder()
                    .order(order)
                    .razorpayOrderId(rzpOrderId)
                    .amount(order.getTotalAmount())
                    .status(Payment.PaymentStatus.CREATED)
                    .build();
            paymentRepository.save(payment);

            log.info("Razorpay order created: {} for BakeryQ order {}",
                    rzpOrderId, order.getOrderNumber());

            return PaymentResponse.builder()
                    .razorpayOrderId(rzpOrderId)
                    .razorpayKeyId(razorpayKeyId)
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .receipt(order.getOrderNumber())
                    .internalOrderId(order.getId())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}",
                    order.getOrderNumber(), e.getMessage());
            throw new PaymentFailedException(
                    "Unable to initiate payment: " + e.getMessage());
        }
    }

    // ── Step 2: Verify + Confirm — called ONCE from controller only ───────────

    @Transactional
    public OrderResponse verifyAndConfirm(Long orderId, PaymentVerifyRequest req) {

        // 1. Validate HMAC-SHA256 signature
        if (!isValidSignature(req)) {
            log.error("Signature mismatch for Razorpay order {}", req.getRazorpayOrderId());
            throw new PaymentFailedException(
                    "Payment signature verification failed.");
        }

        // 2. Find payment record
        Payment payment = paymentRepository
                .findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentFailedException(
                        "No payment record found for: " + req.getRazorpayOrderId()));

        // 3. Idempotency — already captured? return existing confirmed order
        if (payment.getStatus() == Payment.PaymentStatus.CAPTURED) {
            log.warn("Payment {} already captured — idempotent return", req.getRazorpayPaymentId());
            return orderService.toResponse(
                    orderRepository.findById(payment.getOrder().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Order", payment.getOrder().getId()))
            );
        }

        // 4. Update payment → CAPTURED
        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setRazorpaySignature(req.getRazorpaySignature());
        payment.setStatus(Payment.PaymentStatus.CAPTURED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 5. Confirm order — marks CONFIRMED, awards loyalty, sends email
        //    ONLY called here, never from controller separately
        OrderResponse confirmed = orderService.confirmPayment(payment.getOrder().getId());

        log.info("Payment {} verified. Order {} confirmed.",
                req.getRazorpayPaymentId(), confirmed.getOrderNumber());
        return confirmed;
    }

    // ── Step 3 (failure path) ─────────────────────────────────────────────────

    @Transactional
    public void markPaymentFailed(Long orderId, String reason) {
        paymentRepository.findByOrder_Id(orderId).ifPresent(p -> {
            if (p.getStatus() != Payment.PaymentStatus.CAPTURED) {
                p.setStatus(Payment.PaymentStatus.FAILED);
                p.setFailureReason(reason);
                p.setCompletedAt(LocalDateTime.now());
                paymentRepository.save(p);
            }
        });
        orderRepository.findById(orderId).ifPresent(o -> {
            if (o.getPaymentStatus() != Order.PaymentStatus.PAID) {
                o.setPaymentStatus(Order.PaymentStatus.FAILED);
                orderRepository.save(o);
            }
        });
        log.info("Payment marked failed for order {}: {}", orderId, reason);
    }

    // ── HMAC-SHA256 ───────────────────────────────────────────────────────────

    private boolean isValidSignature(PaymentVerifyRequest req) {
        try {
            String payload = req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    razorpayKeySecret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            String generated = sb.toString();

            boolean valid = generated.equals(req.getRazorpaySignature());
            if (!valid) {
                log.error("Sig FAILED. Payload={} | Generated={} | Received={}",
                        payload, generated, req.getRazorpaySignature());
            }
            return valid;
        } catch (Exception e) {
            log.error("Signature check threw: {}", e.getMessage(), e);
            return false;
        }
    }
}