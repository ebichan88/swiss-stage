package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface MatchRepository {

  Optional<Match> findById(TournamentId tournamentId, MatchId id);

  List<Match> findAllByTournamentId(TournamentId tournamentId);

  List<Match> findByRound(TournamentId tournamentId, int roundNumber);

  void save(TournamentId tournamentId, Match match);

  void saveAll(TournamentId tournamentId, List<Match> matches);

  /**
   * 対局を保存する。同じ大会のラウンド(SK=ROUND#nn)がCONFIRMEDへ変わっていないことを同一トランザクションで
   * 検証する(14_tournament_collaboration.md §4.9)。対局側の競合は{@link
   * com.swiss_stage.domain.OptimisticLockException}、ラウンド確定との競合は{@link
   * com.swiss_stage.domain.RoundConfirmedException}を送出する。
   */
  void saveIfRoundNotConfirmed(TournamentId tournamentId, Match match, int roundNumber);
}
