package com.ndobaak.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.common.dto.ApiError;
import com.ndobaak.backend.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, RateLimit> RATE_LIMITS = Map.of(
        "/auth/login", new RateLimit(5, 60_000),
        "/auth/register/student", new RateLimit(5, 60_000),
        "/auth/register/landlord", new RateLimit(5, 60_000),
        "/auth/forgot-password", new RateLimit(3, 60_000),
        "/auth/reset-password", new RateLimit(5, 60_000),
        "/auth/refresh", new RateLimit(10, 60_000)
    );

    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        RateLimit limit = RATE_LIMITS.get(path);

        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = clientIp + ":" + path;
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new LinkedList<>());

        synchronized (timestamps) {
            long windowStart = now - limit.windowMs();
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit.maxRequests()) {
                writeRateLimitResponse(response);
                return;
            }

            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        ApiError error = ApiError.builder()
            .code(ErrorCode.RATE_LIMIT_EXCEEDED.name())
            .message(ErrorCode.RATE_LIMIT_EXCEEDED.getMessage())
            .retryable(ErrorCode.RATE_LIMIT_EXCEEDED.isRetryable())
            .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), error);
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        requestLog.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                timestamps.removeIf(ts -> ts < now - 120_000);
                return timestamps.isEmpty();
            }
        });
    }

    private record RateLimit(int maxRequests, long windowMs) {}
}
