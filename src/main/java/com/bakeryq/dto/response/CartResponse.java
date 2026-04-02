package com.bakeryq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long cartId;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryCharge;
    private BigDecimal totalAmount;
    private int totalItems;
    private boolean freeDeliveryEligible;
    private BigDecimal amountNeededForFreeDelivery;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long cartItemId;
        private Long menuItemId;
        private String itemName;
        private String imageUrl;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal lineTotal;
    }
}
