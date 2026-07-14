package com.swiss_stage.presentation.auth;

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
 * Google OAuth2認証成功時に自前のJWTセッションCookieを発行してSPAへ戻す(13_security_design.md §2)。
 * 以降の認証はJWT Cookieのみで行うため、OAuth2フロー用のHTTPセッションはここで破棄する。
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtSessionSupport jwtSessionSupport;
    private final String frontendBaseUrl;

    public OAuth2LoginSuccessHandler(
            JwtSessionSupport jwtSessionSupport,
            @Value("${app.auth.frontend-base-url:}") String frontendBaseUrl) {
        this.jwtSessionSupport = jwtSessionSupport;
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
        response.sendRedirect(frontendBaseUrl + "/tournaments");
    }
}
