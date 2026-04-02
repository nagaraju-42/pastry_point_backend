package com.bakeryq.controller;

import com.bakeryq.dto.request.PlaceOrderRequest;
import com.bakeryq.dto.request.UpdateOrderStatusRequest;
import com.bakeryq.dto.response.ApiResponse;
import com.bakeryq.dto.response.OrderResponse;
import com.bakeryq.service.OrderService;
import com.bakeryq.util.PdfReceiptGenerator;
import com.bakeryq.exception.ResourceNotFoundException;
import com.bakeryq.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Place and manage orders")
public class OrderController {

    private final OrderService orderService;
    private final PdfReceiptGenerator pdfGenerator;
    private final OrderRepository orderRepository;

    // ── Student endpoints ─────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Place a new order from cart")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceOrderRequest request) {

        OrderResponse order = orderService.placeOrder(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully!", order));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's order history")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getMyOrders(userDetails.getUsername())));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderById(orderId, userDetails.getUsername())));
    }

    @GetMapping("/{orderId}/receipt")
    @Operation(summary = "Download PDF receipt for an order")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        byte[] pdf = pdfGenerator.generateReceipt(order);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Receipt-" + order.getOrderNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Admin endpoints ───────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin only)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @PatchMapping("/admin/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN_STAFF')")
    @Operation(summary = "Update order status (Admin / Kitchen only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated",
                orderService.updateOrderStatus(orderId, request)));
    }

    // ── Kitchen display ───────────────────────────────────────────────────

    @GetMapping("/kitchen/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN_STAFF')")
    @Operation(summary = "Get active orders for kitchen display screen")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getKitchenOrders() {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getActiveOrdersForKitchen()));
    }
}
