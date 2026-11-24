package com.paywell.user;

import com.paywell.audit.AuditService;
import com.paywell.auth.AuthSessionRepository;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.PageResponse;
import com.paywell.user.ProfileDtos.ActivityView;
import com.paywell.user.ProfileDtos.AddPaymentMethodRequest;
import com.paywell.user.ProfileDtos.ChangePasswordRequest;
import com.paywell.user.ProfileDtos.PaymentMethodView;
import com.paywell.user.ProfileDtos.ProfileView;
import com.paywell.user.ProfileDtos.UpdateProfileRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PaymentMethodRepository methods;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final Clock clock;

    public ProfileService(UserRepository users, UserProfileRepository profiles, PaymentMethodRepository methods,
                          AuthSessionRepository sessions, PasswordEncoder encoder, AuditService audit, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.methods = methods;
        this.sessions = sessions;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileView get(UUID userId) {
        return toView(load(userId), loadProfile(userId));
    }

    @Transactional
    public ProfileView update(UUID userId, UpdateProfileRequest r) {
        User u = load(userId);
        UserProfile p = loadProfile(userId);
        u.updateDetails(r.fullName(), r.phone(), Instant.now(clock));
        p.update(r.language(), r.notifyPayments(), r.notifyRewards(), r.notifyBills(), r.notifySecurity(), r.hideBalanceDefault(), r.shareAnalytics());
        audit.record(userId, "PROFILE_UPDATED", "User", userId.toString(), null);
        return toView(u, p);
    }

    /** Changing the password revokes every refresh-token session, forcing re-login on all devices. */
    @Transactional(noRollbackFor = ApiException.class)
    public void changePassword(UUID userId, ChangePasswordRequest r) {
        User u = load(userId);
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) {
            audit.record(userId, "PASSWORD_CHANGE_FAILED", "User", userId.toString(), null);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect.");
        }
        Instant now = Instant.now(clock);
        u.changePasswordHash(encoder.encode(r.newPassword()), now);
        sessions.revokeAllForUser(userId, now);
        audit.record(userId, "PASSWORD_CHANGED", "User", userId.toString(), "all sessions revoked");
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityView> activity(UUID userId, int page, int size) {
        return PageResponse.of(audit.recent(userId, Math.max(page, 0), size),
                e -> new ActivityView(e.getId(), e.getAction(), e.getEntityType(), e.getDetail(), e.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodView> methods(UUID userId) {
        return methods.findByUserIdOrderByCreatedAtAsc(userId).stream().map(ProfileService::view).toList();
    }

    @Transactional
    public PaymentMethodView addMethod(UUID userId, AddPaymentMethodRequest r) {
        boolean first = methods.findByUserIdOrderByCreatedAtAsc(userId).isEmpty();
        PaymentMethod m = methods.save(new PaymentMethod(UUID.randomUUID(), userId, r.kind(), r.label().trim(), r.last4(), first, Instant.now(clock)));
        audit.record(userId, "PAYMENT_METHOD_ADDED", "PaymentMethod", m.getId().toString(), "kind=" + r.kind());
        return view(m);
    }

    @Transactional
    public void deleteMethod(UUID userId, UUID id) {
        PaymentMethod m = methods.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        methods.delete(m);
        audit.record(userId, "PAYMENT_METHOD_REMOVED", "PaymentMethod", id.toString(), null);
    }

    private User load(UUID id) {
        return users.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private UserProfile loadProfile(UUID id) {
        return profiles.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private static PaymentMethodView view(PaymentMethod m) {
        return new PaymentMethodView(m.getId(), m.getKind(), m.getLabel(), m.getLast4(), m.getStatus(), m.isDefault());
    }

    private static ProfileView toView(User u, UserProfile p) {
        return new ProfileView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), p.getHandle(), p.getCurrency(), p.getLanguage(),
                p.isNotifyPayments(), p.isNotifyRewards(), p.isNotifyBills(), p.isNotifySecurity(), p.isHideBalanceDefault(),
                p.isShareAnalytics(), u.getCreatedAt());
    }
}
