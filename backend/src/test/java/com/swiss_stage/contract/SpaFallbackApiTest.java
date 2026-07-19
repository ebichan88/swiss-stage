package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SPA配信とフォールバックの契約(04_react_router_patterns.md §5、14_performance_optimization.md §4)。
 * テスト用の index.html / assets は src/test/resources/static に置いてある。
 */
class SpaFallbackApiTest extends ApiContractTestSupport {

    @Test
    @DisplayName("SPA-AC-001: SPAルートへの直アクセスはindex.htmlを返す(no-cache)")
    void SPAフォールバック() throws Exception {
        for (String path : new String[] {
                "/tournaments", "/tournaments/01ARZ3NDEKTSV4RRFFQ69G5FAV/rounds",
                "/s/some-share-token", "/login"}) {
            MvcResult result = mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-cache"))
                    .andReturn();
            assertThat(result.getResponse().getContentAsString())
                    .contains("spa-fallback-test-marker");
        }
    }

    @Test
    @DisplayName("SPA-AC-002: index.html自体とルート(/)もSPAを返す")
    void ルートアクセス() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "spa-fallback-test-marker")));
        // "/" はview controllerで /index.html へフォワードされる
        mockMvc.perform(get("/")).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @DisplayName("SPA-AC-003: ハッシュ付きアセットは1年+immutableでキャッシュされる")
    void アセットの長期キャッシュ() throws Exception {
        mockMvc.perform(get("/assets/app-test.js"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    @DisplayName("SPA-AC-004: 未知のAPIパスはindex.htmlにフォールバックせず404になる")
    void APIは対象外() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/unknown-endpoint"))
                .andExpect(status().isNotFound())
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("spa-fallback-test-marker");
    }
}
