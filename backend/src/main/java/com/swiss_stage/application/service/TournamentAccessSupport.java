package com.swiss_stage.application.service;

import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.TournamentRepository;
import org.springframework.stereotype.Component;

/**
 * 大会の取得と所有者検証(13_security_design.md §3)。
 * 他人の大会は存在の有無を漏らさないため404を返す。
 */
@Component
public class TournamentAccessSupport {

    private final TournamentRepository tournamentRepository;

    public TournamentAccessSupport(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public Tournament loadOwned(TournamentId id, String ownerSub) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TOURNAMENT_NOT_FOUND));
        if (!tournament.isOwnedBy(ownerSub)) {
            throw new NotFoundException(ErrorCode.TOURNAMENT_NOT_FOUND);
        }
        return tournament;
    }
}
