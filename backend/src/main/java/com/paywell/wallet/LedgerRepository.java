package com.paywell.wallet;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/** Insert + read only (not a CrudRepository), mirroring the immutability trigger. */
public interface LedgerRepository extends Repository<LedgerEntry, Long> {
    LedgerEntry save(LedgerEntry entry);

    Page<LedgerEntry> findByAccountIdOrderByIdDesc(UUID accountId, Pageable pageable);
}
