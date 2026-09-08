package com.swiss_stage.presentation.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Google OAuth2認証成功時に自前のJWTセッションCookieを発行してSPAへ戻す(13_security_design.md §2)。 以降の認証はJWT
 * Cookieのみで行うため、OAuth2フロー用のHTTPセッションはここで破棄する。
 *
 * <p>招待リンク経由でログインした場合は{@code swiss_stage_redirect}Cookie(RedirectCookieSupport)を
 * 読んで元の画面へ戻す(14_tournament_collaboration.md §4.6)。Cookieの値は発行時点で安全な相対パス
 * であることを検証済みだが、ここでも再検証してから使う(多層防御)。
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final String DEFAULT_REDIRECT_PATH = "/tournaments";

  private final JwtSessionSupport jwtSessionSupport;
  private final RedirectCookieSupport redirectCookieSupport;
  private final String frontendBaseUrl;

  public OAuth2LoginSuccessHandler(
      JwtSessionSupport jwtSessionSupport,
      RedirectCookieSupport redirectCookieSupport,
      @Value("${app.auth.frontend-base-url:}") String frontendBaseUrl) {
    this.jwtSessionSupport = jwtSessionSupport;
    this.redirectCookieSupport = redirectCookieSupport;
    this.frontendBaseUrl = frontendBaseUrl;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    // Googleの user-name-attribute は sub(推測不能な数値文字列)
    String sub = principal.getName();
    Object name = principal.getAttribute("name");
    String token = jwtSessionSupport.issue(sub, name == null ? "運営者" : name.toString());

    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    response.addHeader(HttpHeaders.SET_COOKIE, jwtSessionSupport.sessionCookie(token).toString());
    response.addHeader(HttpHeaders.SET_COOKIE, redirectCookieSupport.expired().toString());
    response.sendRedirect(frontendBaseUrl + redirectPathFrom(request));
  }

  private static String redirectPathFrom(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return DEFAULT_REDIRECT_PATH;
    }
    for (Cookie cookie : cookies) {
      if (RedirectCookieSupport.COOKIE_NAME.equals(cookie.getName())
          && RedirectCookieSupport.isSafeRelativePath(cookie.getValue())) {
        return cookie.getValue();
      }
    }
    return DEFAULT_REDIRECT_PATH;
  }
}
