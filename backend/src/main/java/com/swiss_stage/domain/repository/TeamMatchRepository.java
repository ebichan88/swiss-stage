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
}
