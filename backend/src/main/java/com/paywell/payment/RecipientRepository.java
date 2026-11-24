package com.paywell.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    List<Recipient> findByUserIdOrderByFavoriteDescNameAsc(UUID userId);

    Optional<Recipient> findByUserIdAndHandle(UUID userId, String handle);

    Optional<Recipient> findByIdAndUserId(UUID id, UUID userId);
}
