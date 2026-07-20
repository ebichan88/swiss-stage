package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 共有トークン系API(Phase 5)の契約テスト:
 * 発行/再発行・/shared/{token} 集約・トークン経由の結果入力・情報漏えい防止。
 */
class SharedApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"共有テスト大会\",\"gameType\":\"GO\",\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
        performApi(post(base() + "/participants")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"参加 一郎\",\"rank\":\"DAN_3\"}"))
                .andExpect(status().isCreated());
        for (String name : new String[] {"参加 二郎", "参加 三郎", "参加 四郎"}) {
            performApi(post(base() + "/participants")
                            .cookie(ownerCookie())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("SHR-AC-001,SHR-AC-002: トークンは運営者のみ発行・再発行でき、再発行で旧トークンは即時無効になる")
    void トークン発行と再発行() throws Exception {
        // 他人の大会は404(存在を漏らさない)
        performApi(post(base() + "/share-token/regenerate").cookie(sessionCookie(OTHER_SUB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TOURNAMENT_NOT_FOUND"));
        // 未認証は401
        performApi(post(base() + "/share-token/regenerate"))
                .andExpect(status().isUnauthorized());

        String token1 = regenerateToken();
        assertThat(token1).hasSizeGreaterThanOrEqualTo(32).matches("[A-Za-z0-9_-]+");
        setVisibility("TOKEN");
        performApi(get("/api/v1/shared/" + token1)).andExpect(status().isOk());

        // 再発行 → 別トークンになり、旧トークンは403
        String token2 = regenerateToken();
        assertThat(token2).isNotEqualTo(token1);
        performApi(get("/api/v1/shared/" + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_SHARE_TOKEN"));
        performApi(get("/api/v1/shared/" + token2)).andExpect(status().isOk());
    }

    @Test
    @DisplayName(
            "SHR-AC-003,SHR-AC-004,SHR-AC-010: 共有ページ集約は大会・ラウンド・順位(rank・entryOrder含む)を返し、"
                    + "shareToken/ownerSubを含めない")
    void 共有ページ集約() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
        performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated());

        MvcResult shared = performApi(get("/api/v1/shared/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tournament.name").value("共有テスト大会"))
                .andExpect(jsonPath("$.data.tournament.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.tournament.currentRound").value(1))
                .andExpect(jsonPath("$.data.tournament.resultInputEnabled").value(false))
                .andExpect(jsonPath("$.data.rounds.length()").value(1))
                .andExpect(jsonPath("$.data.rounds[0].matches.length()").value(2))
                .andExpect(jsonPath("$.data.standings.length()").value(1))
                .andExpect(jsonPath("$.data.standings[0].group.name").value("A"))
                .andExpect(jsonPath("$.data.standings[0].standings.length()").value(4))
                // SHR-AC-010: 未対局時はエントリー順が最終タイブレークになるため、
                // 先頭は最初に登録した「参加 一郎」(rank=DAN_3, entryOrder=1)
                .andExpect(jsonPath("$.data.standings[0].standings[0].participant.name")
                        .value("参加 一郎"))
                .andExpect(jsonPath("$.data.standings[0].standings[0].participant.rank")
                        .value("DAN_3"))
                .andExpect(jsonPath("$.data.standings[0].standings[0].participant.entryOrder")
                        .value(1))
                .andReturn();
        String body = shared.getResponse().getContentAsString();
        assertThat(body).doesNotContain("shareToken", "ownerSub", OWNER_SUB, token);
    }

    @Test
    @DisplayName("SHR-AC-005: 無効・不明・形式不正トークンと非公開(PRIVATE)は同じ403で大会の存在を漏らさない")
    void 無効トークンと非公開() throws Exception {
        // 形式は正しいが存在しないトークン
        performApi(get("/api/v1/shared/" + "A".repeat(43)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_SHARE_TOKEN"));
        // 形式不正(短すぎ)
        performApi(get("/api/v1/shared/short"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_SHARE_TOKEN"));

        // トークンは有効でも公開範囲がPRIVATEなら403
        String token = regenerateToken();
        performApi(get("/api/v1/shared/" + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_SHARE_TOKEN"));
    }

    @Test
    @DisplayName("SHR-AC-006,SHR-AC-007: トークン経由の結果入力は大会設定で許可時のみ可能で、確定済み・競合は409になる")
    void トークン経由の結果入力() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
        MvcResult round = performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode match = dataOf(round).path("round").path("matches").get(0);
        String matchId = match.path("id").asText();
        long version = match.path("version").asLong();

        // 許可前(resultInputEnabled=false)は403
        performApi(putSharedResult(token, matchId, "PLAYER1_WIN", version))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        // 運営者が許可 → 入力できる
        setResultInputEnabled(true);
        performApi(putSharedResult(token, matchId, "PLAYER1_WIN", version))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.version").value(version + 1));

        // 古いversionは409 CONFLICT
        performApi(putSharedResult(token, matchId, "PLAYER2_WIN", version))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        // 残りを運営者が入力して確定 → 確定後のトークン入力は409
        for (JsonNode m : dataOf(round).path("round").path("matches")) {
            if (!m.path("id").asText().equals(matchId)) {
                performApi(put(base() + "/matches/" + m.path("id").asText() + "/result")
                                .cookie(ownerCookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"result\":\"PLAYER1_WIN\",\"version\":"
                                        + m.path("version").asLong() + "}"))
                        .andExpect(status().isOk());
            }
        }
        performApi(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk());
        performApi(putSharedResult(token, matchId, "PLAYER2_WIN", version + 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    @Test
    @DisplayName("SHR-AC-008: キャッシュ済みの共有ページも結果入力・確定後は即時反映される(evict)")
    void キャッシュevict() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
        MvcResult round = performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();

        // 共有ページを一度取得してキャッシュを温める
        performApi(get("/api/v1/shared/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rounds[0].matches[0].result").value("NONE"));

        // 運営者が結果入力 → キャッシュ済みでも新しい結果が返る
        JsonNode match = dataOf(round).path("round").path("matches").get(0);
        performApi(put(base() + "/matches/" + match.path("id").asText() + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PLAYER1_WIN\",\"version\":"
                                + match.path("version").asLong() + "}"))
                .andExpect(status().isOk());
        performApi(get("/api/v1/shared/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rounds[0].matches[0].result").value("PLAYER1_WIN"));

        // 参加者名の変更も反映される(順位表の表示名)
        MvcResult participants = performApi(get(base() + "/participants")
                        .cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andReturn();
        String participantId = dataOf(participants).get(0).path("id").asText();
        performApi(patch(base() + "/participants/" + participantId)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"改名 太郎\"}"))
                .andExpect(status().isOk());
        MvcResult shared = performApi(get("/api/v1/shared/" + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(shared.getResponse().getContentAsString()).contains("改名 太郎");
    }

    private MockHttpServletRequestBuilder putSharedResult(
            String token, String matchId, String result, long version) {
        return put("/api/v1/shared/" + token + "/matches/" + matchId + "/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"result\":\"" + result + "\",\"version\":" + version + "}");
    }

    private String regenerateToken() throws Exception {
        MvcResult result = performApi(post(base() + "/share-token/regenerate")
                        .cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareToken").isNotEmpty())
                .andReturn();
        return dataOf(result).path("shareToken").asText();
    }

    private void setVisibility(String visibility) throws Exception {
        performApi(patch(base())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"" + visibility + "\",\"version\":"
                                + currentVersion() + "}"))
                .andExpect(status().isOk());
    }

    private void setResultInputEnabled(boolean enabled) throws Exception {
        performApi(patch(base())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultInputEnabled\":" + enabled + ",\"version\":"
                                + currentVersion() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultInputEnabled").value(enabled));
    }

    private long currentVersion() throws Exception {
        MvcResult result = performApi(get(base()).cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andReturn();
        return dataOf(result).path("version").asLong();
    }

    private String base() {
        return "/api/v1/tournaments/" + tournamentId;
    }
}
