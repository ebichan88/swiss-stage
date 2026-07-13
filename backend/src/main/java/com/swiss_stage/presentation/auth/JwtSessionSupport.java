package com.swiss_stage.presentation.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 運営者セッションJWTの発行・検証とCookie組み立て(13_security_design.md §2)。
 * HttpOnly + SameSite=Lax。Secure属性はローカル開発(http)のみ無効化できる。
 * Google OAuth2での発行フローは Phase 5 で追加する。
 */
@Component
public class JwtSessionSupport {

    public static final String COOKIE_NAME = "swiss_stage_session";
    private static final Duration EXPIRY = Duration.ofHours(24);

    private final SecretKey key;
    private final boolean secureCookie;
    private final Clock clock;

    public JwtSessionSupport(
            @Value("${app.auth.jwt-secret}") String secret,
            @Value("${app.auth.secure-cookie:true}") boolean secureCookie,
            Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "app.auth.jwt-secret(環境変数JWT_SECRET)は32文字以上で設定してください");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.secureCookie = secureCookie;
        this.clock = clock;
    }

    public String issue(String sub, String name) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .subject(sub)
                .claim("name", name)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EXPIRY)))
                .signWith(key)
                .compact();
    }

    /** 不正・期限切れトークンはempty(=未認証扱い)。例外は投げない */
    public Optional<CurrentUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new CurrentUser(claims.getSubject(), claims.get("name", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public ResponseCookie sessionCookie(String token) {
        return baseCookie(token).maxAge(EXPIRY).build();
    }

    public ResponseCookie expiredCookie() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/");
    }
}
