package com.swiss_stage.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/**
 * 招待の受諾(招待される側。14_tournament_collaboration.md §4.4・§4.7)。発行・失効は{@link TournamentInviteApiTest}を参照
 */
class InvitationApiTest extends ApiContractTestSupport {

  private String tournamentId;
  private String token;

  @BeforeEach
  void setUp() throws Exception {
    MvcResult tournament =
        performApi(
                post("/api/v1/tournaments")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"招待受諾テスト大会\",\"gameType\":\"GO\",\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    tournamentId = dataOf(tournament).path("id").asText();

    MvcResult issued =
        performApi(
                post("/api/v1/tournaments/" + tournamentId + "/invite")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":3}"))
            .andExpect(status().isOk())
            .andReturn();
    token = dataOf(issued).path("invite").path("token").asText();
  }

  @Test
  @DisplayName("招待のプレビューは大会情報・有効期限・alreadyMember:falseを返す")
  void プレビュー() throws Exception {
    performApi(get(invitationPath()).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tournamentId").value(tournamentId))
        .andExpect(jsonPath("$.data.tournamentName").value("招待受諾テスト大会"))
        .andExpect(jsonPath("$.data.gameType").value("GO"))
        .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
        .andExpect(jsonPath("$.data.alreadyMember").value(false));
  }

  @Test
  @DisplayName("MBR-AC-027: GET /invitations/{token}はOWNER本人・既存MAINTAINERに対してalreadyMember:trueを返す")
  void プレビューは既存メンバーにalreadyMemberを返す() throws Exception {
    performApi(get(invitationPath()).cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.alreadyMember").value(true));

    performApi(post(invitationPath() + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk());
    performApi(get(invitationPath()).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.alreadyMember").value(true));
  }

  @Test
  @DisplayName("MBR-AC-004: 存在しない・形式不正な招待トークンはいずれも同一の403 INVALID_INVITE_TOKENになり、理由を出し分けない")
  void 無効なトークンは統一エラーになる() throws Exception {
    performApi(get("/api/v1/invitations/" + "Z".repeat(43)).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("INVALID_INVITE_TOKEN"));

    // 意図的にスキーマの範囲外(短すぎる)トークンを送るため素のperform
    mockMvc
        .perform(get("/api/v1/invitations/short").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("INVALID_INVITE_TOKEN"));

    performApi(
            post("/api/v1/invitations/" + "Z".repeat(43) + "/accept")
                .cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("INVALID_INVITE_TOKEN"));
  }

  @Test
  @DisplayName("MBR-AC-001: 招待を承諾したユーザーはMAINTAINERとして登録され、" + "その大会の参加者管理・ラウンド進行・結果入力APIを実行できる")
  void 承諾するとMAINTAINERとして操作できる() throws Exception {
    MvcResult acceptResult =
        performApi(post(invitationPath() + "/accept").cookie(sessionCookie(OTHER_SUB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tournamentId").value(tournamentId))
            .andReturn();
    JsonNode accepted = dataOf(acceptResult);
    org.assertj.core.api.Assertions.assertThat(accepted.path("tournamentId").asText())
        .isEqualTo(tournamentId);

    // 一覧APIで正しくMAINTAINERとして返る
    performApi(get("/api/v1/tournaments/" + tournamentId).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.role").value("MAINTAINER"));

    // 参加者管理・ラウンド進行などの通常操作ができる
    performApi(
            post("/api/v1/tournaments/" + tournamentId + "/participants")
                .cookie(sessionCookie(OTHER_SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"MAINTAINER追加\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("MBR-AC-011: OWNER本人・既にMAINTAINERのユーザーが承諾しても二重登録されず、人数枠も消費しない")
  void 二重承諾は冪等() throws Exception {
    performApi(post(invitationPath() + "/accept").cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tournamentId").value(tournamentId));

    performApi(post(invitationPath() + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk());
    // 既存MAINTAINERの再承諾も枠を消費せず成功する
    performApi(post(invitationPath() + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk());

    performApi(get("/api/v1/tournaments/" + tournamentId + "/members").cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.members.length()").value(1))
        .andExpect(jsonPath("$.data.invite.remainingUses").value(2));
  }

  private String invitationPath() {
    return "/api/v1/invitations/" + token;
  }
}
