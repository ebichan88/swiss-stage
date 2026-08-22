package com.swiss_stage.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.model.TournamentMemberId;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 共同管理者の認可基盤(14_tournament_collaboration.md §4.1〜§4.3)のコントラクトテスト。
 * 招待リンクの発行・失効・承諾は別PRで実装するため、このテストでは共同管理者を {@link TournamentMemberRepository} 経由で直接用意する(承諾フローの代わり)。
 */
class TournamentMemberApiTest extends ApiContractTestSupport {

  @Autowired private TournamentMemberRepository memberRepository;
  @Autowired private TournamentRepository tournamentRepository;

  private String tournamentId;

  @BeforeEach
  void setUp() throws Exception {
    MvcResult result =
        performApi(
                post("/api/v1/tournaments")
                    .cookie(ownerCookie())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"共同管理テスト大会\",\"gameType\":\"GO\",\"competitionType\":\"INDIVIDUAL\",\"totalRounds\":3}"))
            .andExpect(status().isCreated())
            .andReturn();
    tournamentId = dataOf(result).path("id").asText();
  }

  @Test
  @DisplayName("MBR-AC-002: MAINTAINERが大会設定・削除・共有トークン再発行・招待/メンバー管理APIを呼ぶと403 FORBIDDENになる")
  void MAINTAINERは設定系APIで403になる() throws Exception {
    addMaintainer(OTHER_SUB, "共同管理 太郎");

    performApi(
            patch("/api/v1/tournaments/" + tournamentId)
                .cookie(sessionCookie(OTHER_SUB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"改名\",\"version\":1}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

    performApi(
            post("/api/v1/tournaments/" + tournamentId + "/share-token/regenerate")
                .cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden());

    performApi(get(membersPath()).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden());

    performApi(
            delete(membersPath() + "/" + TournamentMemberId.generate().value())
                .cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden());

    performApi(delete("/api/v1/tournaments/" + tournamentId).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName(
      "MBR-AC-003: どのメンバーでもないユーザーは大会APIすべてで404 TOURNAMENT_NOT_FOUNDになり、" + "403との差で所属を推測できない")
  void 非メンバーは404になる() throws Exception {
    performApi(get("/api/v1/tournaments/" + tournamentId).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("TOURNAMENT_NOT_FOUND"));

    performApi(get(membersPath()).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isNotFound());

    performApi(
            get("/api/v1/tournaments/" + tournamentId + "/participants")
                .cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("MBR-AC-006: 共同管理者一覧・招待情報はOWNERにのみ返り、MAINTAINER・未認証には返らない")
  void 共同管理者一覧はOWNER専用() throws Exception {
    addMaintainer(OTHER_SUB, "共同管理 太郎");

    performApi(get(membersPath()).cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.members.length()").value(1))
        .andExpect(jsonPath("$.data.members[0].displayName").value("共同管理 太郎"))
        .andExpect(jsonPath("$.data.members[0].role").value("MAINTAINER"))
        .andExpect(jsonPath("$.data.invite").doesNotExist())
        .andExpect(jsonPath("$.data.maxMembers").value(9));

    performApi(get(membersPath()).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isForbidden());

    performApi(get(membersPath())).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("MBR-AC-007: MAINTAINER向けの大会レスポンスにはshareTokenが含まれない")
  void MAINTAINER向けレスポンスにshareTokenが含まれない() throws Exception {
    performApi(
            post("/api/v1/tournaments/" + tournamentId + "/share-token/regenerate")
                .cookie(ownerCookie()))
        .andExpect(status().isOk());
    addMaintainer(OTHER_SUB, "共同管理 太郎");

    performApi(get("/api/v1/tournaments/" + tournamentId).cookie(ownerCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.shareToken").isNotEmpty())
        .andExpect(jsonPath("$.data.role").value("OWNER"));

    performApi(get("/api/v1/tournaments/" + tournamentId).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.shareToken").doesNotExist())
        .andExpect(jsonPath("$.data.role").value("MAINTAINER"));
  }

  @Test
  @DisplayName("MBR-AC-009: OWNERが共同管理者を取り消すと、取り消された側は以後その大会で404になり一覧からも消える")
  void 取り消し後は404になり一覧から消える() throws Exception {
    String memberId = addMaintainer(OTHER_SUB, "共同管理 太郎");

    // OTHER_SUBは他のテストでも使われ一覧が蓄積されうるため、件数ではなく該当IDの有無で検証する
    assertThat(tournamentIdsOf(sessionCookie(OTHER_SUB))).contains(tournamentId);

    performApi(delete(membersPath() + "/" + memberId).cookie(ownerCookie()))
        .andExpect(status().isNoContent());

    performApi(get("/api/v1/tournaments/" + tournamentId).cookie(sessionCookie(OTHER_SUB)))
        .andExpect(status().isNotFound());
    assertThat(tournamentIdsOf(sessionCookie(OTHER_SUB))).doesNotContain(tournamentId);
  }

  private List<String> tournamentIdsOf(Cookie cookie) throws Exception {
    MvcResult result = performApi(get("/api/v1/tournaments").cookie(cookie)).andReturn();
    List<String> ids = new ArrayList<>();
    dataOf(result).forEach(item -> ids.add(item.path("id").asText()));
    return ids;
  }

  @Test
  @DisplayName("MBR-AC-026: 存在しないmemberIdのDELETEは404 TOURNAMENT_MEMBER_NOT_FOUNDになる")
  void 存在しないmemberIdの削除は404になる() throws Exception {
    performApi(
            delete(membersPath() + "/" + TournamentMemberId.generate().value())
                .cookie(ownerCookie()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("TOURNAMENT_MEMBER_NOT_FOUND"));
  }

  private String addMaintainer(String sub, String displayName) {
    Tournament tournament =
        tournamentRepository.findById(new TournamentId(tournamentId)).orElseThrow();
    TournamentMember member = TournamentMember.create(sub, displayName, Instant.now());
    memberRepository.save(tournament.id(), member, tournament.createdAt());
    return member.id().value();
  }

  private String membersPath() {
    return "/api/v1/tournaments/" + tournamentId + "/members";
  }
}
