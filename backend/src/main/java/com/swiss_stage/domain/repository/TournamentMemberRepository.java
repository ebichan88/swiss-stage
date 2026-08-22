package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.model.TournamentMemberId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TournamentMemberRepository {

  /** 大会の共同管理者一覧(AP11)。設定画面の一覧表示・memberId解決に使う(最大9件) */
  List<TournamentMember> findByTournamentId(TournamentId tournamentId);

  /** 特定ユーザーがその大会の共同管理者かどうか(認可判定に使う) */
  Optional<TournamentMember> findBySub(TournamentId tournamentId, String sub);

  /** memberId(APIパスの識別子)から共同管理者を解決する。取り消し(DELETE)の404判定に使う */
  Optional<TournamentMember> findByMemberId(TournamentId tournamentId, TournamentMemberId memberId);

  /** ユーザーが共同管理している大会のID一覧(AP12)。大会一覧のマージに使う */
  List<TournamentId> findTournamentIdsByUserSub(String sub);

  /** 共同管理者を追加する。GSI1SKには大会自身の作成日時を入れ、所有大会と同じ並び順に揃える (14_tournament_collaboration.md §4.3)。 */
  void save(TournamentId tournamentId, TournamentMember member, Instant tournamentCreatedAt);

  void delete(TournamentId tournamentId, TournamentMemberId memberId);
}
