package com.bakeryq.dto.request;

import com.bakeryq.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private Order.OrderStatus status;

    private Integer estimatedWaitMinutes;
}
