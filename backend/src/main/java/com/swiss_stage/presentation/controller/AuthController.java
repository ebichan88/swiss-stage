package com.swiss_stage.presentation.controller;

import com.swiss_stage.presentation.SecurityConfig;
import com.swiss_stage.presentation.api.ApiSuccess;
import com.swiss_stage.presentation.auth.CurrentUser;
import com.swiss_stage.presentation.auth.JwtSessionSupport;
import com.swiss_stage.presentation.auth.RedirectCookieSupport;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 認証(JWT Cookie基盤)。 Google OAuth2のリダイレクト・コールバックは Spring Security(SecurityConfig)が処理する。 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  public record MeDto(String sub, String name) {}

  private final JwtSessionSupport jwtSessionSupport;
  private final RedirectCookieSupport redirectCookieSupport;
  private final Clock clock;

  public AuthController(
      JwtSessionSupport jwtSessionSupport,
      RedirectCookieSupport redirectCookieSupport,
      Clock clock) {
    this.jwtSessionSupport = jwtSessionSupport;
    this.redirectCookieSupport = redirectCookieSupport;
    this.clock = clock;
  }

  /**
   * Google OAuth2へのリダイレクト起点(03_api_design.md)。実処理はSpring Securityのフィルタ。 {@code
   * redirect}は招待リンク経由の未認証ログインをログイン後に元の画面へ戻すためのもの (14_tournament_collaboration.md
   * §4.6)。安全な相対パスのみ短命Cookieに退避する。
   */
  @GetMapping("/login")
  public ResponseEntity<Void> login(
      @RequestParam(value = "redirect", required = false) String redirect) {
    ResponseEntity.BodyBuilder builder =
        ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(SecurityConfig.AUTHORIZATION_BASE_URI + "/google"));
    redirectCookieSupport
        .issue(redirect)
        .ifPresent(cookie -> builder.header(HttpHeaders.SET_COOKIE, cookie.toString()));
    return builder.build();
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
