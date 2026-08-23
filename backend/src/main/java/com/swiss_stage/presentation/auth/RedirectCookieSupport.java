package com.swiss_stage.presentation.auth;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * ログイン後リダイレクト先の一時退避Cookie(14_tournament_collaboration.md §4.6)。
 * 招待リンク経由で未認証のままログインしたユーザーを、ログイン後に元の招待画面へ戻すために使う。 HttpOnly + SameSite=Lax +
 * 短命(10分)。JwtSessionSupportのCookie組み立てと同じ構成。
 */
@Component
public class RedirectCookieSupport {

  public static final String COOKIE_NAME = "swiss_stage_redirect";
  private static final Duration EXPIRY = Duration.ofMinutes(10);

  private final boolean secureCookie;

  public RedirectCookieSupport(@Value("${app.auth.secure-cookie:true}") boolean secureCookie) {
    this.secureCookie = secureCookie;
  }

  /** 安全な相対パスの場合のみCookieを発行する。不正な値はempty(呼び出し元はCookieを発行しない=既定の遷移先に倒す) */
  public Optional<ResponseCookie> issue(String redirectPath) {
    if (!isSafeRelativePath(redirectPath)) {
      return Optional.empty();
    }
    return Optional.of(baseCookie(redirectPath).maxAge(EXPIRY).build());
  }

  public ResponseCookie expired() {
    return baseCookie("").maxAge(0).build();
  }

  /** オープンリダイレクト対策: 「`/` で始まり `//` で始まらない相対パス」のみ許可する。 絶対URL・スキーム付き・プロトコル相対URLはすべて拒否する。 */
  public static boolean isSafeRelativePath(String path) {
    return path != null && path.startsWith("/") && !path.startsWith("//");
  }

  private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
    return ResponseCookie.from(COOKIE_NAME, value)
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite("Lax")
        .path("/");
  }
}
