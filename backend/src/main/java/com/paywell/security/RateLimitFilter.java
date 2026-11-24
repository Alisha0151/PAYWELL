package com.paywell.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywell.common.ErrorCode;
import com.paywell.common.GlobalExceptionHandler;
import com.paywell.config.PaywellProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate-limit-lite: a fixed one-minute window per client. In-memory and per-instance, so it is a first line of defence
 * against credential stuffing and runaway clients, NOT a distributed limiter. Use a gateway/Redis limiter when scaling out.
 * Auth endpoints are keyed by client address; other endpoints by client address too (the filter runs before authentication).
 */
public class RateLimitFilter extends OncePerRequestFilter {
    private record Window(long minute, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int authLimit;
    private final int apiLimit;
    private final Clock clock;
    private final ObjectMapper mapper;

    public RateLimitFilter(PaywellProperties props, Clock clock, ObjectMapper mapper) {
        this.authLimit = props.security().rateLimit().authPerMinute();
        this.apiLimit = props.security().rateLimit().apiPerMinute();
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        boolean auth = req.getRequestURI().startsWith("/api/v1/auth/");
        int limit = auth ? authLimit : apiLimit;
        String key = (auth ? "auth:" : "api:") + req.getRemoteAddr();
        long minute = clock.millis() / 60_000;
        Window w = windows.merge(key, new Window(minute, 1), (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);
        if (windows.size() > 50_000) {
            windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        }
        if (w.count() > limit) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(60 - (clock.millis() / 1000) % 60));
            res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            ProblemDetail pd = GlobalExceptionHandler.problem(ErrorCode.RATE_LIMITED, "Please slow down and retry shortly.");
            mapper.writeValue(res.getOutputStream(), pd);
            return;
        }
        chain.doFilter(req, res);
    }
}
