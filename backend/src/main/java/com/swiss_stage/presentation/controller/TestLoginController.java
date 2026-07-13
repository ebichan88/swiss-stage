package com.swiss_stage.presentation.controller;

import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.JwtSessionSupport;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 開発・テスト用の仮ログイン(Google OAuth2はPhase 5)。
 * local / test プロファイル限定。本番プロファイルではエンドポイント自体が存在しない。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile({"local", "test"})
@Validated
public class TestLoginController {

    public record TestLoginRequest(
            @Size(max = 100) String sub,
            @Size(max = 100) String name) {}

    private final JwtSessionSupport jwtSessionSupport;
    private final Clock clock;

    public TestLoginController(JwtSessionSupport jwtSessionSupport, Clock clock) {
        this.jwtSessionSupport = jwtSessionSupport;
        this.clock = clock;
    }

    @PostMapping("/test-login")
    public ResponseEntity<ApiSuccess<AuthController.MeDto>> testLogin(
            @RequestBody(required = false) TestLoginRequest request) {
        String sub = request == null || request.sub() == null || request.sub().isBlank()
                ? "test-user" : request.sub();
        String name = request == null || request.name() == null || request.name().isBlank()
                ? "テストユーザー" : request.name();
        String token = jwtSessionSupport.issue(sub, name);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtSessionSupport.sessionCookie(token).toString())
                .body(ApiSuccess.of(new AuthController.MeDto(sub, name), Instant.now(clock)));
    }
}
