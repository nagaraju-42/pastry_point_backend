package com.bakeryq.service;

import com.bakeryq.dto.response.DashboardStatsResponse;
import com.bakeryq.dto.response.OrderResponse;
import com.bakeryq.entity.MenuItem;
import com.bakeryq.repository.MenuItemRepository;
import com.bakeryq.repository.OrderRepository;
import com.bakeryq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    private static final int LOW_STOCK_THRESHOLD = 10;

    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime startOfDay  = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now()
                .with(DayOfWeek.MONDAY).atStartOfDay();

        BigDecimal todaysRevenue = orZero(orderRepository.getTodaysRevenue(startOfDay));
        BigDecimal weeklyRevenue = orZero(orderRepository.getWeeklyRevenue(startOfWeek));

        Long todaysOrders  = orZero(orderRepository.getTodaysOrderCount(startOfDay));
        Long totalOrders   = orderRepository.count();
        Long totalCustomers = userRepository.count();

        long pending   = orderRepository.findByStatusOrderByCreatedAtAsc(
                com.bakeryq.entity.Order.OrderStatus.PENDING).size();
        long preparing = orderRepository.findByStatusOrderByCreatedAtAsc(
                com.bakeryq.entity.Order.OrderStatus.PREPARING).size();

        List<MenuItem> lowStockItems = menuItemRepository.findLowStockItems(LOW_STOCK_THRESHOLD);
        List<DashboardStatsResponse.LowStockAlert> alerts = lowStockItems.stream()
                .map(m -> DashboardStatsResponse.LowStockAlert.builder()
                        .menuItemId(m.getId())
                        .itemName(m.getName())
                        .currentStock(m.getStockQuantity())
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .todaysRevenue(todaysRevenue)
                .weeklyRevenue(weeklyRevenue)
                .todaysOrderCount(todaysOrders)
                .totalOrderCount(totalOrders)
                .pendingOrderCount(pending)
                .preparingOrderCount(preparing)
                .totalCustomers(totalCustomers)
                .lowStockAlerts(alerts)
                .build();
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long orZero(Long value) {
        return value != null ? value : 0L;
    }

    public List<OrderResponse> getTodaysOrders() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return orderRepository.findTodaysOrders(startOfDay).stream()
                .map(order -> OrderResponse.builder()
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .userId(order.getUser().getId())
                        .userName(order.getUser().getName())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .paymentStatus(order.getPaymentStatus())
                        .orderType(order.getOrderType())
                        .createdAt(order.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
