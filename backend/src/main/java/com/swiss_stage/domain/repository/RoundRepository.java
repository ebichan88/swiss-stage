package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Optional;

public interface RoundRepository {

    Optional<Round> findByRoundNumber(TournamentId tournamentId, int roundNumber);

    List<Round> findAllByTournamentId(TournamentId tournamentId);

    /**
     * ラウンドの新規作成。実装は条件付き書き込みで二重生成を防ぎ、
     * 既に存在する場合は {@link com.swiss_stage.domain.DuplicateRoundException} を送出する。
     */
    void create(TournamentId tournamentId, Round round);

    /** 既存ラウンドの状態更新 */
    void save(TournamentId tournamentId, Round round);
}
