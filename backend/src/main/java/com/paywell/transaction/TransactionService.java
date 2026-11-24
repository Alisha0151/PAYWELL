package com.paywell.transaction;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public record Filter(TxType type, Direction direction, Category category, TxStatus status, Instant from, Instant to, String query) {}

    @Transactional(readOnly = true)
    public PageResponse<TransactionView> list(UUID userId, Filter f, int page, int size) {
        Specification<Transaction> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("userId"), userId));
            if (f.type() != null) p.add(cb.equal(root.get("type"), f.type()));
            if (f.direction() != null) p.add(cb.equal(root.get("direction"), f.direction()));
            if (f.category() != null) p.add(cb.equal(root.get("category"), f.category()));
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
            if (f.to() != null) p.add(cb.lessThan(root.get("createdAt"), f.to()));
            if (f.query() != null && !f.query().isBlank()) {
                String like = "%" + escape(f.query().trim().toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("counterpartyName")), like, '\\'),
                        cb.like(cb.lower(root.get("counterpartyHandle")), like, '\\'),
                        cb.like(cb.lower(root.get("reference")), like, '\\')));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(repository.findAll(spec, pageable), TransactionView::of);
    }

    @Transactional(readOnly = true)
    public TransactionView get(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).map(TransactionView::of)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Transaction not found."));
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
