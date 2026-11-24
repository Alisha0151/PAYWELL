package com.paywell.security;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String role) {}
