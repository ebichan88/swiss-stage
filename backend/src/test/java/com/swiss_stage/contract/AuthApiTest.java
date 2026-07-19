package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swiss_stage.presentation.auth.JwtSessionSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthApiTest extends ApiContractTestSupport {

    @Test
    @DisplayName("AUTH-AC-001: 未認証の /auth/me は401を統一エラーフォーマットで返し、X-Request-Idが付く")
    void 未認証() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    @DisplayName("AUTH-AC-002: test-loginでHttpOnlyのセッションCookieが発行され、/auth/me が通る")
    void テストログイン() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/test-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"user-1\",\"name\":\"運営 太郎\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(JwtSessionSupport.COOKIE_NAME, true))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sub").value("user-1"))
                .andExpect(jsonPath("$.meta.timestamp").isNotEmpty())
                .andReturn();
        Cookie session = login.getResponse().getCookie(JwtSessionSupport.COOKIE_NAME);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sub").value("user-1"))
                .andExpect(jsonPath("$.data.name").value("運営 太郎"));
    }

    @Test
    @DisplayName("AUTH-AC-003: /auth/login はGoogleのOAuth2認可フローへ2段リダイレクトする")
    void Googleログインへのリダイレクト() throws Exception {
        MvcResult entry = mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isFound())
                .andReturn();
        String authorizationPath = entry.getResponse().getRedirectedUrl();
        assertThat(authorizationPath).isEqualTo("/api/v1/auth/login/google");

        MvcResult authorize = mockMvc.perform(get(authorizationPath))
                .andExpect(status().isFound())
                .andReturn();
        String googleUrl = authorize.getResponse().getRedirectedUrl();
        assertThat(googleUrl)
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
                .contains("redirect_uri=")
                .contains("/api/v1/auth/callback");
    }

    @Test
    @DisplayName("AUTH-AC-004,AUTH-AC-005: logoutでCookieが失効し、不正なCookieは未認証扱いになる")
    void ログアウトと不正Cookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(JwtSessionSupport.COOKIE_NAME, 0));

        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(new Cookie(JwtSessionSupport.COOKIE_NAME, "broken-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
