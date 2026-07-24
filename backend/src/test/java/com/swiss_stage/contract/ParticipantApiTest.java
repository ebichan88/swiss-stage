package com.swiss_stage.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class ParticipantApiTest extends ApiContractTestSupport {

    private String tournamentId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"参加者テスト大会\",\"gameType\":\"GO\",\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        tournamentId = dataOf(result).path("id").asText();
    }

    @Test
    @DisplayName("PTC-AC-001: 参加者を追加するとエントリー順が自動採番される")
    void 追加() throws Exception {
        performApi(post(participantsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"一人目\",\"organization\":\"A社\",\"rank\":\"DAN_3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entryOrder").value(1))
                .andExpect(jsonPath("$.data.rank").value("DAN_3"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        performApi(post(participantsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"二人目\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entryOrder").value(2))
                .andExpect(jsonPath("$.data.organization").doesNotExist());

        performApi(get(participantsPath()).cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("PTC-AC-002: CSVインポート(UTF-8)で全行取り込める")
    void CSVインポートUTF8() throws Exception {
        String csv = "氏名,所属,段級位\n蛯名 隆,〇〇株式会社,3級\n山田 花子,,初段\n佐藤 一,B社,\n";
        performApi(multipart(participantsPath() + "/import")
                        .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                        .cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(3))
                .andExpect(jsonPath("$.data.participants[0].name").value("蛯名 隆"))
                .andExpect(jsonPath("$.data.participants[0].rank").value("KYU_3"))
                .andExpect(jsonPath("$.data.participants[1].rank").value("DAN_1"))
                .andExpect(jsonPath("$.data.participants[2].rank").doesNotExist());
    }

    @Test
    @DisplayName("PTC-AC-003: CSVインポート(Shift_JIS)も自動判定して取り込める")
    void CSVインポートShiftJIS() throws Exception {
        String csv = "氏名,所属,段級位\n蛯名 隆,囲碁部,5段\n";
        performApi(multipart(participantsPath() + "/import")
                        .file(csvFile(csv.getBytes(Charset.forName("windows-31j"))))
                        .cookie(ownerCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.participants[0].name").value("蛯名 隆"))
                .andExpect(jsonPath("$.data.participants[0].organization").value("囲碁部"))
                .andExpect(jsonPath("$.data.participants[0].rank").value("DAN_5"));
    }

    @Test
    @DisplayName("PTC-AC-004: CSVの行エラーは行番号付きdetailsで400になり、1件も取り込まれない")
    void CSVインポート行エラー() throws Exception {
        String csv = "氏名,所属,段級位\n,A社,3級\n正常 太郎,B社,初段\n異常 次郎,C社,超段\n";
        performApi(multipart(participantsPath() + "/import")
                        .file(csvFile(csv.getBytes(StandardCharsets.UTF_8)))
                        .cookie(ownerCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CSV_INVALID_FORMAT"))
                .andExpect(jsonPath("$.error.details.length()").value(2))
                .andExpect(jsonPath("$.error.details[0].field").value("2行目"))
                .andExpect(jsonPath("$.error.details[1].field").value("4行目"));

        performApi(get(participantsPath()).cookie(ownerCookie()))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("PTC-AC-005: ヘッダー行が不正なCSVは400になる")
    void CSVヘッダー不正() throws Exception {
        performApi(multipart(participantsPath() + "/import")
                        .file(csvFile("name,org,rank\nx,y,z\n".getBytes(StandardCharsets.UTF_8)))
                        .cookie(ownerCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CSV_INVALID_FORMAT"))
                .andExpect(jsonPath("$.error.details[0].field").value("1行目"));
    }

    @Test
    @DisplayName("PTC-AC-006,PTC-AC-007: 棄権(PATCH)はいつでもでき、大会開始後の追加・削除は409になる")
    void 開始後の制約と棄権() throws Exception {
        MvcResult p1 = performApi(post(participantsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"参加 一郎\"}"))
                .andExpect(status().isCreated()).andReturn();
        performApi(post(participantsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"参加 二郎\"}"))
                .andExpect(status().isCreated());
        String participantId = dataOf(p1).path("id").asText();

        performApi(post("/api/v1/tournaments/" + tournamentId + "/start").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        performApi(post(participantsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"遅刻 三郎\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        performApi(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete(participantsPath() + "/" + participantId).cookie(ownerCookie()))
                .andExpect(status().isConflict());

        performApi(patch(participantsPath() + "/" + participantId)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WITHDRAWN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    @Test
    @DisplayName("PTC-AC-008,PTC-AC-009: clearRank=trueで棋力を未入力に戻せ(rankとの同時指定は400)、未指定項目は失われない")
    void 棋力のクリア() throws Exception {
        MvcResult created = performApi(post(participantsPath())
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"棋力 未定\",\"rank\":\"DAN_3\"}"))
                .andExpect(status().isCreated()).andReturn();
        String participantId = dataOf(created).path("id").asText();

        String groupId = dataOf(created).path("groupId").asText();
        performApi(patch(participantsPath() + "/" + participantId)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearRank\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rank").doesNotExist())
                .andExpect(jsonPath("$.data.name").value("棋力 未定"))
                // グループ割当は他項目の更新で失われない
                .andExpect(jsonPath("$.data.groupId").value(groupId));

        performApi(patch(participantsPath() + "/" + participantId)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rank\":\"KYU_1\",\"clearRank\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private String participantsPath() {
        return "/api/v1/tournaments/" + tournamentId + "/participants";
    }

    private static MockMultipartFile csvFile(byte[] bytes) {
        return new MockMultipartFile("file", "participants.csv", "text/csv", bytes);
    }
}
