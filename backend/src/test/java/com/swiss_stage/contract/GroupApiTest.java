package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 棋力帯グループ分けのAPIコントラクト(05_swiss_pairing_algorithm.md §2.4)。
 * グループCRUD・自動振り分け・開始時検証・グループ独立のラウンド生成/順位を検証する。
 */
class GroupApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"グループテスト大会\",\"gameType\":\"GO\",\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
    }

    @Test
    @DisplayName("GRP-AC-001,GRP-AC-002,GRP-AC-003,GRP-AC-004,GRP-AC-005: "
            + "グループの作成・一覧(作成順)・改名・削除ができ、重複名や上限超過は400になる")
    void グループCRUD() throws Exception {
        // 大会作成時にデフォルトグループ「A」が自動作成されている
        String groupA = defaultGroupId();
        String groupB = createGroup("B");

        performApi(get(groupsPath()).cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("A"))
                .andExpect(jsonPath("$.data[1].name").value("B"));

        // 重複名は作成・改名とも400
        performApi(post(groupsPath()).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        performApi(patch(groupsPath() + "/" + groupB).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\"}"))
                .andExpect(status().isBadRequest());

        // 改名(同名のままの改名は自分自身を除外して判定される)
        performApi(patch(groupsPath() + "/" + groupB).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bクラス\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Bクラス"));

        // 存在しないグループは404
        performApi(patch(groupsPath() + "/" + "0".repeat(26)).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("GROUP_NOT_FOUND"));

        // 削除すると一覧から消える
        performApi(delete(groupsPath() + "/" + groupB).cookie(ownerCookie()))
                .andExpect(status().isNoContent());
        performApi(get(groupsPath()).cookie(ownerCookie()))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(groupA));

        // 最後の1グループは削除できない
        performApi(delete(groupsPath() + "/" + groupA).cookie(ownerCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // 上限10個(残り1個の状態から9個追加すると11個目で400)
        for (int i = 2; i <= 10; i++) {
            createGroup("G" + i);
        }
        performApi(post(groupsPath()).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"G11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GRP-AC-006,GRP-AC-008,GRP-AC-009: 段級位の自動振り分けは強い順に均等分割(端数は先頭)し、個別調整もできる")
    void 自動振り分けと個別調整() throws Exception {
        String groupA = defaultGroupId();
        String groupB = createGroup("B");
        // 強い順: 一(9段) > 二(初段) > 三(1級) > 四(10級) > 五(未入力)
        List<String> ids = List.of(
                addParticipant("参加 一郎", "DAN_9"),
                addParticipant("参加 二郎", "DAN_1"),
                addParticipant("参加 三郎", "KYU_1"),
                addParticipant("参加 四郎", "KYU_10"),
                addParticipant("参加 五郎", null));

        // 5名を2グループへ → A:3名(強い側)、B:2名
        MvcResult result = performApi(post(groupsPath() + "/auto-assign").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andReturn();
        List<String> assigned = new ArrayList<>();
        for (JsonNode p : dataOf(result)) {
            assigned.add(p.path("groupId").asText());
        }
        assertThat(assigned).containsExactly(groupA, groupA, groupA, groupB, groupB);

        // 個別調整: 三郎をBへ
        performApi(patch(participantsPath() + "/" + ids.get(2)).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupB + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(groupB));

        // 未知グループへの変更は400
        performApi(patch(participantsPath() + "/" + ids.get(2)).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + "0".repeat(26) + "\"}"))
                .andExpect(status().isBadRequest());

        // グループ削除で割当済み参加者は直前のグループ(A)へ移動する
        performApi(delete(groupsPath() + "/" + groupB).cookie(ownerCookie()))
                .andExpect(status().isNoContent());
        performApi(get(participantsPath()).cookie(ownerCookie()))
                .andExpect(jsonPath("$.data[3].groupId").value(groupA))
                .andExpect(jsonPath("$.data[4].groupId").value(groupA));
    }

    @Test
    @DisplayName("GRP-AC-007: 自動振り分けは棄権中の参加者の割当を変更しない")
    void 自動振り分けは棄権者の割当を維持() throws Exception {
        String groupA = defaultGroupId();
        String groupB = createGroup("B");
        addParticipant("参加 一郎", "DAN_9");
        addParticipant("参加 二郎", "DAN_5");
        String withdrawnId = addParticipant("参加 三郎", "KYU_9");
        assignGroup(withdrawnId, groupB);
        performApi(patch(participantsPath() + "/" + withdrawnId).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WITHDRAWN\"}"))
                .andExpect(status().isOk());

        // ACTIVE 2名は A/B へ1名ずつ。棄権者(三郎)の割当は B のまま変わらない
        performApi(post(groupsPath() + "/auto-assign").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupId").value(groupA))
                .andExpect(jsonPath("$.data[1].groupId").value(groupB))
                .andExpect(jsonPath("$.data[2].status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.data[2].groupId").value(groupB));
    }

    @Test
    @DisplayName("GRP-AC-010: 参加者追加はグループ省略時に先頭グループへ割り当てられ、指定時はそのグループになる")
    void 参加者追加のグループ割当() throws Exception {
        String groupA = defaultGroupId();
        String groupB = createGroup("B");

        String withoutGroup = addParticipant("参加 一郎", "DAN_1");
        performApi(get(participantsPath()).cookie(ownerCookie()))
                .andExpect(jsonPath("$.data[0].id").value(withoutGroup))
                .andExpect(jsonPath("$.data[0].groupId").value(groupA));

        performApi(post(participantsPath()).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"参加 二郎\",\"groupId\":\"" + groupB + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupId").value(groupB));

        // 未知グループの指定は400
        performApi(post(participantsPath()).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"参加 三郎\",\"groupId\":\"" + "0".repeat(26) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GRP-AC-011,GRP-AC-012: 空グループや2名未満のグループがあると開始できず、開始後のグループ操作は409になる")
    void 開始時検証() throws Exception {
        String groupB = createGroup("B");
        List<String> ids = List.of(
                addParticipant("参加 一郎", "DAN_1"),
                addParticipant("参加 二郎", "KYU_1"),
                addParticipant("参加 三郎", "KYU_5"),
                addParticipant("参加 四郎", "KYU_9"));

        // 全員デフォルトグループAのまま → Bが0名で開始不可
        performApi(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        // Bが1名のみでも開始不可
        assignGroup(ids.get(3), groupB);
        performApi(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isConflict());

        // A:2名 / B:2名 に調整すると開始できる
        assignGroup(ids.get(2), groupB);
        performApi(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isOk());

        // 開始後のグループ操作・割当変更は409
        performApi(post(groupsPath()).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"C\"}"))
                .andExpect(status().isConflict());
        performApi(delete(groupsPath() + "/" + groupB).cookie(ownerCookie()))
                .andExpect(status().isConflict());
        performApi(post(groupsPath() + "/auto-assign").cookie(ownerCookie()))
                .andExpect(status().isConflict());
        performApi(patch(participantsPath() + "/" + ids.get(0)).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupB + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GRP-AC-013,GRP-AC-014: グループ大会の一巡: グループ独立ペアリング・グループ内卓番号/BYE・グループ別順位")
    void グループ大会の一巡() throws Exception {
        String groupA = defaultGroupId();
        String groupB = createGroup("B");
        // A: 3名(奇数なのでグループ内BYE) / B: 2名。追加時の省略で先頭グループAに入る
        addParticipant("参加 一郎", "DAN_5");
        addParticipant("参加 二郎", "DAN_3");
        addParticipant("参加 三郎", "DAN_1");
        assignGroup(addParticipant("参加 四郎", "KYU_1"), groupB);
        assignGroup(addParticipant("参加 五郎", "KYU_5"), groupB);
        performApi(post(base() + "/start").cookie(ownerCookie()))
                .andExpect(status().isOk());

        MvcResult r1 = performApi(post(base() + "/rounds").cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.round.matches.length()").value(3))
                .andExpect(jsonPath("$.data.relaxations.length()").value(0))
                .andReturn();

        // グループ内で卓番号1始まり。Aは2卓(ペア+BYE)、Bは1卓。BYEはA(奇数)側のみ
        List<JsonNode> matches = new ArrayList<>();
        dataOf(r1).path("round").path("matches").forEach(matches::add);
        List<JsonNode> groupAMatches = matches.stream()
                .filter(m -> m.path("group").path("id").asText().equals(groupA)).toList();
        List<JsonNode> groupBMatches = matches.stream()
                .filter(m -> m.path("group").path("id").asText().equals(groupB)).toList();
        assertThat(groupAMatches).extracting(m -> m.path("tableNumber").asInt())
                .containsExactly(1, 2);
        assertThat(groupBMatches).extracting(m -> m.path("tableNumber").asInt())
                .containsExactly(1);
        assertThat(groupAMatches).extracting(m -> m.path("result").asText())
                .contains("BYE");
        assertThat(groupBMatches.getFirst().path("result").asText()).isNotEqualTo("BYE");
        assertThat(matches).allSatisfy(
                m -> assertThat(m.path("group").path("name").asText()).isIn("A", "B"));

        // 結果入力(BYE以外)→確定
        for (JsonNode match : matches) {
            if (!match.path("result").asText().equals("BYE")) {
                performApi(put(base() + "/matches/" + match.path("id").asText() + "/result")
                                .cookie(ownerCookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"result\":\"PLAYER1_WIN\",\"version\":"
                                        + match.path("version").asLong() + "}"))
                        .andExpect(status().isOk());
            }
        }
        performApi(post(base() + "/rounds/1/confirm").cookie(ownerCookie()))
                .andExpect(status().isOk());

        // 順位表はグループごとに独立(グループ内順位1始まり)
        performApi(get(base() + "/standings").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].group.name").value("A"))
                .andExpect(jsonPath("$.data[0].standings.length()").value(3))
                .andExpect(jsonPath("$.data[0].standings[0].rank").value(1))
                .andExpect(jsonPath("$.data[1].group.name").value("B"))
                .andExpect(jsonPath("$.data[1].standings.length()").value(2))
                .andExpect(jsonPath("$.data[1].standings[0].rank").value(1));
    }

    @Test
    @DisplayName("GRP-AC-015,GRP-AC-016: CSVのグループ列(4列)で割当付きインポートでき、未知のグループ名は行エラーになる")
    void CSVグループ列() throws Exception {
        String groupA = defaultGroupId();
        String groupB = createGroup("B");

        String csv = "氏名,所属,段級位,グループ\n"
                + "蛯名 隆,〇〇株式会社,5段,A\n"
                + "山田 花子,,初段,B\n"
                + "佐藤 一,B社,3級,\n";
        performApi(multipart(participantsPath() + "/import")
                        .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                        .cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(3))
                .andExpect(jsonPath("$.data.participants[0].groupId").value(groupA))
                .andExpect(jsonPath("$.data.participants[1].groupId").value(groupB))
                // グループ列が空欄の行は先頭グループに割り当てられる
                .andExpect(jsonPath("$.data.participants[2].groupId").value(groupA));

        // 未知のグループ名は行番号付きエラーで1件も取り込まれない
        String invalid = "氏名,所属,段級位,グループ\n正常 太郎,,初段,A\n異常 次郎,,1級,X\n";
        performApi(multipart(participantsPath() + "/import")
                        .file(csvFile(invalid.getBytes(StandardCharsets.UTF_8)))
                        .cookie(ownerCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CSV_INVALID_FORMAT"))
                .andExpect(jsonPath("$.error.details[0].field").value("3行目"));
        performApi(get(participantsPath()).cookie(ownerCookie()))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    /** 大会作成時に自動作成されるデフォルトグループ「A」のID */
    private String defaultGroupId() throws Exception {
        MvcResult result = performApi(get(groupsPath()).cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("A"))
                .andReturn();
        return dataOf(result).path(0).path("id").asText();
    }

    private String createGroup(String name) throws Exception {
        MvcResult result = performApi(post(groupsPath()).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return dataOf(result).path("id").asText();
    }

    private String addParticipant(String name, String rank) throws Exception {
        String rankJson = rank == null ? "" : ",\"rank\":\"" + rank + "\"";
        MvcResult result = performApi(post(participantsPath()).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"" + rankJson + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return dataOf(result).path("id").asText();
    }

    private void assignGroup(String participantId, String groupId) throws Exception {
        performApi(patch(participantsPath() + "/" + participantId).cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\"}"))
                .andExpect(status().isOk());
    }

    private String base() {
        return "/api/v1/tournaments/" + tournamentId;
    }

    private String groupsPath() {
        return base() + "/groups";
    }

    private String participantsPath() {
        return base() + "/participants";
    }

    private static MockMultipartFile csvFile(byte[] bytes) {
        return new MockMultipartFile("file", "participants.csv", "text/csv", bytes);
    }
}
