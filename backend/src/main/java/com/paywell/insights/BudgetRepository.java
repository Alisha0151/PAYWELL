package com.paywell.insights;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserIdOrderByCategoryAsc(UUID userId);

    Optional<Budget> findByUserIdAndCategory(UUID userId, String category);
}
