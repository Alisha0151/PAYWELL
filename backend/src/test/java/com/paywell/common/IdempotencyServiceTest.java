package com.paywell.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywell.TestProps;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IdempotencyServiceTest {
    private IdempotencyRepository repo;
    private IdempotencyService service;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repo = mock(IdempotencyRepository.class);
        service = new IdempotencyService(repo, TestProps.CLOCK);
    }

    @Test
    void headerIsRequiredAndValidated() {
        assertThatThrownBy(() -> service.requireValid(null)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED));
        assertThatThrownBy(() -> service.requireValid("  ")).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED));
        assertThatThrownBy(() -> service.requireValid("short")).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_INVALID));
        assertThatThrownBy(() -> service.requireValid("has spaces in it!!")).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_INVALID));
        assertThat(service.requireValid("7f9c2b1e-4d3a-4c55-9a10-0123456789ab")).isNotBlank();
    }

    @Test
    void hashIsStableAndSensitiveToEveryField() {
        String a = IdempotencyService.hash("SEND", "alice", "10.00", "note", null);
        assertThat(a).hasSize(64).isEqualTo(IdempotencyService.hash("SEND", "alice", "10.00", "note", null));
        assertThat(a).isNotEqualTo(IdempotencyService.hash("SEND", "alice", "10.01", "note", null));
        assertThat(a).isNotEqualTo(IdempotencyService.hash("SEND", "bob", "10.00", "note", null));
        // delimiter prevents ("ab","c") colliding with ("a","bc")
        assertThat(IdempotencyService.hash("ab", "c")).isNotEqualTo(IdempotencyService.hash("a", "bc"));
    }

    @Test
    void unknownKeyIsNotAReplay() {
        when(repo.findByUserIdAndIdemKey(user, "key-key-key-1")).thenReturn(Optional.empty());
        assertThat(service.findReplay(user, "key-key-key-1", "h")).isEmpty();
    }

    @Test
    void sameKeySameRequestReplaysStoredTransaction() {
        UUID tx = UUID.randomUUID();
        when(repo.findByUserIdAndIdemKey(user, "key-key-key-1"))
                .thenReturn(Optional.of(new IdempotencyKey(user, "key-key-key-1", "hash-1", tx, Instant.now())));
        assertThat(service.findReplay(user, "key-key-key-1", "hash-1")).contains(tx);
    }

    @Test
    void sameKeyDifferentRequestIsRejected() {
        when(repo.findByUserIdAndIdemKey(user, "key-key-key-1"))
                .thenReturn(Optional.of(new IdempotencyKey(user, "key-key-key-1", "hash-1", UUID.randomUUID(), Instant.now())));
        assertThatThrownBy(() -> service.findReplay(user, "key-key-key-1", "hash-2")).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED));
    }

    @Test
    void recordStoresUserKeyHashAndTransaction() {
        UUID tx = UUID.randomUUID();
        service.record(user, "key-key-key-2", "hash-9", tx);
        ArgumentCaptor<IdempotencyKey> c = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(repo).save(c.capture());
        assertThat(c.getValue().getTransactionId()).isEqualTo(tx);
        assertThat(c.getValue().getRequestHash()).isEqualTo("hash-9");
        verify(repo).save(any(IdempotencyKey.class));
    }
}
