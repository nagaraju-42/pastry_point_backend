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
public class DashboardStatsResponse {

    private BigDecimal todaysRevenue;
    private BigDecimal weeklyRevenue;
    private Long todaysOrderCount;
    private Long totalOrderCount;
    private Long pendingOrderCount;
    private Long preparingOrderCount;
    private Long totalCustomers;
    private List<LowStockAlert> lowStockAlerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockAlert {
        private Long menuItemId;
        private String itemName;
        private Integer currentStock;
    }
}
