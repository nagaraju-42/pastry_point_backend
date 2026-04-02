package com.bakeryq.service;

import com.bakeryq.dto.request.PlaceOrderRequest;
import com.bakeryq.dto.request.UpdateOrderStatusRequest;
import com.bakeryq.dto.response.OrderResponse;
import com.bakeryq.entity.*;
import com.bakeryq.exception.BusinessException;
import com.bakeryq.exception.ResourceNotFoundException;
import com.bakeryq.repository.*;
import com.bakeryq.util.DeliveryChargeCalculator;
import com.bakeryq.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final LoyaltyPointsRepository loyaltyRepo;
    private final DeliveryChargeCalculator deliveryCalculator;
    private final OrderNumberGenerator orderNumberGenerator;
    private final CartService cartService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;  // WebSocket

    // ── Place Order ────────────────────────────────────────────────────────

    @Transactional
    public OrderResponse placeOrder(String userEmail, PlaceOrderRequest request) {
        User user = findUser(userEmail);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty. Please add items before ordering.");
        }

        // Validate delivery address
        if (request.getOrderType() == Order.OrderType.DELIVERY
                && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new BusinessException("Delivery address is required for delivery orders");
        }

        BigDecimal subtotal = cart.getTotalAmount();
        BigDecimal deliveryCharge = request.getOrderType() == Order.OrderType.DELIVERY
                ? deliveryCalculator.calculate(subtotal)
                : BigDecimal.ZERO;

        // Loyalty points discount (100 points = ₹10 off)
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getRedeemLoyaltyPoints() != null && request.getRedeemLoyaltyPoints()) {
            int points = user.getLoyaltyPoints();
            if (points >= 100) {
                int redeemablePoints = (points / 100) * 100;   // Redeem in multiples of 100
                discount = BigDecimal.valueOf(redeemablePoints / 10.0);
                // Discount cannot exceed subtotal
                if (discount.compareTo(subtotal) > 0) {
                    discount = subtotal;
                }
            }
        }

        BigDecimal total = subtotal.add(deliveryCharge).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        // Estimate wait time (5 min per item type + base 5 min)
        int estimatedWait = 5 + (cart.getItems().size() * 3);

        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generate())
                .user(user)
                .subtotal(subtotal)
                .deliveryCharge(deliveryCharge)
                .discount(discount)
                .totalAmount(total)
                .orderType(request.getOrderType())
                .deliveryAddress(request.getDeliveryAddress())
                .specialInstructions(request.getSpecialInstructions())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .estimatedWaitMinutes(estimatedWait)
                .build();

        // Snapshot cart items into order items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(cartItem.getMenuItem())
                    .itemName(cartItem.getMenuItem().getName())
                    .itemPrice(cartItem.getMenuItem().getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            order.getItems().add(orderItem);

            // Reduce stock
            MenuItem menuItem = cartItem.getMenuItem();
            menuItem.setStockQuantity(menuItem.getStockQuantity() - cartItem.getQuantity());
        }

        Order saved = orderRepository.save(order);

        // Clear cart
        cartService.clearCartForUser(user.getId());

        // Deduct loyalty points if redeemed
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            int pointsUsed = discount.multiply(BigDecimal.TEN).intValue();
            user.setLoyaltyPoints(user.getLoyaltyPoints() - pointsUsed);
            loyaltyRepo.save(LoyaltyPoints.builder()
                    .user(user).points(-pointsUsed)
                    .transactionType(LoyaltyPoints.TransactionType.REDEEM)
                    .description("Redeemed for order " + saved.getOrderNumber())
                    .orderId(saved.getId()).build());
            userRepository.save(user);
        }

        log.info("Order placed: {} for user {}", saved.getOrderNumber(), userEmail);

        // Push new order to kitchen screen via WebSocket
        messagingTemplate.convertAndSend("/topic/kitchen", toResponse(saved));

        return toResponse(saved);
    }

    // ── After successful payment ───────────────────────────────────────────

    @Transactional
    public OrderResponse confirmPayment(Long orderId) {
        Order order = findOrderById(orderId);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        // Award loyalty points (1 point per ₹10 spent)
        int pointsEarned = saved.getTotalAmount().intValue() / 10;
        if (pointsEarned > 0) {
            User user = saved.getUser();
            user.setLoyaltyPoints(user.getLoyaltyPoints() + pointsEarned);
            userRepository.save(user);

            loyaltyRepo.save(LoyaltyPoints.builder()
                    .user(user).points(pointsEarned)
                    .transactionType(LoyaltyPoints.TransactionType.EARN)
                    .description("Earned for order " + saved.getOrderNumber())
                    .orderId(saved.getId()).build());
        }

        // Send confirmation email (async — won't block response)
        emailService.sendOrderConfirmation(saved);

        // Notify kitchen
        messagingTemplate.convertAndSend("/topic/kitchen", toResponse(saved));

        log.info("Payment confirmed for order {}", saved.getOrderNumber());
        return toResponse(saved);
    }

    // ── Admin: update order status ─────────────────────────────────────────

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = findOrderById(orderId);
        order.setStatus(request.getStatus());

        if (request.getEstimatedWaitMinutes() != null) {
            order.setEstimatedWaitMinutes(request.getEstimatedWaitMinutes());
        }

        Order saved = orderRepository.save(order);

        // Notify customer via WebSocket
        messagingTemplate.convertAndSend(
                "/queue/order-" + orderId, toResponse(saved));

        // Send "ready" email
        if (request.getStatus() == Order.OrderStatus.READY) {
            emailService.sendOrderReadyNotification(saved);
        }

        return toResponse(saved);
    }

    // ── Queries ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)

    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = findUser(userEmail);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long orderId, String userEmail) {
        Order order = findOrderById(orderId);
        User user = findUser(userEmail);

        // Students can only see their own orders; admins see all
        if (user.getRole() == User.Role.STUDENT
                && !order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("You can only view your own orders");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)

    public List<OrderResponse> getActiveOrdersForKitchen() {
        return orderRepository.findActiveOrdersForKitchen()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .orderItemId(item.getId())
                        .menuItemId(item.getMenuItem() != null ? item.getMenuItem().getId() : null)
                        .itemName(item.getItemName())
                        .itemPrice(item.getItemPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .userEmail(order.getUser().getEmail())
                .items(itemResponses)
                .subtotal(order.getSubtotal())
                .deliveryCharge(order.getDeliveryCharge())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .orderType(order.getOrderType())
                .deliveryAddress(order.getDeliveryAddress())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .specialInstructions(order.getSpecialInstructions())
                .estimatedWaitMinutes(order.getEstimatedWaitMinutes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
