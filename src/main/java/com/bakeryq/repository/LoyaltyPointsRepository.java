package com.bakeryq.repository;

import com.bakeryq.entity.LoyaltyPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, Long> {

    List<LoyaltyPoints> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoints lp WHERE lp.user.id = :userId")
    Integer getTotalPointsByUserId(@Param("userId") Long userId);
}
