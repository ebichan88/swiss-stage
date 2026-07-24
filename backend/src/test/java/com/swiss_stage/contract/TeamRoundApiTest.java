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
 * 団体戦のラウンド生成 → ボード結果入力 → 確定 → 次ラウンド → 順位表(05_swiss_pairing_algorithm.md §5)。
 */
class TeamRoundApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"団体戦ラウンドテスト\",\"gameType\":\"GO\","
                                + "\"competitionType\":\"TEAM\",\"teamSize\":3,\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
        for (String name : new String[] {"Aチーム", "Bチーム", "Cチーム", "Dチーム"}) {
            createTeamWithFullRoster(name);
        }
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TEAM-AC-012,TEAM-AC-013,TEAM-AC-016,TEAM-AC-017: "
            + "エントリー順のみで初回ペアリングされ、再戦禁止が効き、順位表がチーム単位で計算され、"
            + "個人名を含まない")
    void 一巡シナリオ() throws Exception {
        MvcResult r1 = performApi(post(base() + "/team-rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.round.roundNumber").value(1))
                .andExpect(jsonPath("$.data.round.matches.length()").value(2))
                .andExpect(jsonPath("$.data.relaxations.length()").value(0))
                .andReturn();

        Set<String> round1Pairs = new HashSet<>();
        for (JsonNode match : dataOf(r1).path("round").path("matches")) {
            round1Pairs.add(pairKey(match));
            inputAllBoardsWin(match, "PLAYER1_WIN");
        }

        performApi(post(base() + "/team-rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        MvcResult r2 = performApi(post(base() + "/team-rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        for (JsonNode match : dataOf(r2).path("round").path("matches")) {
            assertThat(round1Pairs).doesNotContain(pairKey(match));
            // 個人名を含まない(チーム名のみ)
            assertThat(match.path("team1").has("name")).isTrue();
            assertThat(match.has("player1")).isFalse();
        }

        performApi(get(base() + "/team-standings").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].standings.length()").value(4))
                .andExpect(jsonPath("$.data[0].standings[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].standings[0].wins").value(1.0))
                .andExpect(jsonPath("$.data[0].standings[0].team.name").isNotEmpty());
    }

    @Test
    @DisplayName("TEAM-AC-014,TEAM-AC-015: 運営者が全ボード結果をまとめて直接確定でき、"
            + "未着手の対局が残るラウンドは確定できない")
    void ボード結果の直接確定と未着手ブロック() throws Exception {
        MvcResult r1 = performApi(post(base() + "/team-rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();

        performApi(post(base() + "/team-rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        JsonNode matches = dataOf(r1).path("round").path("matches");
        JsonNode first = matches.get(0);
        String matchId = first.path("id").asText();
        long version = first.path("version").asLong();

        // 2勝1敗の内訳で入力(チーム全体は勝ち)
        performApi(put(base() + "/team-matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardResults\":[\"PLAYER1_WIN\",\"PLAYER1_WIN\",\"PLAYER2_WIN\"],"
                                + "\"version\":" + version + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.boardResults[2].result").value("PLAYER2_WIN"))
                .andExpect(jsonPath("$.data.version").value(version + 1));

        // 古いversionでの再入力は409
        performApi(put(base() + "/team-matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardResults\":[\"PLAYER2_WIN\",\"PLAYER2_WIN\",\"PLAYER2_WIN\"],"
                                + "\"version\":" + version + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        for (JsonNode match : matches) {
            if (!match.path("id").asText().equals(matchId)) {
                inputAllBoardsWin(match, "PLAYER1_WIN");
            }
        }
        performApi(post(base() + "/team-rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk());
    }

    private void createTeamWithFullRoster(String name) throws Exception {
        MvcResult result = performApi(post(base() + "/teams")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String teamId = dataOf(result).path("id").asText();
        for (int position = 1; position <= 3; position++) {
            performApi(post(base() + "/teams/" + teamId + "/members")
                            .cookie(ownerCookie())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"メンバー" + position + "\",\"boardPosition\":"
                                    + position + "}"))
                    .andExpect(status().isCreated());
        }
    }

    private void inputAllBoardsWin(JsonNode match, String result) throws Exception {
        performApi(put(base() + "/team-matches/" + match.path("id").asText() + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardResults\":[\"" + result + "\",\"" + result + "\",\""
                                + result + "\"],\"version\":" + match.path("version").asLong() + "}"))
                .andExpect(status().isOk());
    }

    private static String pairKey(JsonNode match) {
        String t1 = match.path("team1").path("id").asText();
        String t2 = match.path("team2").path("id").asText("");
        return t1.compareTo(t2) < 0 ? t1 + ":" + t2 : t2 + ":" + t1;
    }

    private String base() {
        return "/api/v1/tournaments/" + tournamentId;
    }
}
