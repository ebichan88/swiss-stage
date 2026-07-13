package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.StandingDto;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.MatchRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.service.StandingCalculator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 順位表。保存せず全対局結果から都度計算する(05_swiss_pairing_algorithm.md §3.3) */
@Service
public class StandingService {

    private final ParticipantRepository participantRepository;
    private final MatchRepository matchRepository;
    private final TournamentAccessSupport access;
    private final StandingCalculator standingCalculator = new StandingCalculator();

    public StandingService(
            ParticipantRepository participantRepository,
            MatchRepository matchRepository,
            TournamentAccessSupport access) {
        this.participantRepository = participantRepository;
        this.matchRepository = matchRepository;
        this.access = access;
    }

    public List<StandingDto> standings(TournamentId tournamentId, String ownerSub) {
        access.loadOwned(tournamentId, ownerSub);
        List<Participant> participants = participantRepository.findAllByTournamentId(tournamentId);
        List<Match> matches = matchRepository.findAllByTournamentId(tournamentId);
        Map<ParticipantId, Participant> byId = participants.stream()
                .collect(Collectors.toMap(Participant::id, Function.identity()));
        return standingCalculator.calculate(participants, matches).stream()
                .map(s -> StandingDto.from(s, byId.get(s.participantId())))
                .toList();
    }
}
