package com.swiss_stage.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiss_stage.presentation.auth.JwtSessionSupport;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * APIコントラクトテストの共通基盤。
 * リポジトリはインメモリ実装に差し替え、統一レスポンス・ステータスコード・認可を検証する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(InMemoryRepositoryConfig.class)
public abstract class ApiContractTestSupport {

    protected static final String OWNER_SUB = "owner-sub";
    protected static final String OTHER_SUB = "other-sub";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtSessionSupport jwtSessionSupport;

    /** 認証済みCookie(JWTを直接発行。test-loginエンドポイント自体のテストはAuthApiTest) */
    protected Cookie sessionCookie(String sub) {
        return new Cookie(JwtSessionSupport.COOKIE_NAME, jwtSessionSupport.issue(sub, "テスト運営者"));
    }

    protected Cookie ownerCookie() {
        return sessionCookie(OWNER_SUB);
    }

    protected JsonNode dataOf(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data");
    }
}
