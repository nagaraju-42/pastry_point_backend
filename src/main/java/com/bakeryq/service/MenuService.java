package com.bakeryq.service;

import com.bakeryq.dto.request.MenuItemRequest;
import com.bakeryq.dto.response.MenuItemResponse;
import com.bakeryq.entity.Category;
import com.bakeryq.entity.MenuItem;
import com.bakeryq.exception.ResourceNotFoundException;
import com.bakeryq.repository.CategoryRepository;
import com.bakeryq.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    // ── Public menu endpoints ──────────────────────────────────────────────

    public List<MenuItemResponse> getAllAvailableItems() {
        return menuItemRepository.findByAvailableTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MenuItemResponse> getItemsByCategory(Long categoryId) {
        return menuItemRepository.findByCategoryIdAndAvailableTrue(categoryId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MenuItemResponse> getFeaturedItems() {
        return menuItemRepository.findByFeaturedTrueAndAvailableTrue()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MenuItemResponse> searchItems(String query) {
        return menuItemRepository.searchByNameOrDescription(query)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MenuItemResponse getItemById(Long id) {
        return toResponse(findMenuItemById(id));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    // ── Admin CRUD ─────────────────────────────────────────────────────────

    @Transactional
    public MenuItemResponse createItem(MenuItemRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        MenuItem item = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .imageUrl(request.getImageUrl())
                .available(request.getAvailable())
                .stockQuantity(request.getStockQuantity())
                .featured(request.getFeatured())
                .isVeg(request.getIsVeg())
                .preparationTimeMinutes(request.getPreparationTimeMinutes())
                .build();

        return toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse updateItem(Long id, MenuItemRequest request) {
        MenuItem item = findMenuItemById(id);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(category);
        item.setImageUrl(request.getImageUrl());
        item.setAvailable(request.getAvailable());
        item.setStockQuantity(request.getStockQuantity());
        item.setFeatured(request.getFeatured());
        item.setIsVeg(request.getIsVeg());
        item.setPreparationTimeMinutes(request.getPreparationTimeMinutes());

        return toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void toggleAvailability(Long id) {
        MenuItem item = findMenuItemById(id);
        item.setAvailable(!item.getAvailable());
        menuItemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        MenuItem item = findMenuItemById(id);
        menuItemRepository.delete(item);
    }

    @Transactional
    public MenuItemResponse updateStock(Long id, Integer quantity) {
        MenuItem item = findMenuItemById(id);
        item.setStockQuantity(quantity);
        return toResponse(menuItemRepository.save(item));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public MenuItem findMenuItemById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .imageUrl(item.getImageUrl())
                .available(item.getAvailable())
                .stockQuantity(item.getStockQuantity())
                .featured(item.getFeatured())
                .isVeg(item.getIsVeg())
                .preparationTimeMinutes(item.getPreparationTimeMinutes())
                .build();
    }
}
