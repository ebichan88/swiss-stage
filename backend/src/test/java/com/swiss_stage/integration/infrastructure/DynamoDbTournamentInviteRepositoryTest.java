package com.swiss_stage.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.GameType;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentInvite;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.repository.TournamentInviteRepository;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DynamoDbTournamentInviteRepositoryTest extends DynamoDbRepositoryTestSupport {

  private static final Instant NOW = Instant.parse("2026-08-16T00:00:00Z");

  @Autowired TournamentInviteRepository repository;
  @Autowired TournamentRepository tournamentRepository;
  @Autowired TournamentMemberRepository memberRepository;

  @Test
  @DisplayName("招待を発行して大会ID・トークンの両方から取得でき、失効すると見つからなくなる(冪等)")
  void 発行と取得と失効() {
    Tournament tournament = createTournament();
    String token = uniqueToken();
    TournamentInvite invite = TournamentInvite.issue(tournament.id(), token, 3, NOW, null);
    repository.save(invite);

    TournamentInvite byTournament = repository.findByTournamentId(tournament.id()).orElseThrow();
    assertThat(byTournament.token()).isEqualTo(token);
    assertThat(byTournament.maxUses()).isEqualTo(3);
    assertThat(byTournament.usedCount()).isZero();
    assertThat(byTournament.version()).isPositive();

    TournamentInvite byToken =
        awaitNonEmpty(() -> repository.findByToken(token).map(List::of).orElse(List.of()))
            .getFirst();
    assertThat(byToken.tournamentId()).isEqualTo(tournament.id());

    repository.delete(tournament.id());
    assertThat(repository.findByTournamentId(tournament.id())).isEmpty();

    // 未発行(=既に削除済み)への失効は冪等に成功する
    repository.delete(tournament.id());
  }

  @Test
  @DisplayName("再発行すると同じアイテムが上書きされ、旧トークンでは見つからなくなる")
  void 再発行で旧トークンが無効になる() {
    Tournament tournament = createTournament();
    String oldToken = uniqueToken();
    repository.save(TournamentInvite.issue(tournament.id(), oldToken, 3, NOW, null));

    TournamentInvite current = repository.findByTournamentId(tournament.id()).orElseThrow();
    String newToken = uniqueToken();
    repository.save(TournamentInvite.issue(tournament.id(), newToken, 5, NOW, current.version()));

    TournamentInvite reissued = repository.findByTournamentId(tournament.id()).orElseThrow();
    assertThat(reissued.token()).isEqualTo(newToken);
    assertThat(reissued.maxUses()).isEqualTo(5);
    assertThat(reissued.usedCount()).isZero();
    assertThat(repository.findByToken(oldToken)).isEmpty();
  }

  @Test
  @DisplayName("古いversionでの保存は楽観ロック競合になる")
  void 楽観ロック() {
    Tournament tournament = createTournament();
    repository.save(TournamentInvite.issue(tournament.id(), uniqueToken(), 3, NOW, null));
    TournamentInvite loaded = repository.findByTournamentId(tournament.id()).orElseThrow();

    repository.save(
        TournamentInvite.issue(tournament.id(), uniqueToken(), 4, NOW, loaded.version()));

    assertThatThrownBy(
            () ->
                repository.save(
                    TournamentInvite.issue(
                        tournament.id(), uniqueToken(), 2, NOW, loaded.version())))
        .isInstanceOf(OptimisticLockException.class);
  }

  @Test
  @DisplayName("acceptWithMemberは招待の使用済み人数を+1しつつ共同管理者を同一トランザクションで追加する")
  void 承諾トランザクション成功() {
    Tournament tournament = createTournament();
    repository.save(TournamentInvite.issue(tournament.id(), uniqueToken(), 3, NOW, null));
    TournamentInvite invite = repository.findByTournamentId(tournament.id()).orElseThrow();

    String sub = uniqueSub();
    TournamentMember member = TournamentMember.create(sub, "承諾太郎", NOW);
    boolean accepted =
        repository.acceptWithMember(invite.accepted(), member, tournament.createdAt());

    assertThat(accepted).isTrue();
    TournamentInvite updated = repository.findByTournamentId(tournament.id()).orElseThrow();
    assertThat(updated.usedCount()).isEqualTo(1);
    assertThat(memberRepository.findBySub(tournament.id(), sub)).isPresent();
  }

  @Test
  @DisplayName("acceptWithMemberは招待のversion競合時、MEMBER作成も含めて全体がロールバックされfalseを返す")
  void 承諾トランザクションのversion競合() {
    Tournament tournament = createTournament();
    repository.save(TournamentInvite.issue(tournament.id(), uniqueToken(), 3, NOW, null));
    TournamentInvite stale = repository.findByTournamentId(tournament.id()).orElseThrow();
    // 割り込みで先に他の操作が招待を更新し、staleのversionを古くする
    repository.save(
        TournamentInvite.issue(tournament.id(), uniqueToken(), 3, NOW, stale.version()));

    String sub = uniqueSub();
    TournamentMember member = TournamentMember.create(sub, "競合太郎", NOW);
    boolean accepted =
        repository.acceptWithMember(stale.accepted(), member, tournament.createdAt());

    assertThat(accepted).isFalse();
    assertThat(memberRepository.findBySub(tournament.id(), sub)).isEmpty();
    assertThat(repository.findByTournamentId(tournament.id()).orElseThrow().usedCount()).isZero();
  }

  @Test
  @DisplayName("acceptWithMemberは同じsubのMEMBERが既に存在する場合falseを返し、招待も更新されない(二重承諾)")
  void 承諾トランザクションのMEMBER重複() {
    Tournament tournament = createTournament();
    repository.save(TournamentInvite.issue(tournament.id(), uniqueToken(), 3, NOW, null));
    TournamentInvite invite = repository.findByTournamentId(tournament.id()).orElseThrow();

    String sub = uniqueSub();
    memberRepository.save(
        tournament.id(), TournamentMember.create(sub, "既存太郎", NOW), tournament.createdAt());

    TournamentMember duplicate = TournamentMember.create(sub, "既存太郎(重複)", NOW);
    boolean accepted =
        repository.acceptWithMember(invite.accepted(), duplicate, tournament.createdAt());

    assertThat(accepted).isFalse();
    assertThat(repository.findByTournamentId(tournament.id()).orElseThrow().usedCount()).isZero();
  }

  private Tournament createTournament() {
    Tournament tournament =
        Tournament.create(
            "招待統合テスト大会", GameType.GO, CompetitionType.INDIVIDUAL, null, null, 5, uniqueSub(), NOW);
    tournamentRepository.save(tournament);
    return tournament;
  }

  private static String uniqueSub() {
    return "sub-" + UUID.randomUUID();
  }

  private static String uniqueToken() {
    return "tok-" + UUID.randomUUID();
  }
}
