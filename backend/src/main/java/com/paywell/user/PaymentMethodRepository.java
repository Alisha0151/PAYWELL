package com.paywell.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);
}
