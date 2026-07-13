package com.swiss_stage.presentation.filter;

import com.swiss_stage.presentation.auth.CurrentUser;
import com.swiss_stage.presentation.auth.JwtSessionSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * セッションCookieを検証し、有効なら CurrentUser をrequest attributeに載せる。
 * 認証の強制はしない(必要なエンドポイントが CurrentUser 引数で要求する)。
 */
@Component
public class AuthCookieFilter extends OncePerRequestFilter {

    private final JwtSessionSupport jwtSessionSupport;

    public AuthCookieFilter(JwtSessionSupport jwtSessionSupport) {
        this.jwtSessionSupport = jwtSessionSupport;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies)
                    .filter(c -> JwtSessionSupport.COOKIE_NAME.equals(c.getName()))
                    .findFirst()
                    .flatMap(c -> jwtSessionSupport.verify(c.getValue()))
                    .ifPresent(user -> request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, user));
        }
        filterChain.doFilter(request, response);
    }
}
