package com.swiss_stage.presentation.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.presentation.api.ApiError;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 共有トークン系エンドポイント(/api/v1/shared/**)のIPベース簡易レート制限
 * (13_security_design.md §5。トークン探索のブルートフォース対策)。
 * インメモリのbucket4jで足りる規模(単一インスタンス・MVP)を前提とする。
 */
@Component
public class SharedRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SharedRateLimitFilter.class);
    private static final String PATH_PREFIX = "/api/v1/shared/";
    /** メモリ保護の上限。超えたら全リセット(正規利用者への影響は一時的な制限緩和のみ) */
    private static final int MAX_TRACKED_IPS = 10_000;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final long capacity;
    private final long refillPerMinute;

    public SharedRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.shared.capacity:60}") long capacity,
            @Value("${app.rate-limit.shared.refill-per-minute:60}") long refillPerMinute) {
        this.objectMapper = objectMapper;
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // ALB配下では server.forward-headers-strategy によりクライアントIPが復元される
        Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), ip -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("errorCode={} sharedRateLimited", ErrorCode.RATE_LIMITED);
        response.setStatus(ErrorCode.RATE_LIMITED.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiError.of(
                ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.defaultMessage(), List.of())));
    }

    private Bucket newBucket() {
        if (buckets.size() >= MAX_TRACKED_IPS) {
            buckets.clear();
        }
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillGreedy(refillPerMinute, Duration.ofMinutes(1)))
                .build();
    }
}
