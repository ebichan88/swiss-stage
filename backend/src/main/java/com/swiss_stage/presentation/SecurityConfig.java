package com.swiss_stage.presentation;

import com.swiss_stage.presentation.auth.OAuth2LoginFailureHandler;
import com.swiss_stage.presentation.auth.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security設定(13_security_design.md §2, §5)。
 *
 * <p>Spring SecurityはGoogle OAuth2フロー(認可リダイレクト・コールバック)と
 * セキュリティヘッダーのためだけに使う。APIの認証・認可は既存の
 * JWT Cookie({@link com.swiss_stage.presentation.filter.AuthCookieFilter})+
 * application層の所有者検証で行うため、全リクエストをpermitAllにする。
 *
 * <p>CSRF: 認証CookieはSameSite=Lax + 更新系はJSON APIのため無効化(設計書 §5)。
 */
@Configuration
public class SecurityConfig {

    /** 認可エンドポイント。実URLは {baseUri}/{registrationId} = /api/v1/auth/login/google */
    public static final String AUTHORIZATION_BASE_URI = "/api/v1/auth/login";
    public static final String CALLBACK_URI = "/api/v1/auth/callback";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler successHandler,
            OAuth2LoginFailureHandler failureHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.disable())
                .headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        // SPAのログイン画面。デフォルトのログインページ生成を無効化する
                        .loginPage("/login")
                        .authorizationEndpoint(endpoint -> endpoint.baseUri(AUTHORIZATION_BASE_URI))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri(CALLBACK_URI))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));
        return http.build();
    }
}
