package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository {

    Optional<Participant> findById(TournamentId tournamentId, ParticipantId id);

    List<Participant> findAllByTournamentId(TournamentId tournamentId);

    void save(TournamentId tournamentId, Participant participant);

    void saveAll(TournamentId tournamentId, List<Participant> participants);

    void delete(TournamentId tournamentId, ParticipantId id);
}
