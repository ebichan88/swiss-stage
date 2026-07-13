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
        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"契約テスト大会\",\"gameType\":\"GO\",\"totalRounds\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        return dataOf(result);
    }

    @Test
    @DisplayName("大会を作成すると201で統一フォーマットのDTOが返る")
    void 作成() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments")
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
    @DisplayName("バリデーションエラーは400 VALIDATION_ERROR + detailsで返る")
    void バリデーション() throws Exception {
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
    @DisplayName("一覧・詳細が取得でき、他人の大会は404になる(存在を漏らさない)")
    void 取得と認可() throws Exception {
        String id = createTournament().path("id").asText();

        mockMvc.perform(get("/api/v1/tournaments").cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id));

        mockMvc.perform(get("/api/v1/tournaments/" + id).cookie(ownerCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));

        mockMvc.perform(get("/api/v1/tournaments/" + id).cookie(sessionCookie(OTHER_SUB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TOURNAMENT_NOT_FOUND"));

        // ULID形式でないIDも404(キー組み立てへの入力混入禁止)
        mockMvc.perform(get("/api/v1/tournaments/not-a-ulid").cookie(ownerCookie()))
                .andExpect(status().isNotFound());

        // パストラバーサルはSpring SecurityのHttpFirewallが400で拒否する(Phase 5〜)
        mockMvc.perform(get("/api/v1/tournaments/../etc").cookie(ownerCookie()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("更新はversion一致で成功し、不一致は409 CONFLICTになる")
    void 更新と楽観ロック() throws Exception {
        JsonNode created = createTournament();
        String id = created.path("id").asText();
        long version = created.path("version").asLong();

        mockMvc.perform(patch("/api/v1/tournaments/" + id)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"改名後\",\"version\":" + version + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("改名後"))
                .andExpect(jsonPath("$.data.version").value(version + 1));

        mockMvc.perform(patch("/api/v1/tournaments/" + id)
                        .cookie(ownerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"二重更新\",\"version\":" + version + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("参加者2名未満の大会は開始できず、削除は204で消える")
    void 開始制約と削除() throws Exception {
        String id = createTournament().path("id").asText();

        mockMvc.perform(post("/api/v1/tournaments/" + id + "/start").cookie(ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));

        mockMvc.perform(delete("/api/v1/tournaments/" + id).cookie(ownerCookie()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/tournaments/" + id).cookie(ownerCookie()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("未認証はすべて401になる")
    void 未認証() throws Exception {
        mockMvc.perform(get("/api/v1/tournaments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
