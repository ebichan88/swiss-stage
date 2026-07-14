package com.swiss_stage.presentation.controller;

import com.swiss_stage.presentation.SecurityConfig;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import com.swiss_stage.presentation.auth.JwtSessionSupport;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認証(JWT Cookie基盤)。
 * Google OAuth2のリダイレクト・コールバックは Spring Security(SecurityConfig)が処理する。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public record MeDto(String sub, String name) {}

    private final JwtSessionSupport jwtSessionSupport;
    private final Clock clock;

    public AuthController(JwtSessionSupport jwtSessionSupport, Clock clock) {
        this.jwtSessionSupport = jwtSessionSupport;
        this.clock = clock;
    }

    /** Google OAuth2へのリダイレクト起点(03_api_design.md)。実処理はSpring Securityのフィルタ */
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(SecurityConfig.AUTHORIZATION_BASE_URI + "/google"))
                .build();
    }

    @GetMapping("/me")
    public ApiSuccess<MeDto> me(CurrentUser user) {
        return ApiSuccess.of(new MeDto(user.sub(), user.name()), Instant.now(clock));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiSuccess<Void>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtSessionSupport.expiredCookie().toString())
                .body(ApiSuccess.of(null, Instant.now(clock)));
    }
}
