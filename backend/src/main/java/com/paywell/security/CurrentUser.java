package com.paywell.security;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static UUID id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AuthPrincipal p) {
            return p.userId();
        }
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
}
