package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 招待リンクの発行・再発行・失効(OWNER専用。14_tournament_collaboration.md §4.4・§4.7)。 招待の承諾(招待される側)は{@link
 * InvitationApiTest}を参照。
 */
class TournamentInviteApiTest extends ApiContractTestSupport {

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private TournamentMemberRepository memberRepository;

  private String tournamentId;

  @BeforeEach
  void setUp() throws Exception {
    MvcResult result =
        performApi(
                post("/api/v1/tournaments")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"招待テスト大会\",\"gameType\":\"GO\",\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    tournamentId = dataOf(result).path("id").asText();
  }

  @Test
  @DisplayName("MBR-AC-021: 招待を一度も発行していない大会のGET /membersはinvite:nullを返す")
  void 未発行はinviteがnull() throws Exception {
    performApi(get(membersPath()).cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.invite").doesNotExist())
        .andExpect(jsonPath("$.data.maxMembers").value(9));
  }

  @Test
  @DisplayName("MBR-AC-020: 招待を一度も発行していない大会でDELETE /inviteを呼んでも204になる(冪等)")
  void 未発行の失効は冪等() throws Exception {
    performApi(delete(invitePath()).cookie(ownerCookie())).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("招待リンクを発行するとGET /membersのinviteに反映される")
  void 発行() throws Exception {
    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.invite.maxUses").value(3))
        .andExpect(jsonPath("$.data.invite.remainingUses").value(3))
        .andExpect(jsonPath("$.data.invite.token").isNotEmpty())
        .andExpect(jsonPath("$.data.invite.expiresAt").isNotEmpty());

    performApi(get(membersPath()).cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.invite.maxUses").value(3));
  }

  @Test
  @DisplayName("MBR-AC-008: 招待リンクを再発行すると旧トークンは即時無効になり、使用済みの枠がリセットされる")
  void 再発行で旧トークンが無効になり枠がリセットされる() throws Exception {
    MvcResult issued =
        performApi(
                post(invitePath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":1}"))
            .andExpect(status().isOk())
            .andReturn();
    String oldToken = dataOf(issued).path("invite").path("token").asText();

    // 使い切る
    performApi(post("/api/v1/invitations/" + oldToken + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk());

    MvcResult reissued =
        performApi(
                post(invitePath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.invite.remainingUses").value(2))
            .andReturn();
    String newToken = dataOf(reissued).path("invite").path("token").asText();
    assertThat(newToken).isNotEqualTo(oldToken);

    performApi(get("/api/v1/invitations/" + oldToken).cookie(sessionCookie("third-sub")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("INVALID_INVITE_TOKEN"));
  }

  @Test
  @DisplayName("MBR-AC-010: 招待の失効(DELETE)後はそのトークンで承諾できない")
  void 失効後は承諾できない() throws Exception {
    MvcResult issued =
        performApi(
                post(invitePath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":3}"))
            .andExpect(status().isOk())
            .andReturn();
    String token = dataOf(issued).path("invite").path("token").asText();

    performApi(delete(invitePath()).cookie(ownerCookie())).andExpect(status().isNoContent());

    performApi(post("/api/v1/invitations/" + token + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("INVALID_INVITE_TOKEN"));
  }

  @Test
  @DisplayName(
      "MBR-AC-005,MBR-AC-017: 人数枠1で発行した招待は1人が承諾すると即座に枠切れになり、" + "以後の承諾はINVALID_INVITE_TOKENになる")
  void 人数枠1は1人で枠切れになる() throws Exception {
    MvcResult issued =
        performApi(
                post(invitePath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":1}"))
            .andExpect(status().isOk())
            .andReturn();
    String token = dataOf(issued).path("invite").path("token").asText();

    performApi(post("/api/v1/invitations/" + token + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk());

    performApi(post("/api/v1/invitations/" + token + "/accept").cookie(sessionCookie("second-sub")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("INVALID_INVITE_TOKEN"));
  }

  @Test
  @DisplayName("MBR-AC-018: 招待発行のmaxUsesに0または10以上を指定すると400 VALIDATION_ERRORになる")
  void maxUsesの範囲外は400() throws Exception {
    // 意図的にスキーマ違反(schema上もminimum1/maximum9)を送るため素のperform
    mockMvc
        .perform(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":10}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  @DisplayName("MBR-AC-012,MBR-AC-019: 招待発行のmaxUsesが「9−発行時点の共同管理者数」を超えると400 VALIDATION_ERRORになる")
  void maxUsesが残り枠を超えると400() throws Exception {
    seedMembers(7);

    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":3}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":2}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
      "MBR-AC-012,MBR-AC-022: 共同管理者0人の状態でmaxUses=9を指定すると発行に成功し、"
          + "共同管理者がN人いる状態でmaxUses=9-Nちょうどを指定しても発行に成功する")
  void 上限ちょうどのmaxUsesは成功する() throws Exception {
    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":9}"))
        .andExpect(status().isOk());

    seedMembers(4);
    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":5}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
      "MBR-AC-023: 共同管理者を取り消すと発行中の招待リンクも同時に失効し、" + "取り消された人が同じリンクで再承諾してMAINTAINERに復帰することはできない")
  void 取り消しは招待リンクも道連れにする() throws Exception {
    MvcResult issued =
        performApi(
                post(invitePath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":3}"))
            .andExpect(status().isOk())
            .andReturn();
    String token = dataOf(issued).path("invite").path("token").asText();

    performApi(post("/api/v1/invitations/" + token + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk());
    String memberId =
        memberRepository
            .findBySub(new TournamentId(tournamentId), OTHER_SUB)
            .orElseThrow()
            .id()
            .value();

    performApi(delete(membersPath() + "/" + memberId).cookie(ownerCookie()))
        .andExpect(status().isNoContent());

    // 取り消し前に有効だった招待リンクは同時に失効しているため、再承諾で復帰できない
    performApi(post("/api/v1/invitations/" + token + "/accept").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("INVALID_INVITE_TOKEN"));
  }

  @Test
  @DisplayName("MBR-AC-024: 招待の発行・失効は大会の状態(PREPARING/IN_PROGRESS/FINISHED)を問わず利用できる")
  void 大会の状態を問わず利用できる() throws Exception {
    // 開始条件を満たすため参加者を2名追加してから開始する
    performApi(
            post("/api/v1/tournaments/" + tournamentId + "/participants")
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"参加 一郎\"}"))
        .andExpect(status().isCreated());
    performApi(
            post("/api/v1/tournaments/" + tournamentId + "/participants")
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"参加 二郎\"}"))
        .andExpect(status().isCreated());
    performApi(post("/api/v1/tournaments/" + tournamentId + "/start").cookie(ownerCookie()))
        .andExpect(status().isOk());

    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":3}"))
        .andExpect(status().isOk());
    performApi(delete(invitePath()).cookie(ownerCookie())).andExpect(status().isNoContent());
  }

  private void seedMembers(int count) {
    Tournament tournament =
        tournamentRepository.findById(new TournamentId(tournamentId)).orElseThrow();
    for (int i = 0; i < count; i++) {
      memberRepository.save(
          tournament.id(),
          TournamentMember.create("seed-sub-" + tournamentId + "-" + i, "シード" + i, Instant.now()),
          tournament.createdAt());
    }
  }

  private String membersPath() {
    return "/api/v1/tournaments/" + tournamentId + "/members";
  }

  private String invitePath() {
    return "/api/v1/tournaments/" + tournamentId + "/invite";
  }
}
