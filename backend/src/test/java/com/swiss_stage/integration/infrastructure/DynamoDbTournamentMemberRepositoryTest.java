package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.model.TournamentMemberId;
import com.swiss_stage.domain.model.TournamentRole;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbTournamentMemberRepositoryTest extends DynamoDbRepositoryTestSupport {

  private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

  @Autowired TournamentMemberRepository repository;
  @Autowired TournamentRepository tournamentRepository;

  @Test
  @DisplayName("共同管理者を保存・取得・削除できる")
  void 保存と取得と削除() {
    Tournament tournament = createTournament(uniqueSub());
    String memberSub = uniqueSub();
    TournamentMember member = TournamentMember.create(memberSub, "表示名一郎", NOW);
    repository.save(tournament.id(), member, tournament.createdAt());

    TournamentMember found = repository.findBySub(tournament.id(), memberSub).orElseThrow();
    assertThat(found.id()).isEqualTo(member.id());
    assertThat(found.sub()).isEqualTo(memberSub);
    assertThat(found.role()).isEqualTo(TournamentRole.MAINTAINER);
    assertThat(found.displayName()).isEqualTo("表示名一郎");
    assertThat(found.joinedAt()).isEqualTo(NOW);

    assertThat(repository.findByMemberId(tournament.id(), member.id())).contains(member);
    assertThat(repository.findByTournamentId(tournament.id())).containsExactly(member);

    repository.delete(tournament.id(), member.id());
    assertThat(repository.findBySub(tournament.id(), memberSub)).isEmpty();
    assertThat(repository.findByMemberId(tournament.id(), member.id())).isEmpty();
    assertThat(repository.findByTournamentId(tournament.id())).isEmpty();
  }

  @Test
  @DisplayName("存在しない共同管理者はemptyを返す")
  void 未存在() {
    Tournament tournament = createTournament(uniqueSub());
    assertThat(repository.findBySub(tournament.id(), uniqueSub())).isEmpty();
    assertThat(repository.findByMemberId(tournament.id(), TournamentMemberId.generate())).isEmpty();
  }

  @Test
  @DisplayName("findTournamentIdsByUserSubは、同じsubがGSI1に相乗りする所有大会(TOURNAMENTアイテム)を混入させない")
  void 所有大会アイテムの混入防止() {
    String sub = uniqueSub();
    // subが所有者の大会(GSI1PK=USER#{sub}、entityType=TOURNAMENT)
    Tournament owned = createTournament(sub);
    // subが共同管理者として参加している別の大会(GSI1PK=USER#{sub}、entityType=MEMBER)
    Tournament other = createTournament(uniqueSub());
    TournamentMember member = TournamentMember.create(sub, "表示名二郎", NOW);
    repository.save(other.id(), member, other.createdAt());

    List<TournamentId> found = awaitNonEmpty(() -> repository.findTournamentIdsByUserSub(sub));
    assertThat(found).containsExactly(other.id());
    assertThat(found).doesNotContain(owned.id());
  }

  private Tournament createTournament(String ownerSub) {
    Tournament tournament =
        Tournament.create(
            "共同管理テスト大会", GameType.GO, CompetitionType.INDIVIDUAL, null, null, 5, ownerSub, NOW);
    tournamentRepository.save(tournament);
    return tournament;
  }

  private static String uniqueSub() {
    return "sub-" + UUID.randomUUID();
  }
}
