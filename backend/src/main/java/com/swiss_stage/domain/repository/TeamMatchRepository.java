package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamMatchId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface TeamMatchRepository {

  Optional<TeamMatch> findById(TournamentId tournamentId, TeamMatchId id);

  List<TeamMatch> findAllByTournamentId(TournamentId tournamentId);

  List<TeamMatch> findByRound(TournamentId tournamentId, int roundNumber);

  void save(TournamentId tournamentId, TeamMatch match);

  void saveAll(TournamentId tournamentId, List<TeamMatch> matches);

  /**
   * 対局を保存する。同じ大会のラウンド(SK=ROUND#nn)がCONFIRMEDへ変わっていないことを同一トランザクションで
   * 検証する(14_tournament_collaboration.md §4.9。個人戦・団体戦は同じRoundを参照するためRND-AC-015と
   * 同じ対策が効く)。対局側の競合は{@link com.swiss_stage.domain.OptimisticLockException}、ラウンド確定との 競合は{@link
   * com.swiss_stage.domain.RoundConfirmedException}を送出する。
   */
  void saveIfRoundNotConfirmed(TournamentId tournamentId, TeamMatch match, int roundNumber);
}
