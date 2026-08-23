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
import com.swiss_stage.domain.repository.TournamentInviteRepository;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
  @Autowired private TournamentInviteRepository inviteRepository;

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
      "MBR-AC-017: 人数枠1で発行した招待は1人が承諾すると即座に枠切れになり、"
          + "枠を超えるMAINTAINERは作られず以後の承諾はINVALID_INVITE_TOKENになる")
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

    // 枠を超えた側はMAINTAINERとして登録されない
    assertThat(memberRepository.findBySub(new TournamentId(tournamentId), "second-sub")).isEmpty();
  }

  @Test
  @DisplayName("MBR-AC-005: 人数枠を超える同時承諾は上限で打ち切られ、枠を超えるMAINTAINERは作られない")
  void 同時承諾は上限で打ち切られる() throws Exception {
    MvcResult issued =
        performApi(
                post(invitePath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":1}"))
            .andExpect(status().isOk())
            .andReturn();
    String token = dataOf(issued).path("invite").path("token").asText();

    int concurrency = 16;
    ExecutorService pool = Executors.newFixedThreadPool(concurrency);
    CyclicBarrier barrier = new CyclicBarrier(concurrency);
    try {
      List<Future<Integer>> futures = new ArrayList<>();
      for (int i = 0; i < concurrency; i++) {
        String sub = "concurrent-sub-" + i;
        futures.add(
            pool.submit(
                () -> {
                  barrier.await();
                  MvcResult result =
                      performApi(
                              post("/api/v1/invitations/" + token + "/accept")
                                  .cookie(sessionCookie(sub)))
                          .andReturn();
                  return result.getResponse().getStatus();
                }));
      }
      int successCount = 0;
      for (Future<Integer> future : futures) {
        int httpStatus = future.get(10, TimeUnit.SECONDS);
        if (httpStatus == 200) {
          successCount++;
        } else {
          assertThat(httpStatus).isEqualTo(403);
        }
      }
      // 人数枠1のため、同時に何人が承諾を試みても成功はちょうど1人に打ち切られる
      assertThat(successCount).isEqualTo(1);
    } finally {
      pool.shutdown();
    }
    assertThat(memberRepository.findByTournamentId(new TournamentId(tournamentId))).hasSize(1);
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
  @DisplayName("MBR-AC-019: 招待発行のmaxUsesが「9−発行時点の共同管理者数」を超えると400 VALIDATION_ERRORになる")
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
      "MBR-AC-012: 共同管理者は9人(OWNER含め10人)を超えて追加できず、"
          + "招待を再発行しても上限は回避できない(再発行時もmaxUsesの上限が発行時点の共同管理者数で再計算される)")
  void 上限は再発行でも回避できない() throws Exception {
    seedMembers(7);
    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":2}"))
        .andExpect(status().isOk());

    // 再発行時も同じ上限(9-7=2)が適用され、超える指定は拒否される
    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":3}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

    // 上限ちょうどでの再発行(枠のリセット)は成功する
    performApi(
            post(invitePath())
                .cookie(ownerCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxUses\":2}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
      "MBR-AC-022: 共同管理者0人の状態でmaxUses=9を指定すると発行に成功し、" + "共同管理者がN人いる状態でmaxUses=9-Nちょうどを指定しても発行に成功する")
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
  @DisplayName(
      "MBR-AC-024: 招待の発行・失効・承諾、共同管理者の取り消しは" + "大会の状態(PREPARING/IN_PROGRESS/FINISHED)を問わず利用できる")
  void 大会の状態を問わず利用できる() throws Exception {
    // PREPARING: 発行→承諾→取り消し(招待も道連れに失効)がひととおり成功する
    issueAcceptAndRemove("prep-sub");

    // 開始条件を満たすため参加者を2名追加してからIN_PROGRESSへ進める
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

    // IN_PROGRESS: 同様にひととおり成功する
    issueAcceptAndRemove("progress-sub");

    // FINISHEDへ進める(ラウンドを消化しなくても終了できる。Tournament.finish()参照)
    performApi(post("/api/v1/tournaments/" + tournamentId + "/finish").cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("FINISHED"));

    // FINISHED: 同様にひととおり成功する(監査目的で共同管理者を整理できる必要がある)
    issueAcceptAndRemove("finished-sub");
  }

  /** 招待の発行→承諾→共同管理者の取り消し(招待も道連れに失効)がその時点の大会の状態で一通り成功することを確認する */
  private void issueAcceptAndRemove(String sub) throws Exception {
    MvcResult issued =
        performApi(
                post(invitePath())
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"maxUses\":1}"))
            .andExpect(status().isOk())
            .andReturn();
    String token = dataOf(issued).path("invite").path("token").asText();

    performApi(post("/api/v1/invitations/" + token + "/accept").cookie(sessionCookie(sub)))
        .andExpect(status().isOk());
    String memberId =
        memberRepository.findBySub(new TournamentId(tournamentId), sub).orElseThrow().id().value();

    performApi(delete(membersPath() + "/" + memberId).cookie(ownerCookie()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("MBR-AC-013: 大会を削除すると共同管理者・招待アイテムも物理削除され、MAINTAINERの大会一覧から消える")
  void 大会削除で共同管理者と招待も物理削除される() throws Exception {
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

    TournamentId id = new TournamentId(tournamentId);
    assertThat(memberRepository.findBySub(id, OTHER_SUB)).isPresent();
    assertThat(inviteRepository.findByTournamentId(id)).isPresent();
    performApi(get("/api/v1/tournaments").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(jsonPath("$.data[?(@.id=='" + tournamentId + "')]").isNotEmpty());

    performApi(delete("/api/v1/tournaments/" + tournamentId).cookie(ownerCookie()))
        .andExpect(status().isNoContent());

    assertThat(memberRepository.findBySub(id, OTHER_SUB)).isEmpty();
    assertThat(inviteRepository.findByTournamentId(id)).isEmpty();
    performApi(get("/api/v1/tournaments").cookie(sessionCookie(OTHER_SUB)))
        .andExpect(jsonPath("$.data[?(@.id=='" + tournamentId + "')]").isEmpty());
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
