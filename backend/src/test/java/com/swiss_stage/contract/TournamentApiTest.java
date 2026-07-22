package com.swiss_stage.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class TournamentApiTest extends ApiContractTestSupport {

    private JsonNode createTournament() throws Exception {
        MvcResult result = performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"契約テスト大会\",\"gameType\":\"GO\",\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        return dataOf(result);
    }

    @Test
    @DisplayName("TRN-AC-001: 大会を作成すると201で統一フォーマットのDTOが返る")
    void 作成() throws Exception {
        performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新規大会\",\"gameType\":\"SHOGI\",\"totalRounds\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("新規大会"))
                .andExpect(jsonPath("$.data.gameType").value("SHOGI"))
                .andExpect(jsonPath("$.data.status").value("PREPARING"))
                .andExpect(jsonPath("$.data.currentRound").value(0))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.meta.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("TRN-AC-002: バリデーションエラーは400 VALIDATION_ERROR + detailsで返る")
    void バリデーション() throws Exception {
        // 意図的にスキーマ違反のリクエストを送るため素のperform(performApiのjavadoc参照)
        mockMvc.perform(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"gameType\":\"GO\",\"totalRounds\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isArray())
                .andExpect(jsonPath("$.error.details.length()").value(2));
    }

    @Test
    @DisplayName("TRN-AC-011: totalRoundsが上限(8)を超えると400 VALIDATION_ERRORになる")
    void ラウンド数上限超過() throws Exception {
        // 意図的にスキーマ違反のリクエストを送るため素のperform(performApiのjavadoc参照)
        mockMvc.perform(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"上限超過大会\",\"gameType\":\"GO\",\"totalRounds\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("TRN-AC-011: totalRoundsが上限(8)ちょうどなら201で作成できる")
    void ラウンド数上限ちょうど() throws Exception {
        performApi(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"上限大会\",\"gameType\":\"GO\",\"totalRounds\":8}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("上限大会"));
    }

    @Test
    @DisplayName("TRN-AC-003,TRN-AC-004,TRN-AC-005: 一覧・詳細が取得でき、他人の大会・不正形式IDは404になる(存在を漏らさない)")
    void 取得と認可() throws Exception {
        String id = createTournament().path("id").asText();

        performApi(get("/api/v1/tournaments").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id));

        performApi(get("/api/v1/tournaments/" + id).cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));

        performApi(get("/api/v1/tournaments/" + id).cookie(sessionCookie(OTHER_SUB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TOURNAMENT_NOT_FOUND"));

        // ULID形式でないIDも404(キー組み立てへの入力混入禁止)。スキーマ違反のIDを送るため素のperform
        mockMvc.perform(get("/api/v1/tournaments/not-a-ulid").cookie(ownerCookie()))
                .andExpect(status().isNotFound());

        // パストラバーサルはSpring SecurityのHttpFirewallが400で拒否する(Phase 5〜)
        mockMvc.perform(get("/api/v1/tournaments/../etc").cookie(ownerCookie()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TRN-AC-006,TRN-AC-007: 更新はversion一致で成功し、不一致は409 CONFLICTになる")
    void 更新と楽観ロック() throws Exception {
        JsonNode created = createTournament();
        String id = created.path("id").asText();
        long version = created.path("version").asLong();

        performApi(patch("/api/v1/tournaments/" + id)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"改名後\",\"version\":" + version + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("改名後"))
                .andExpect(jsonPath("$.data.version").value(version + 1));

        performApi(patch("/api/v1/tournaments/" + id)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"二重更新\",\"version\":" + version + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("TRN-AC-008,TRN-AC-009: 参加者2名未満の大会は開始できず、削除は204で消える")
    void 開始制約と削除() throws Exception {
        String id = createTournament().path("id").asText();

        performApi(post("/api/v1/tournaments/" + id + "/start").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        performApi(delete("/api/v1/tournaments/" + id).cookie(ownerCookie()))
                .andExpect(status().isNoContent());
        performApi(get("/api/v1/tournaments/" + id).cookie(ownerCookie()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TRN-AC-010: 未認証はすべて401になる")
    void 未認証() throws Exception {
        performApi(get("/api/v1/tournaments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
