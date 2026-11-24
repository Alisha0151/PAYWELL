package com.paywell.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {
    Optional<OtpChallenge> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId, String purpose);

    @Modifying
    @Query("update OtpChallenge o set o.consumedAt = :now where o.userId = :userId and o.purpose = :purpose and o.consumedAt is null")
    int invalidateOpen(@Param("userId") UUID userId, @Param("purpose") String purpose, @Param("now") Instant now);
}
