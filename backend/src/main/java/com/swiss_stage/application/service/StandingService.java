package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.GroupDto;
import com.swiss_stage.application.dto.GroupStandingsDto;
import com.swiss_stage.application.dto.StandingDto;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.MatchRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.service.StandingCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 順位表。保存せず全対局結果から都度計算する(05_swiss_pairing_algorithm.md §3) */
@Service
public class StandingService {

    private final ParticipantRepository participantRepository;
    private final MatchRepository matchRepository;
    private final GroupRepository groupRepository;
    private final TournamentAccessSupport access;
    private final StandingCalculator standingCalculator = new StandingCalculator();

    public StandingService(
            ParticipantRepository participantRepository,
            MatchRepository matchRepository,
            GroupRepository groupRepository,
            TournamentAccessSupport access) {
        this.participantRepository = participantRepository;
        this.matchRepository = matchRepository;
        this.groupRepository = groupRepository;
        this.access = access;
    }

    public List<GroupStandingsDto> standings(TournamentId tournamentId, String ownerSub) {
        access.loadOwned(tournamentId, ownerSub);
        return assembleStandings(tournamentId);
    }

    /**
     * 順位表の組み立て(認可済みの呼び出し元専用。共有ページからも使う)。
     * グループごとに独立計算する(全大会が1つ以上のグループを持つ。05 §2.4/§3.3)。
     */
    List<GroupStandingsDto> assembleStandings(TournamentId tournamentId) {
        List<Participant> participants = participantRepository.findAllByTournamentId(tournamentId);
        List<Match> matches = matchRepository.findAllByTournamentId(tournamentId);
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        List<GroupStandingsDto> result = new ArrayList<>();
        for (Group group : groups) {
            List<Participant> groupParticipants = participants.stream()
                    .filter(p -> group.id().equals(p.groupId()))
                    .toList();
            List<Match> groupMatches = matches.stream()
                    .filter(m -> group.id().equals(m.groupId()))
                    .toList();
            result.add(new GroupStandingsDto(
                    GroupDto.from(group), calculate(groupParticipants, groupMatches)));
        }
        return result;
    }

    private List<StandingDto> calculate(List<Participant> participants, List<Match> matches) {
        Map<ParticipantId, Participant> byId = participants.stream()
                .collect(Collectors.toMap(Participant::id, Function.identity()));
        return standingCalculator.calculate(participants, matches).stream()
                .map(s -> StandingDto.from(s, byId.get(s.participantId())))
                .toList();
    }
}
