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
}
