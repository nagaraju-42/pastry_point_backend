package com.bakeryq.controller;

import com.bakeryq.dto.request.MenuItemRequest;
import com.bakeryq.dto.response.ApiResponse;
import com.bakeryq.dto.response.DashboardStatsResponse;
import com.bakeryq.dto.response.MenuItemResponse;
import com.bakeryq.dto.response.OrderResponse;
import com.bakeryq.service.AdminService;
import com.bakeryq.service.MenuService;
import com.bakeryq.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only endpoints for shop management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final MenuService menuService;
    private final OrderService orderService;

    // ── Dashboard Stats ────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Get today's revenue, order counts, and low stock alerts")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    // ── Menu Management ────────────────────────────────────────────────────

    @PostMapping("/menu")
    @Operation(summary = "Add a new menu item")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @Valid @RequestBody MenuItemRequest request) {

        MenuItemResponse item = menuService.createItem(request);
        return ResponseEntity.ok(ApiResponse.success("Menu item added", item));
    }

    @PutMapping("/menu/{itemId}")
    @Operation(summary = "Update an existing menu item")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Long itemId,
            @Valid @RequestBody MenuItemRequest request) {

        MenuItemResponse item = menuService.updateItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Menu item updated", item));
    }

    @DeleteMapping("/menu/{itemId}")
    @Operation(summary = "Delete a menu item")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Long itemId) {
        menuService.deleteItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted", null));
    }

    @PatchMapping("/menu/{itemId}/availability")
    @Operation(summary = "Toggle item availability (mark as out-of-stock)")
    public ResponseEntity<ApiResponse<Void>> toggleAvailability(@PathVariable Long itemId) {
        menuService.toggleAvailability(itemId);
        return ResponseEntity.ok(ApiResponse.success("Availability toggled", null));
    }

    @PatchMapping("/menu/{itemId}/stock")
    @Operation(summary = "Update stock quantity for an item")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateStock(
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {

        MenuItemResponse item = menuService.updateStock(itemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated", item));
    }

    // ── Order Management ───────────────────────────────────────────────────

    @GetMapping("/orders")
    @Operation(summary = "Get all orders (most recent first)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @GetMapping("/orders/today")
    @Operation(summary = "Get today's orders only")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getTodaysOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    // ── Revenue Reports ────────────────────────────────────────────────────

    @GetMapping("/reports/revenue")
    @Operation(summary = "Get revenue report (daily/weekly)")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getRevenueReport() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }
}
