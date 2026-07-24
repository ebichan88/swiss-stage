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
 * 団体戦の共有トークン系API契約テスト:共有ページ集約(teamRounds/teamStandings)・
 * ボード単位の自己申告(TEAM-AC-018〜020、13_security_design.md §結果確定の運用ルール)。
 */
class TeamSharedApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"団体戦共有テスト\",\"gameType\":\"GO\","
                                + "\"competitionType\":\"TEAM\",\"teamSize\":3,\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
        for (String name : new String[] {"Aチーム", "Bチーム"}) {
            MvcResult team = performApi(post(base() + "/teams")
                            .cookie(ownerCookie())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isCreated())
                    .andReturn();
            String teamId = dataOf(team).path("id").asText();
            for (int position = 1; position <= 3; position++) {
                performApi(post(base() + "/teams/" + teamId + "/members")
                                .cookie(ownerCookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"メンバー" + position + "\",\"boardPosition\":"
                                        + position + "}"))
                        .andExpect(status().isCreated());
            }
        }
    }

    @Test
    @DisplayName("共有ページ集約は団体戦ではteamRounds/teamStandingsを返し、rounds/standingsはnullになる。"
            + "個人名を含まない")
    void 共有ページ集約はチーム形式() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
        performApi(post(base() + "/team-rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated());

        MvcResult shared = performApi(get("/api/v1/shared/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tournament.competitionType").value("TEAM"))
                .andExpect(jsonPath("$.data.rounds").doesNotExist())
                .andExpect(jsonPath("$.data.standings").doesNotExist())
                .andExpect(jsonPath("$.data.teamRounds.length()").value(1))
                .andExpect(jsonPath("$.data.teamRounds[0].matches.length()").value(1))
                .andExpect(jsonPath("$.data.teamStandings.length()").value(1))
                .andExpect(jsonPath("$.data.teamStandings[0].standings.length()").value(2))
                .andReturn();
        String body = shared.getResponse().getContentAsString();
        assertThat(body).doesNotContain("メンバー1", "メンバー2", "メンバー3");
    }

    @Test
    @DisplayName("TEAM-AC-018: 片方のみの申告では確定せず、ボードごとに両者の申告が一致した時点でそのボードのみ確定する")
    void ボード単位の自己申告() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        setResultInputEnabled(true);
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
        MvcResult round = performApi(post(base() + "/team-rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode match = dataOf(round).path("round").path("matches").get(0);
        String matchId = match.path("id").asText();
        long version = match.path("version").asLong();

        // team1側が3ボード分申告(片方のみなのですべて未確定のまま)
        performApi(reportTeamResult(token, matchId, "PLAYER1",
                        "[\"PLAYER1_WIN\",\"PLAYER1_WIN\",\"PLAYER2_WIN\"]", version))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("NONE"))
                .andExpect(jsonPath("$.data.boardResults[0].team1ReportedResult").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.boardResults[2].team1ReportedResult").value("PLAYER2_WIN"));

        // team2側が主将戦(0)・副将戦(1)は一致、三将戦(2)は不一致の申告 → 一致した2ボードのみ確定
        performApi(reportTeamResult(token, matchId, "PLAYER2",
                        "[\"PLAYER1_WIN\",\"PLAYER1_WIN\",\"PLAYER1_WIN\"]", version + 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.boardResults[1].result").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.boardResults[2].result").value("NONE"))
                .andExpect(jsonPath("$.data.boardResults[2].team1ReportedResult").value("PLAYER2_WIN"))
                .andExpect(jsonPath("$.data.boardResults[2].team2ReportedResult").value("PLAYER1_WIN"));
    }

    @Test
    @DisplayName("TEAM-AC-020: 運営者が直接確定したボード結果は、その後の参加者の自己申告で上書きされない")
    void 運営者確定は自己申告で上書きされない() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        setResultInputEnabled(true);
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
        MvcResult round = performApi(post(base() + "/team-rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode match = dataOf(round).path("round").path("matches").get(0);
        String matchId = match.path("id").asText();
        long version = match.path("version").asLong();

        // 運営者が主将戦をDRAWで直接確定
        performApi(put(base() + "/team-matches/" + matchId + "/result")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardResults\":[\"DRAW\",\"NONE\",\"NONE\"],\"version\":"
                                + version + "}"))
                .andExpect(status().isOk());

        // 両者がPLAYER1_WINで一致申告しても、運営者確定(DRAW)のままで上書きされない
        // (自己申告はボード配列すべてに実際の結果を含める必要があり、NONEは指定できない)
        performApi(reportTeamResult(token, matchId, "PLAYER1",
                        "[\"PLAYER1_WIN\",\"PLAYER1_WIN\",\"PLAYER1_WIN\"]", version + 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("DRAW"));
        performApi(reportTeamResult(token, matchId, "PLAYER2",
                        "[\"PLAYER1_WIN\",\"PLAYER1_WIN\",\"PLAYER1_WIN\"]", version + 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("DRAW"))
                .andExpect(jsonPath("$.data.boardResults[0].team1ReportedResult").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.boardResults[0].team2ReportedResult").value("PLAYER1_WIN"))
                .andExpect(jsonPath("$.data.boardResults[1].result").value("PLAYER1_WIN"));
    }

    @Test
    @DisplayName("自動確定したボードも、その後の自己申告の変更で上書き・巻き戻りしない")
    void 自動確定は自己申告の変更で上書きされない() throws Exception {
        String token = regenerateToken();
        setVisibility("TOKEN");
        setResultInputEnabled(true);
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());
        MvcResult round = performApi(post(base() + "/team-rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode match = dataOf(round).path("round").path("matches").get(0);
        String matchId = match.path("id").asText();
        long version = match.path("version").asLong();

        performApi(reportTeamResult(token, matchId, "PLAYER1",
                        "[\"PLAYER1_WIN\",\"PLAYER1_WIN\",\"PLAYER1_WIN\"]", version))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("NONE"));
        performApi(reportTeamResult(token, matchId, "PLAYER2",
                        "[\"PLAYER1_WIN\",\"PLAYER1_WIN\",\"PLAYER1_WIN\"]", version + 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("PLAYER1_WIN"));

        // team1が申告を翻しても確定済みボードは変わらない
        performApi(reportTeamResult(token, matchId, "PLAYER1",
                        "[\"PLAYER2_WIN\",\"PLAYER1_WIN\",\"PLAYER1_WIN\"]", version + 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardResults[0].result").value("PLAYER1_WIN"));
    }

    private MockHttpServletRequestBuilder reportTeamResult(
            String token, String matchId, String reportedBy, String boardResultsJson, long version) {
        return put("/api/v1/shared/" + token + "/team-matches/" + matchId + "/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reportedBy\":\"" + reportedBy + "\",\"boardResults\":" + boardResultsJson
                        + ",\"version\":" + version + "}");
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

    private String base() {
        return "/api/v1/tournaments/" + tournamentId;
    }
}
