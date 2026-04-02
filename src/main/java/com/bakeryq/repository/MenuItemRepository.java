package com.bakeryq.repository;

import com.bakeryq.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByCategoryIdAndAvailableTrue(Long categoryId);

    List<MenuItem> findByAvailableTrue();

    List<MenuItem> findByFeaturedTrueAndAvailableTrue();

    @Query("SELECT m FROM MenuItem m WHERE m.available = true AND " +
           "(LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<MenuItem> searchByNameOrDescription(@Param("query") String query);

    @Query("SELECT m FROM MenuItem m WHERE m.stockQuantity < :threshold")
    List<MenuItem> findLowStockItems(@Param("threshold") int threshold);
}
