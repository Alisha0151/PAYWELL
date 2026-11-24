package com.paywell.payment;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.payment.PaymentDtos.CreateRecipientRequest;
import com.paywell.payment.PaymentDtos.FavoriteRequest;
import com.paywell.payment.PaymentDtos.RecipientView;
import com.paywell.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipients")
public class RecipientController {
    private final RecipientRepository repository;
    private final Clock clock;

    public RecipientController(RecipientRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    private static RecipientView view(Recipient r) {
        return new RecipientView(r.getId(), r.getName(), r.getHandle(), r.isFavorite());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<RecipientView> list() {
        return repository.findByUserIdOrderByFavoriteDescNameAsc(CurrentUser.id()).stream().map(RecipientController::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RecipientView create(@Valid @RequestBody CreateRecipientRequest req) {
        UUID me = CurrentUser.id();
        String handle = req.handle().trim().toLowerCase(Locale.ROOT);
        if (repository.findByUserIdAndHandle(me, handle).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "Recipient already saved.");
        }
        return view(repository.save(new Recipient(me, req.name().trim(), handle, Instant.now(clock))));
    }

    @PutMapping("/{id}/favorite")
    @Transactional
    public RecipientView favorite(@PathVariable UUID id, @Valid @RequestBody FavoriteRequest req) {
        Recipient r = repository.findByIdAndUserId(id, CurrentUser.id()).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        r.setFavorite(req.favorite());
        return view(r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id) {
        Recipient r = repository.findByIdAndUserId(id, CurrentUser.id()).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        repository.delete(r);   // history keeps counterparty name/handle; transactions.recipient_id is ON DELETE SET NULL
    }
}
