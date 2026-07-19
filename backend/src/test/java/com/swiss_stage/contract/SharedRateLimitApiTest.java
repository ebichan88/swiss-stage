package com.swiss_stage.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * /api/v1/shared/** のIPベースレート制限(13_security_design.md §5)。
 * 上限を小さく上書きした専用コンテキストで検証する。
 */
@TestPropertySource(properties = {
        "app.rate-limit.shared.capacity=3",
        "app.rate-limit.shared.refill-per-minute=1"})
class SharedRateLimitApiTest extends ApiContractTestSupport {

    @Test
    @DisplayName("SHR-AC-009: 上限を超えた共有トークンアクセスは429を統一フォーマットで返す")
    void レート制限() throws Exception {
        String path = "/api/v1/shared/" + "B".repeat(43);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get(path)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(path))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }
}
