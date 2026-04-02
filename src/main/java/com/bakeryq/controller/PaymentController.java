package com.bakeryq.controller;

import com.bakeryq.dto.request.PaymentVerifyRequest;
import com.bakeryq.dto.response.ApiResponse;
import com.bakeryq.dto.response.OrderResponse;
import com.bakeryq.dto.response.PaymentResponse;
import com.bakeryq.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Razorpay payment flow")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    // ← OrderService intentionally NOT injected here.
    //   confirmPayment() is called ONLY inside PaymentService.verifyAndConfirm().
    //   Never call it from the controller — causes double-confirm crash.
    private final PaymentService paymentService;

    @PostMapping("/create/{orderId}")
    @Operation(summary = "Step 1 — Create a Razorpay payment order")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        PaymentResponse response = paymentService.createRazorpayOrder(
                orderId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment order created", response));
    }

    @PostMapping("/verify/{orderId}")
    @Operation(summary = "Step 2 — Verify Razorpay signature + confirm order")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentVerifyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // verifyAndConfirm does: validate sig → capture payment → confirm order (ONCE)
        OrderResponse order = paymentService.verifyAndConfirm(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment successful! Order confirmed.", order));
    }

    @PostMapping("/failed/{orderId}")
    @Operation(summary = "Step 3 (failure) — Mark payment as failed")
    public ResponseEntity<ApiResponse<Void>> paymentFailed(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        paymentService.markPaymentFailed(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Payment failure recorded", null));
    }
}