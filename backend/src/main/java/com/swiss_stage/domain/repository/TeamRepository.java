package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface TeamRepository {

    Optional<Team> findById(TournamentId tournamentId, TeamId id);

    List<Team> findAllByTournamentId(TournamentId tournamentId);

    void save(TournamentId tournamentId, Team team);

    void saveAll(TournamentId tournamentId, List<Team> teams);

    void delete(TournamentId tournamentId, TeamId id);
}
