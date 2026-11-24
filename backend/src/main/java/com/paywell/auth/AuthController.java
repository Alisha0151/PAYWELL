package com.paywell.auth;

import com.paywell.auth.AuthDtos.ForgotPasswordRequest;
import com.paywell.auth.AuthDtos.ForgotPasswordResponse;
import com.paywell.auth.AuthDtos.LoginRequest;
import com.paywell.auth.AuthDtos.RefreshRequest;
import com.paywell.auth.AuthDtos.ResetPasswordRequest;
import com.paywell.auth.AuthDtos.SignupRequest;
import com.paywell.auth.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest r, @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return service.signup(r, ua);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r, @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return service.login(r.email(), r.password(), ua);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest r, @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String ua) {
        return service.refresh(r.refreshToken(), ua);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest r) {
        service.logout(r.refreshToken());
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgot(@Valid @RequestBody ForgotPasswordRequest r) {
        return service.forgotPassword(r.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest r) {
        service.resetPassword(r);
    }
}
