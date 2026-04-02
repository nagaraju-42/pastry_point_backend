package com.bakeryq.controller;

import com.bakeryq.dto.request.UpdateOrderStatusRequest;
import com.bakeryq.dto.response.ApiResponse;
import com.bakeryq.dto.response.OrderResponse;
import com.bakeryq.entity.Order;
import com.bakeryq.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN_STAFF')")
@RequiredArgsConstructor
@Tag(name = "Kitchen Display", description = "Kitchen screen order management")
@SecurityRequirement(name = "bearerAuth")
public class KitchenController {

    private final OrderService orderService;

    @GetMapping("/orders")
    @Operation(summary = "Get all active orders for kitchen display (CONFIRMED + PREPARING)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getActiveOrders() {
        return ResponseEntity.ok(
                ApiResponse.success(orderService.getActiveOrdersForKitchen()));
    }

    @PostMapping("/orders/{orderId}/start")
    @Operation(summary = "Mark order as PREPARING (kitchen started working on it)")
    public ResponseEntity<ApiResponse<OrderResponse>> startPreparing(@PathVariable Long orderId) {
        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(Order.OrderStatus.PREPARING);
        return ResponseEntity.ok(
                ApiResponse.success("Order is being prepared",
                        orderService.updateOrderStatus(orderId, req)));
    }

    @PostMapping("/orders/{orderId}/ready")
    @Operation(summary = "Mark order as READY (customer can pick up / dispatch delivery)")
    public ResponseEntity<ApiResponse<OrderResponse>> markReady(@PathVariable Long orderId) {
        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(Order.OrderStatus.READY);
        return ResponseEntity.ok(
                ApiResponse.success("Order is ready!",
                        orderService.updateOrderStatus(orderId, req)));
    }

    @PostMapping("/orders/{orderId}/delivered")
    @Operation(summary = "Mark order as DELIVERED (for delivery orders)")
    public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(@PathVariable Long orderId) {
        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(Order.OrderStatus.DELIVERED);
        return ResponseEntity.ok(
                ApiResponse.success("Order delivered",
                        orderService.updateOrderStatus(orderId, req)));
    }
}
