package com.swiss_stage.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * /api/v1/invitations/** のIPベースレート制限(13_security_design.md §5・14_tournament_collaboration.md §4.7)。
 * 招待トークンの総当たりはMAINTAINER(書き込み権限)の奪取に直結するためSHR-AC-009より優先度を上げている。 上限を小さく上書きした専用コンテキストで検証する。
 */
@TestPropertySource(
    properties = {
      "app.rate-limit.invitation.capacity=3",
      "app.rate-limit.invitation.refill-per-minute=1"
    })
class InvitationRateLimitApiTest extends ApiContractTestSupport {

  @Test
  @DisplayName("MBR-AC-016: 招待のプレビュー・承諾APIはIPベースのレート制限超過で429になる")
  void レート制限() throws Exception {
    String path = "/api/v1/invitations/" + "Z".repeat(43);
    for (int i = 0; i < 3; i++) {
      performApi(get(path).cookie(ownerCookie())).andExpect(status().isForbidden());
    }
    performApi(get(path).cookie(ownerCookie()))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.error.message").isNotEmpty());
  }
}
