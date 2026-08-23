package com.swiss_stage.unit.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.presentation.auth.JwtSessionSupport;
import com.swiss_stage.presentation.auth.OAuth2LoginSuccessHandler;
import com.swiss_stage.presentation.auth.RedirectCookieSupport;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * ログイン後リダイレクト先の決定ロジック(14_tournament_collaboration.md §4.6)。 実際のGoogle
 * OAuth2ハンドシェイクを経由せず、AuthenticationSuccessHandlerを直接呼び出して検証する (MockMvcでは到達できないため。GET
 * /auth/login?redirect=側の検証はAuthApiTestを参照)。
 */
class OAuth2LoginSuccessHandlerTest {

  private final JwtSessionSupport jwtSessionSupport =
      new JwtSessionSupport("test-secret-at-least-32-characters-long!!", false, Clock.systemUTC());
  private final RedirectCookieSupport redirectCookieSupport = new RedirectCookieSupport(false);
  private final OAuth2LoginSuccessHandler handler =
      new OAuth2LoginSuccessHandler(jwtSessionSupport, redirectCookieSupport, "");

  @Test
  @DisplayName("MBR-AC-014: redirect Cookieが安全な相対パスならログイン後にその画面へ戻る")
  void 安全な相対パスへリダイレクトする() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(RedirectCookieSupport.COOKIE_NAME, "/invite/abc123"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, authenticationOf("user-sub"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/invite/abc123");
  }

  @Test
  @DisplayName("MBR-AC-014: redirect Cookieが絶対URLなら無視して/tournamentsへ戻す")
  void 絶対URLは既定に倒す() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(RedirectCookieSupport.COOKIE_NAME, "https://evil.example.com"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, authenticationOf("user-sub"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/tournaments");
  }

  @Test
  @DisplayName("MBR-AC-014: redirect Cookieが\"//\"始まり(プロトコル相対URL)なら無視して/tournamentsへ戻す")
  void プロトコル相対URLは既定に倒す() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(RedirectCookieSupport.COOKIE_NAME, "//evil.example.com"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, authenticationOf("user-sub"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/tournaments");
  }

  @Test
  @DisplayName("MBR-AC-014: redirect Cookieが\"/\\\\\"始まり(バックスラッシュによるオーソリティ偽装)なら無視して/tournamentsへ戻す")
  void 先頭スラッシュ直後のバックスラッシュは既定に倒す() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(RedirectCookieSupport.COOKIE_NAME, "/\\evil.example.com"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, authenticationOf("user-sub"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/tournaments");
  }

  @Test
  @DisplayName("redirect Cookieが無ければ/tournamentsへ戻る")
  void Cookieが無ければ既定へ() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationSuccess(request, response, authenticationOf("user-sub"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/tournaments");
  }

  private static Authentication authenticationOf(String sub) {
    OAuth2User principal =
        new DefaultOAuth2User(List.of(), Map.of("sub", sub, "name", "テスト太郎"), "sub");
    return new TestingAuthenticationToken(principal, null);
  }
}
