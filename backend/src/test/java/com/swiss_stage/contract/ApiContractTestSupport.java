package com.swiss_stage.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

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

    /**
     * API契約のSSoT(schema/openapi.yaml)に対するリクエスト/レスポンス検証器。
     * 認証Cookieの有無は401のテストで意図的に外すため、securityの検証のみ無効化する。
     */
    private static final OpenApiInteractionValidator OPENAPI_VALIDATOR =
            OpenApiInteractionValidator.createForSpecificationUrl("/openapi.yaml")
                    .withLevelResolver(LevelResolver.create()
                            .withLevel("validation.request.security.missing",
                                    ValidationReport.Level.IGNORE)
                            .build())
                    // MockMvcアダプタはmultipartボディを検証器に渡せないため、
                    // CSVインポートに限りリクエストボディ欠落を無視する(レスポンスは検証される)
                    .withWhitelist(ValidationErrorsWhitelist.create()
                            .withRule("multipart request body (MockMvc limitation)",
                                    WhitelistRules.allOf(
                                            WhitelistRules.messageHasKey(
                                                    "validation.request.body.missing"),
                                            WhitelistRules.pathContainsSubstring(
                                                    "/participants/import"))))
                    .build();

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

    /**
     * mockMvc.perform + OpenAPIスキーマ検証(schema/openapi.yaml)。
     * /api/v1 のJSON API呼び出しは原則こちらを使う。次の場合のみ素の mockMvc.perform を使う:
     * <ul>
     *   <li>意図的にスキーマ違反のリクエストを送るテスト(バリデーションエラー系)</li>
     *   <li>SPA配信(HTML)・OAuth2リダイレクトなどスキーマの対象外</li>
     * </ul>
     */
    protected ResultActions performApi(RequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder)
                .andExpect(OpenApiValidationMatchers.openApi().isValid(OPENAPI_VALIDATOR));
    }
}
