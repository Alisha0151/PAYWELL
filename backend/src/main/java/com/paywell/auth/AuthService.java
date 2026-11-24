package com.paywell.auth;

import com.paywell.audit.AuditService;
import com.paywell.auth.AuthDtos.ForgotPasswordResponse;
import com.paywell.auth.AuthDtos.ResetPasswordRequest;
import com.paywell.auth.AuthDtos.SignupRequest;
import com.paywell.auth.AuthDtos.TokenResponse;
import com.paywell.auth.AuthDtos.UserSummary;
import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.Hashing;
import com.paywell.config.PaywellProperties;
import com.paywell.demo.DemoDataService;
import com.paywell.rewards.RewardService;
import com.paywell.security.JwtService;
import com.paywell.user.User;
import com.paywell.user.UserProfile;
import com.paywell.user.UserProfileRepository;
import com.paywell.user.UserRepository;
import com.paywell.user.UserStatus;
import com.paywell.wallet.Account;
import com.paywell.wallet.WalletService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final OtpService otp;
    private final WalletService wallet;
    private final DemoDataService demo;
    private final RewardService rewards;
    private final AuditService audit;
    private final PaywellProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    public AuthService(UserRepository users, UserProfileRepository profiles, AuthSessionRepository sessions, PasswordEncoder encoder,
                       JwtService jwt, OtpService otp, WalletService wallet, DemoDataService demo, RewardService rewards,
                       AuditService audit, PaywellProperties props, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.sessions = sessions;
        this.encoder = encoder;
        this.jwt = jwt;
        this.otp = otp;
        this.wallet = wallet;
        this.demo = demo;
        this.rewards = rewards;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
        this.dummyHash = encoder.encode("paywell-timing-equaliser-" + UUID.randomUUID());
    }

    @Transactional
    public TokenResponse signup(SignupRequest r, String userAgent) {
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        String handle = r.handle() != null ? r.handle().toLowerCase(Locale.ROOT) : generateHandle(email);
        if (profiles.existsByHandle(handle)) {
            throw new ApiException(ErrorCode.HANDLE_TAKEN);
        }
        Instant now = Instant.now(clock);
        User user = users.save(new User(UUID.randomUUID(), email, encoder.encode(r.password()), r.fullName(), r.phone(), now));
        profiles.save(new UserProfile(user.getId(), handle, props.payments().currency()));
        Account account = wallet.createAccount(user.getId());
        if (props.demo().enabled()) {
            demo.seed(account);
            rewards.grantWelcome(user.getId());
        }
        audit.record(user.getId(), "USER_SIGNUP", "User", user.getId().toString(), null);
        return issueTokens(user, handle, UUID.randomUUID(), userAgent);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse login(String emailRaw, String password, String userAgent) {
        Instant now = Instant.now(clock);
        User user = users.findByEmail(emailRaw.trim()).orElse(null);
        if (user == null) {
            encoder.matches(password, dummyHash);   // equalise timing for unknown emails
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.isLocked(now)) {
            audit.record(user.getId(), "LOGIN_BLOCKED_LOCKED", "User", user.getId().toString(), null);
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED, "Too many failed attempts. Try again later.");
        }
        if (!encoder.matches(password, user.getPasswordHash())) {
            user.registerFailedLogin(props.security().maxFailedLogins(), props.security().lockout(), now);
            audit.record(user.getId(), "LOGIN_FAILED", "User", user.getId().toString(), null);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        user.registerSuccessfulLogin(now);
        audit.record(user.getId(), "LOGIN_SUCCESS", "User", user.getId().toString(), null);
        String handle = profiles.findById(user.getId()).map(UserProfile::getHandle).orElse(null);
        return issueTokens(user, handle, UUID.randomUUID(), userAgent);
    }

    /** Rotates the refresh token. Presenting an already-rotated token revokes the whole family (token theft signal). */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String refreshToken, String userAgent) {
        Instant now = Instant.now(clock);
        AuthSession s = sessions.findByRefreshTokenHash(Hashing.sha256Hex(refreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (s.getRevokedAt() != null || !s.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        if (s.getRotatedAt() != null) {
            sessions.revokeFamily(s.getFamilyId(), now);
            audit.record(s.getUserId(), "REFRESH_REUSE_DETECTED", "AuthSession", s.getId().toString(), "session family revoked");
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        User user = users.findById(s.getUserId()).orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        s.markRotated(now);
        String handle = profiles.findById(user.getId()).map(UserProfile::getHandle).orElse(null);
        return issueTokens(user, handle, s.getFamilyId(), userAgent);
    }

    @Transactional
    public void logout(String refreshToken) {
        sessions.findByRefreshTokenHash(Hashing.sha256Hex(refreshToken)).ifPresent(s -> {
            sessions.revokeFamily(s.getFamilyId(), Instant.now(clock));
            audit.record(s.getUserId(), "LOGOUT", "AuthSession", s.getId().toString(), null);
        });
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(String emailRaw) {
        boolean configured = otp.deliveryConfigured();
        users.findByEmail(emailRaw.trim()).filter(u -> u.getStatus() == UserStatus.ACTIVE).ifPresent(u -> {
            otp.issue(u.getId(), u.getEmail(), OtpService.PASSWORD_RESET);
            audit.record(u.getId(), "PASSWORD_RESET_REQUESTED", "User", u.getId().toString(), null);
        });
        String msg = configured
                ? "If an account exists for that email, a one-time code has been sent."
                : "Request recorded. One-time-code delivery (SMS/email) is NOT configured in this environment, so no code was sent.";
        return new ForgotPasswordResponse(true, configured, msg);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(ResetPasswordRequest r) {
        User user = users.findByEmail(r.email().trim()).orElseThrow(() -> new ApiException(ErrorCode.INVALID_OTP));
        otp.verifyAndConsume(user.getId(), OtpService.PASSWORD_RESET, r.code());
        Instant now = Instant.now(clock);
        user.changePasswordHash(encoder.encode(r.newPassword()), now);
        sessions.revokeAllForUser(user.getId(), now);
        audit.record(user.getId(), "PASSWORD_RESET_COMPLETED", "User", user.getId().toString(), "all sessions revoked");
    }

    private TokenResponse issueTokens(User user, String handle, UUID familyId, String userAgent) {
        Instant now = Instant.now(clock);
        String refresh = jwt.newOpaqueToken();
        sessions.save(new AuthSession(user.getId(), familyId, Hashing.sha256Hex(refresh), now, now.plus(props.jwt().refreshTtl()), userAgent));
        return new TokenResponse(jwt.createAccessToken(user.getId(), user.getRole().name()), refresh, "Bearer", jwt.accessTtlSeconds(),
                new UserSummary(user.getId(), user.getEmail(), user.getFullName(), handle));
    }

    private String generateHandle(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._-]", "");
        if (base.length() < 3) base = base + "user";
        if (base.length() > 30) base = base.substring(0, 30);
        for (int i = 0; i < 10; i++) {
            String candidate = base + (1000 + random.nextInt(9000));
            if (!profiles.existsByHandle(candidate)) return candidate;
        }
        throw new ApiException(ErrorCode.HANDLE_TAKEN, "Could not allocate a handle; please choose one.");
    }
}
