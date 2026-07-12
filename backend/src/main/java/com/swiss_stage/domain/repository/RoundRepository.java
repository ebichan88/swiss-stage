package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface RoundRepository {

    Optional<Round> findByRoundNumber(TournamentId tournamentId, int roundNumber);

    List<Round> findAllByTournamentId(TournamentId tournamentId);

    /** 同一ラウンドの二重生成は実装が条件付き書き込みで防ぐ */
    void save(TournamentId tournamentId, Round round);
}
