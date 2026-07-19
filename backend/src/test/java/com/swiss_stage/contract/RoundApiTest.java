package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * ラウンド生成 → 結果入力 → 確定 → 次ラウンド → 順位表 の一巡(Phase 3の受け入れシナリオ)。
 */
class RoundApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ラウンドテスト大会\",\"gameType\":\"GO\",\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
        String[][] entries = {
                {"参加 一郎", "DAN_3"}, {"参加 二郎", "DAN_1"},
                {"参加 三郎", "KYU_1"}, {"参加 四郎", "KYU_5"}};
        for (String[] entry : entries) {
            mockMvc.perform(post(base() + "/participants")
                            .cookie(ownerCookie())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + entry[0] + "\",\"rank\":\"" + entry[1] + "\"}"))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("生成→結果入力→確定→次ラウンド→順位表の一巡が通り、再戦が発生しない")
    void 一巡シナリオ() throws Exception {
        // ラウンド1生成(棋力順の隣接ペア・4名なのでBYEなし)
        MvcResult r1 = mockMvc.perform(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.round.roundNumber").value(1))
                .andExpect(jsonPath("$.data.round.status").value("PLAYING"))
                .andExpect(jsonPath("$.data.round.matches.length()").value(2))
                .andExpect(jsonPath("$.data.relaxations.length()").value(0))
                .andReturn();

        // 確定前の再生成は409(現在ラウンド未確定)
        mockMvc.perform(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        // 未入力のまま確定は409
        mockMvc.perform(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        // 全対局の結果入力(player1勝ち)
        Set<String> round1Pairs = new HashSet<>();
        for (JsonNode match : dataOf(r1).path("round").path("matches")) {
            round1Pairs.add(pairKey(match));
            inputResult(match, "PLAYER1_WIN");
        }

        // 確定 → ラウンド2生成
        mockMvc.perform(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        MvcResult r2 = mockMvc.perform(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.round.roundNumber").value(2))
                .andExpect(jsonPath("$.data.relaxations.length()").value(0))
                .andReturn();

        // 再戦禁止: ラウンド2にラウンド1と同じペアがない
        for (JsonNode match : dataOf(r2).path("round").path("matches")) {
            assertThat(round1Pairs).doesNotContain(pairKey(match));
        }

        // ラウンド一覧に2ラウンド分が対局付きで返る
        mockMvc.perform(get(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].roundNumber").value(1))
                .andExpect(jsonPath("$.data[0].matches.length()").value(2));

        // 順位表: 単一グループ大会はデフォルトグループ「A」の単一要素。R1全勝の2名が上位(勝点1.0)
        mockMvc.perform(get(base() + "/standings").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].group.name").value("A"))
                .andExpect(jsonPath("$.data[0].standings.length()").value(4))
                .andExpect(jsonPath("$.data[0].standings[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].standings[0].wins").value(1.0))
                .andExpect(jsonPath("$.data[0].standings[0].participant.name").isNotEmpty());
    }

    @Test
    @DisplayName("結果入力はversion不一致・確定済みラウンドで409になる")
    void 結果入力の競合と確定後変更() throws Exception {
        MvcResult r1 = mockMvc.perform(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode firstMatch = dataOf(r1).path("round").path("matches").get(0);
        String matchId = firstMatch.path("id").asText();
        long version = firstMatch.path("version").asLong();

        // 不正な結果値(BYE)は400
        mockMvc.perform(put(base() + "/matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"BYE\",\"version\":" + version + "}"))
                .andExpect(status().isBadRequest());

        // 正常入力 → 200(versionが進む)
        mockMvc.perform(put(base() + "/matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PLAYER2_WIN\",\"version\":" + version + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PLAYER2_WIN"))
                .andExpect(jsonPath("$.data.version").value(version + 1));

        // 古いversionでの再入力は409 CONFLICT
        mockMvc.perform(put(base() + "/matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PLAYER1_WIN\",\"version\":" + version + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        // 全対局入力→確定後は結果変更不可
        for (JsonNode match : dataOf(r1).path("round").path("matches")) {
            if (!match.path("id").asText().equals(matchId)) {
                inputResult(match, "PLAYER1_WIN");
            }
        }
        mockMvc.perform(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk());
        mockMvc.perform(put(base() + "/matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"DRAW\",\"version\":" + (version + 1) + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    private void inputResult(JsonNode match, String result) throws Exception {
        mockMvc.perform(put(base() + "/matches/" + match.path("id").asText() + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"" + result + "\",\"version\":"
                                + match.path("version").asLong() + "}"))
                .andExpect(status().isOk());
    }

    private static String pairKey(JsonNode match) {
        String p1 = match.path("player1").path("id").asText();
        String p2 = match.path("player2").path("id").asText("");
        return p1.compareTo(p2) < 0 ? p1 + ":" + p2 : p2 + ":" + p1;
    }

    private String base() {
        return "/api/v1/tournaments/" + tournamentId;
    }
}
