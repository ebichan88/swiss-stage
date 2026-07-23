package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * ラウンド生成 → 結果入力 → 確定 → 次ラウンド → 順位表 の一巡(Phase 3の受け入れシナリオ)。
 */
class RoundApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ラウンドテスト大会\",\"gameType\":\"GO\",\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
        String[][] entries = {
                {"参加 一郎", "DAN_3"}, {"参加 二郎", "DAN_1"},
                {"参加 三郎", "KYU_1"}, {"参加 四郎", "KYU_5"}};
        for (String[] entry : entries) {
            performApi(post(base() + "/participants")
                            .cookie(ownerCookie())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + entry[0] + "\",\"rank\":\"" + entry[1] + "\"}"))
                    .andExpect(status().isCreated());
        }
        performApi(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RND-AC-001,RND-AC-002,RND-AC-003,RND-AC-004,RND-AC-005,RND-AC-006,RND-AC-007: "
            + "生成→結果入力→確定→次ラウンド→順位表の一巡が通り、再戦が発生しない")
    void 一巡シナリオ() throws Exception {
        // ラウンド1生成(棋力順の隣接ペア・4名なのでBYEなし)
        MvcResult r1 = performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.round.roundNumber").value(1))
                .andExpect(jsonPath("$.data.round.status").value("PLAYING"))
                .andExpect(jsonPath("$.data.round.matches.length()").value(2))
                .andExpect(jsonPath("$.data.relaxations.length()").value(0))
                .andReturn();

        // 確定前の再生成は409(現在ラウンド未確定)
        performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        // 未入力のまま確定は409
        performApi(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        // 全対局の結果入力(player1勝ち)
        Set<String> round1Pairs = new HashSet<>();
        for (JsonNode match : dataOf(r1).path("round").path("matches")) {
            round1Pairs.add(pairKey(match));
            inputResult(match, "PLAYER1_WIN");
        }

        // 確定 → ラウンド2生成
        performApi(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        MvcResult r2 = performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.round.roundNumber").value(2))
                .andExpect(jsonPath("$.data.relaxations.length()").value(0))
                .andReturn();

        // 再戦禁止: ラウンド2にラウンド1と同じペアがない
        for (JsonNode match : dataOf(r2).path("round").path("matches")) {
            assertThat(round1Pairs).doesNotContain(pairKey(match));
        }

        // ラウンド一覧に2ラウンド分が対局付きで返る
        performApi(get(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].roundNumber").value(1))
                .andExpect(jsonPath("$.data[0].matches.length()").value(2));

        // 順位表: 単一グループ大会はデフォルトグループ「A」の単一要素。R1全勝の2名が上位(勝点1.0)
        performApi(get(base() + "/standings").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].group.name").value("A"))
                .andExpect(jsonPath("$.data[0].standings.length()").value(4))
                .andExpect(jsonPath("$.data[0].standings[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].standings[0].wins").value(1.0))
                .andExpect(jsonPath("$.data[0].standings[0].participant.name").isNotEmpty());
    }

    @Test
    @DisplayName("RND-AC-008,RND-AC-009,RND-AC-010,RND-AC-011: 結果入力はversion不一致・確定済みラウンドで409、不正値は400になる")
    void 結果入力の競合と確定後変更() throws Exception {
        MvcResult r1 = performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode firstMatch = dataOf(r1).path("round").path("matches").get(0);
        String matchId = firstMatch.path("id").asText();
        long version = firstMatch.path("version").asLong();

        // 不正な結果値(BYE)は400
        performApi(put(base() + "/matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"BYE\",\"version\":" + version + "}"))
                .andExpect(status().isBadRequest());

        // 正常入力 → 200(versionが進む)
        performApi(put(base() + "/matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PLAYER2_WIN\",\"version\":" + version + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PLAYER2_WIN"))
                .andExpect(jsonPath("$.data.version").value(version + 1));

        // 古いversionでの再入力は409 CONFLICT
        performApi(put(base() + "/matches/" + matchId + "/result")
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
        performApi(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk());
        performApi(put(base() + "/matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"DRAW\",\"version\":" + (version + 1) + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    @Test
    @DisplayName("RND-AC-012: 片方のみ申告・申告不一致の対局が残っていてもラウンド確定はブロックしない")
    void 申告未確定でもラウンド確定できる() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        setResultInputEnabled(true);

        MvcResult r1 = performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode matches = dataOf(r1).path("round").path("matches");
        JsonNode waitingMatch = matches.get(0);
        JsonNode conflictingMatch = matches.get(1);

        // 片方のみ申告(待ち状態)。resultはNONEのまま
        reportSharedResult(token, waitingMatch.path("id").asText(), "PLAYER1",
                "PLAYER1_WIN", waitingMatch.path("version").asLong())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("NONE"))
                .andExpect(jsonPath("$.data.player1ReportedResult").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.player2ReportedResult").value("NONE"));

        // 両者が異なる結果を申告(不一致)。resultはNONEのまま
        reportSharedResult(token, conflictingMatch.path("id").asText(), "PLAYER1",
                "PLAYER1_WIN", conflictingMatch.path("version").asLong())
                .andExpect(status().isOk());
        reportSharedResult(token, conflictingMatch.path("id").asText(), "PLAYER2",
                "PLAYER2_WIN", conflictingMatch.path("version").asLong() + 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("NONE"));

        // どちらの対局も未確定だが、ラウンド確定はブロックされない(運営者の裁量)
        performApi(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    private ResultActions reportSharedResult(
            String token, String matchId, String reportedBy, String result, long version)
            throws Exception {
        return performApi(put("/api/v1/shared/" + token + "/matches/" + matchId + "/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedBy\":\"" + reportedBy + "\",\"result\":\"" + result
                        + "\",\"version\":" + version + "}"));
    }

    private String regenerateToken() throws Exception {
        MvcResult result = performApi(post(base() + "/share-token/regenerate")
                        .cookie(ownerCookie()))
                .andExpect(status().isOk())
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
                .andExpect(status().isOk());
    }

    private long currentVersion() throws Exception {
        MvcResult result = performApi(get(base()).cookie(ownerCookie())).andReturn();
        return dataOf(result).path("version").asLong();
    }

    private void inputResult(JsonNode match, String result) throws Exception {
        performApi(put(base() + "/matches/" + match.path("id").asText() + "/result")
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
