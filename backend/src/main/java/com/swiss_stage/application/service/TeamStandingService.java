package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.GroupDto;
import com.swiss_stage.application.dto.GroupTeamStandingsDto;
import com.swiss_stage.application.dto.TeamStandingDto;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.TeamMatchRepository;
import com.swiss_stage.domain.repository.TeamRepository;
import com.swiss_stage.domain.service.TeamStandingCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 団体戦順位表。保存せず全対局結果から都度計算する(05_swiss_pairing_algorithm.md §5.5) */
@Service
public class TeamStandingService {

    private final TeamRepository teamRepository;
    private final TeamMatchRepository teamMatchRepository;
    private final GroupRepository groupRepository;
    private final TournamentAccessSupport access;
    private final TeamStandingCalculator standingCalculator = new TeamStandingCalculator();

    public TeamStandingService(
            TeamRepository teamRepository,
            TeamMatchRepository teamMatchRepository,
            GroupRepository groupRepository,
            TournamentAccessSupport access) {
        this.teamRepository = teamRepository;
        this.teamMatchRepository = teamMatchRepository;
        this.groupRepository = groupRepository;
        this.access = access;
    }

    public List<GroupTeamStandingsDto> standings(TournamentId tournamentId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        if (tournament.competitionType() != CompetitionType.TEAM) {
            throw new InvalidStateException("この操作は団体戦の大会でのみ可能です");
        }
        return assembleStandings(tournamentId);
    }

    /** 順位表の組み立て(認可済みの呼び出し元専用。共有ページからも使う) */
    List<GroupTeamStandingsDto> assembleStandings(TournamentId tournamentId) {
        List<Team> teams = teamRepository.findAllByTournamentId(tournamentId);
        List<TeamMatch> matches = teamMatchRepository.findAllByTournamentId(tournamentId);
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        List<GroupTeamStandingsDto> result = new ArrayList<>();
        for (Group group : groups) {
            List<Team> groupTeams = teams.stream()
                    .filter(t -> group.id().equals(t.groupId()))
                    .toList();
            List<TeamMatch> groupMatches = matches.stream()
                    .filter(m -> group.id().equals(m.groupId()))
                    .toList();
            result.add(new GroupTeamStandingsDto(
                    GroupDto.from(group), calculate(groupTeams, groupMatches)));
        }
        return result;
    }

    private List<TeamStandingDto> calculate(List<Team> teams, List<TeamMatch> matches) {
        Map<TeamId, Team> byId = teams.stream().collect(Collectors.toMap(Team::id, Function.identity()));
        return standingCalculator.calculate(teams, matches).stream()
                .map(s -> TeamStandingDto.from(s, byId.get(s.teamId())))
                .toList();
    }
}
