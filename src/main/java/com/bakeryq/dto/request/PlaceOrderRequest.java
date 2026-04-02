package com.bakeryq.dto.request;

import com.bakeryq.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotNull(message = "Order type is required")
    private Order.OrderType orderType;   // PICKUP or DELIVERY

    private String deliveryAddress;      // Required only when orderType = DELIVERY

    private String specialInstructions;

    private Boolean redeemLoyaltyPoints = false;
}
