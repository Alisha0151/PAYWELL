package com.paywell.insights;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringBillRepository extends JpaRepository<RecurringBill, UUID> {
    List<RecurringBill> findByUserIdAndActiveTrueOrderByNextDueDateAsc(UUID userId);

    Optional<RecurringBill> findByIdAndUserId(UUID id, UUID userId);
}
