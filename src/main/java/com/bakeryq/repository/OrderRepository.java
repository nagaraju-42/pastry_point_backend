package com.bakeryq.repository;

import com.bakeryq.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatusInOrderByCreatedAtAsc(List<Order.OrderStatus> statuses);

    List<Order> findByStatusOrderByCreatedAtAsc(Order.OrderStatus status);

    // Today's orders for dashboard
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startOfDay ORDER BY o.createdAt DESC")
    List<Order> findTodaysOrders(@Param("startOfDay") LocalDateTime startOfDay);

    // Revenue analytics
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID' " +
           "AND o.createdAt >= :startOfDay")
    BigDecimal getTodaysRevenue(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'PAID' " +
           "AND o.createdAt >= :startOfWeek")
    BigDecimal getWeeklyRevenue(@Param("startOfWeek") LocalDateTime startOfWeek);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay")
    Long getTodaysOrderCount(@Param("startOfDay") LocalDateTime startOfDay);

    // Active orders for kitchen
    @Query("SELECT o FROM Order o WHERE o.status IN ('CONFIRMED', 'PREPARING') " +
           "ORDER BY o.createdAt ASC")
    List<Order> findActiveOrdersForKitchen();
}
