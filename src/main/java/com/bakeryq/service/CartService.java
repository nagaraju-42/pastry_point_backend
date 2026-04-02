package com.bakeryq.service;

import com.bakeryq.dto.request.AddToCartRequest;
import com.bakeryq.dto.response.CartResponse;
import com.bakeryq.entity.Cart;
import com.bakeryq.entity.CartItem;
import com.bakeryq.entity.MenuItem;
import com.bakeryq.entity.User;
import com.bakeryq.exception.BusinessException;
import com.bakeryq.exception.ResourceNotFoundException;
import com.bakeryq.repository.CartRepository;
import com.bakeryq.repository.UserRepository;
import com.bakeryq.util.DeliveryChargeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final MenuService menuService;
    private final DeliveryChargeCalculator deliveryCalculator;

    public CartResponse getCart(String userEmail) {
        User user = findUser(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyCart(user));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(String userEmail, AddToCartRequest request) {
        User user = findUser(userEmail);
        MenuItem menuItem = menuService.findMenuItemById(request.getMenuItemId());

        // Validate item is available
        if (!menuItem.getAvailable()) {
            throw new BusinessException(menuItem.getName() + " is currently not available");
        }
        if (menuItem.getStockQuantity() < request.getQuantity()) {
            throw new BusinessException("Only " + menuItem.getStockQuantity()
                    + " units of " + menuItem.getName() + " are available");
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createEmptyCart(user));

        // If item already in cart, update quantity
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getMenuItem().getId().equals(request.getMenuItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .menuItem(menuItem)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(String userEmail, Long menuItemId, Integer quantity) {
        User user = findUser(userEmail);
        Cart cart = getCartEntity(user.getId());

        if (quantity <= 0) {
            return removeFromCart(userEmail, menuItemId);
        }

        cart.getItems().stream()
                .filter(i -> i.getMenuItem().getId().equals(menuItemId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeFromCart(String userEmail, Long menuItemId) {
        User user = findUser(userEmail);
        Cart cart = getCartEntity(user.getId());

        cart.getItems().removeIf(i -> i.getMenuItem().getId().equals(menuItemId));
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(String userEmail) {
        User user = findUser(userEmail);
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    // Called after order is placed
    @Transactional
    public void clearCartForUser(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Cart createEmptyCart(User user) {
        return cartRepository.save(Cart.builder().user(user).build());
    }

    private Cart getCartEntity(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Cart not found. Please add items first."));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CartResponse toResponse(Cart cart) {
        BigDecimal subtotal = cart.getTotalAmount();
        BigDecimal deliveryCharge = deliveryCalculator.calculate(subtotal);

        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartResponse.CartItemResponse.builder()
                        .cartItemId(item.getId())
                        .menuItemId(item.getMenuItem().getId())
                        .itemName(item.getMenuItem().getName())
                        .imageUrl(item.getMenuItem().getImageUrl())
                        .price(item.getMenuItem().getPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getMenuItem().getPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .deliveryCharge(deliveryCharge)
                .totalAmount(subtotal.add(deliveryCharge))
                .totalItems(cart.getTotalItems())
                .freeDeliveryEligible(deliveryCalculator.isFreeDeliveryEligible(subtotal))
                .amountNeededForFreeDelivery(deliveryCalculator.amountNeededForFreeDelivery(subtotal))
                .build();
    }
}
