package com.paywell.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    @Query("select p from UserProfile p where lower(p.handle) = lower(:handle)")
    Optional<UserProfile> findByHandle(@Param("handle") String handle);

    @Query("select count(p) > 0 from UserProfile p where lower(p.handle) = lower(:handle)")
    boolean existsByHandle(@Param("handle") String handle);
}
