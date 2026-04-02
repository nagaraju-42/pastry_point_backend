package com.bakeryq.controller;

import com.bakeryq.dto.response.ApiResponse;
import com.bakeryq.dto.response.MenuItemResponse;
import com.bakeryq.entity.Category;
import com.bakeryq.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Browse and manage menu items")
public class MenuController {

    private final MenuService menuService;

    // ── Public endpoints ──────────────────────────────────────────────────

    @GetMapping("/menu")
    @Operation(summary = "Get all available menu items")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAllItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {

        List<MenuItemResponse> items;
        if (search != null && !search.isBlank()) {
            items = menuService.searchItems(search);
        } else if (categoryId != null) {
            items = menuService.getItemsByCategory(categoryId);
        } else {
            items = menuService.getAllAvailableItems();
        }
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/menu/featured")
    @Operation(summary = "Get featured menu items for homepage")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getFeaturedItems()));
    }

    @GetMapping("/menu/{id}")
    @Operation(summary = "Get a single menu item by ID")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getItemById(id)));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all active categories")
    public ResponseEntity<ApiResponse<List<Category>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAllCategories()));
    }

}
