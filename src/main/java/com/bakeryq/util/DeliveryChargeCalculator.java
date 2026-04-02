package com.bakeryq.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DeliveryChargeCalculator {

    @Value("${app.free-delivery-minimum:300}")
    private BigDecimal freeDeliveryMinimum;

    @Value("${app.delivery-charge:40}")
    private BigDecimal standardDeliveryCharge;

    /**
     * Returns the delivery charge for a given cart total.
     * If total >= freeDeliveryMinimum → ₹0
     * Otherwise → standard delivery charge (default ₹40)
     */
    public BigDecimal calculate(BigDecimal cartTotal) {
        if (cartTotal.compareTo(freeDeliveryMinimum) >= 0) {
            return BigDecimal.ZERO;
        }
        return standardDeliveryCharge;
    }

    /**
     * Returns how much more the customer needs to add to get free delivery.
     * Returns ZERO if already eligible.
     */
    public BigDecimal amountNeededForFreeDelivery(BigDecimal cartTotal) {
        BigDecimal needed = freeDeliveryMinimum.subtract(cartTotal);
        return needed.compareTo(BigDecimal.ZERO) > 0 ? needed : BigDecimal.ZERO;
    }

    public boolean isFreeDeliveryEligible(BigDecimal cartTotal) {
        return cartTotal.compareTo(freeDeliveryMinimum) >= 0;
    }

    public BigDecimal getFreeDeliveryMinimum() {
        return freeDeliveryMinimum;
    }
}
