package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentInvite;
import com.swiss_stage.domain.model.TournamentMember;
import java.time.Instant;
import java.util.Optional;

public interface TournamentInviteRepository {

  /** 大会の現在の招待(AP14)。1大会につき常に0または1件 */
  Optional<TournamentInvite> findByTournamentId(TournamentId tournamentId);

  /** 招待トークンから招待とその大会を特定する(AP13・GSI2) */
  Optional<TournamentInvite> findByToken(String token);

  /**
   * 発行・再発行。{@code version}楽観ロックによる条件付き書き込みで、呼び出し元が読み込んだ状態
   * からの差分が無ければそのまま上書きする(OWNERの単独操作のため実質常に競合しない)。
   */
  void save(TournamentInvite invite);

  /** 招待の失効。未発行でも冪等に成功する */
  void delete(TournamentId tournamentId);

  /**
   * 招待の使用済み人数を+1しつつ、共同管理者を同一トランザクションで追加する (14_tournament_collaboration.md
   * §4.4)。招待のversion競合、または既に同じsubの メンバーが存在する場合はfalseを返す(呼び出し元が状態を再読込し、既にメンバーになって
   * いれば成功扱い、そうでなければ再試行する)。
   */
  boolean acceptWithMember(
      TournamentInvite acceptedInvite, TournamentMember member, Instant tournamentCreatedAt);
}
