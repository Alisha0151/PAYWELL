package com.paywell.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequestEntity, UUID> {
    List<PaymentRequestEntity> findByRequesterUserIdOrderByCreatedAtDesc(UUID requesterUserId);

    List<PaymentRequestEntity> findByPayerUserIdOrderByCreatedAtDesc(UUID payerUserId);

    Optional<PaymentRequestEntity> findByIdAndPayerUserId(UUID id, UUID payerUserId);

    Optional<PaymentRequestEntity> findByIdAndRequesterUserId(UUID id, UUID requesterUserId);
}
