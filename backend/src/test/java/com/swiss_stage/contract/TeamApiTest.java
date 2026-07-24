package com.swiss_stage.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 団体戦(competitionType=TEAM)のチーム・メンバー管理APIコントラクト
 * (05_swiss_pairing_algorithm.md §5.1)。
 */
class TeamApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"団体戦テスト大会\",\"gameType\":\"GO\","
                                + "\"competitionType\":\"TEAM\",\"teamSize\":3,\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
    }

    @Test
    @DisplayName("TEAM-AC-001,TEAM-AC-002: 大会作成時にcompetitionType/teamSizeを指定でき、"
            + "不正な指定は400になる")
    void 大会作成時のチーム制指定() throws Exception {
        performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"個人戦にteamSize\",\"gameType\":\"GO\","
                                + "\"competitionType\":\"INDIVIDUAL\",\"teamSize\":3,\"totalRounds\":3}"))
                .andExpect(status().isBadRequest());
        performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"団体戦teamSizeなし\",\"gameType\":\"GO\","
                                + "\"competitionType\":\"TEAM\",\"totalRounds\":3}"))
                .andExpect(status().isBadRequest());
        // teamSizeはスキーマ上も3/5のみ許容(意図的にスキーマ違反を送るため素のperform)
        mockMvc.perform(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"4チーム制\",\"gameType\":\"GO\","
                                + "\"competitionType\":\"TEAM\",\"teamSize\":4,\"totalRounds\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEAM-AC-003,TEAM-AC-004,TEAM-AC-005: チームを作成するとエントリー順が自動採番され、"
            + "メンバーのboardPosition重複・範囲外は400になる")
    void チーム作成とメンバー追加() throws Exception {
        String teamId = createTeam("Aチーム");

        performApi(post(membersPath(teamId))
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"主将 一郎\",\"rank\":\"DAN_3\",\"boardPosition\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.members.length()").value(1))
                .andExpect(jsonPath("$.data.members[0].boardPosition").value(1));

        // 範囲外(3人制で4)は400
        performApi(post(membersPath(teamId))
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"範囲外\",\"boardPosition\":4}"))
                .andExpect(status().isBadRequest());

        // 重複(既に1がいる)は400
        performApi(post(membersPath(teamId))
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"重複\",\"boardPosition\":1}"))
                .andExpect(status().isBadRequest());

        String secondTeamId = createTeam("Bチーム");
        performApi(get(teamsPath()).cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].entryOrder").value(1))
                .andExpect(jsonPath("$.data[1].entryOrder").value(2));
        org.assertj.core.api.Assertions.assertThat(secondTeamId).isNotBlank();
    }

    @Test
    @DisplayName("TEAM-AC-006: 補欠人数の上限(3チーム制=2名)を超える追加は400になる")
    void 補欠人数の上限() throws Exception {
        String teamId = createTeam("補欠チーム");
        addMember(teamId, "補欠1", null);
        addMember(teamId, "補欠2", null);
        performApi(post(membersPath(teamId))
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"補欠3\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEAM-AC-007: 必須ポジションが揃っていないチームがあると大会を開始できない")
    void 必須ポジション未充足で開始不可() throws Exception {
        String teamA = createTeam("Aチーム");
        String teamB = createTeam("Bチーム");
        fillRequiredPositions(teamA, 3);
        addMember(teamB, "主将のみ", 1);

        performApi(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        addMember(teamB, "副将", 2);
        addMember(teamB, "三将", 3);
        performApi(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("TEAM-AC-008: チーム+メンバー一覧のCSVインポートができ、行エラーは1件も取り込まれない")
    void CSVインポート() throws Exception {
        String csv = "チーム名,氏名,段級位,ポジション\n"
                + "Aチーム,主将 一郎,3段,主将\n"
                + "Aチーム,副将 二郎,初段,副将\n"
                + "Aチーム,三将 三郎,,三将\n"
                + "Aチーム,補欠 四郎,5級,\n"
                + "Bチーム,主将 五郎,,主将\n"
                + "Bチーム,副将 六郎,,副将\n"
                + "Bチーム,三将 七郎,,三将\n";
        performApi(multipart(teamsPath() + "/csv-import")
                        .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                        .cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedTeamCount").value(2))
                .andExpect(jsonPath("$.data.teams[0].name").value("Aチーム"))
                .andExpect(jsonPath("$.data.teams[0].members.length()").value(4))
                .andExpect(jsonPath("$.data.teams[1].name").value("Bチーム"))
                .andExpect(jsonPath("$.data.teams[1].members.length()").value(3));

        String badCsv = "チーム名,氏名,段級位,ポジション\nAチーム,一郎,,四将\n";
        performApi(multipart(teamsPath() + "/csv-import")
                        .file(csvFile(badCsv.getBytes(StandardCharsets.UTF_8)))
                        .cookie(ownerCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CSV_INVALID_FORMAT"));
    }

    @Test
    @DisplayName("TEAM-AC-009: 大会開始後のチーム追加・削除・メンバー構成変更は409になる"
            + "(棄権(WITHDRAWN)は開始後も可)")
    void 開始後の制約() throws Exception {
        String teamA = createTeam("Aチーム");
        String teamB = createTeam("Bチーム");
        fillRequiredPositions(teamA, 3);
        fillRequiredPositions(teamB, 3);
        performApi(post(base() + "/start").cookie(ownerCookie())).andExpect(status().isOk());

        performApi(post(teamsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"遅刻チーム\"}"))
                .andExpect(status().isConflict());
        performApi(delete(teamsPath() + "/" + teamA).cookie(ownerCookie()))
                .andExpect(status().isConflict());
        performApi(post(membersPath(teamA))
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"追加補欠\"}"))
                .andExpect(status().isConflict());

        performApi(patch(teamsPath() + "/" + teamA)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WITHDRAWN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    @Test
    @DisplayName("TEAM-AC-010,TEAM-AC-011: グループ分けはチーム単位で使え、"
            + "段級位による自動振り分けは団体戦では非公開になる")
    void グループのチーム対応() throws Exception {
        String teamId = createTeam("Aチーム");
        MvcResult groupResult = performApi(post(base() + "/groups")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bグループ\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String groupId = dataOf(groupResult).path("id").asText();

        performApi(patch(teamsPath() + "/" + teamId)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(groupId));

        performApi(post(base() + "/groups/auto-assign").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    private void fillRequiredPositions(String teamId, int teamSize) throws Exception {
        for (int position = 1; position <= teamSize; position++) {
            addMember(teamId, "メンバー" + position, position);
        }
    }

    private void addMember(String teamId, String name, Integer boardPosition) throws Exception {
        String body = boardPosition == null
                ? "{\"name\":\"" + name + "\"}"
                : "{\"name\":\"" + name + "\",\"boardPosition\":" + boardPosition + "}";
        performApi(post(membersPath(teamId))
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String createTeam(String name) throws Exception {
        MvcResult result = performApi(post(teamsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode data = dataOf(result);
        return data.path("id").asText();
    }

    private String teamsPath() {
        return base() + "/teams";
    }

    private String membersPath(String teamId) {
        return teamsPath() + "/" + teamId + "/members";
    }

    private String base() {
        return "/api/v1/tournaments/" + tournamentId;
    }

    private static MockMultipartFile csvFile(byte[] bytes) {
        return new MockMultipartFile("file", "teams.csv", "text/csv", bytes);
    }
}
