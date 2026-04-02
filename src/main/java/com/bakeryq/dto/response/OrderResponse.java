package com.bakeryq.dto.response;

import com.bakeryq.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userName;
    private String userEmail;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryCharge;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private Order.OrderType orderType;
    private String deliveryAddress;
    private Order.OrderStatus status;
    private Order.PaymentStatus paymentStatus;
    private String specialInstructions;
    private Integer estimatedWaitMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long orderItemId;
        private Long menuItemId;
        private String itemName;
        private BigDecimal itemPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
    }
}
