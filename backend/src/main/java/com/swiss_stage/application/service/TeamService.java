package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.AddTeamMemberRequest;
import com.swiss_stage.application.dto.CreateTeamRequest;
import com.swiss_stage.application.dto.FieldErrorDto;
import com.swiss_stage.application.dto.TeamCsvImportResultDto;
import com.swiss_stage.application.dto.TeamDto;
import com.swiss_stage.application.dto.UpdateTeamMemberRequest;
import com.swiss_stage.application.dto.UpdateTeamRequest;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMember;
import com.swiss_stage.domain.model.TeamMemberId;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.TeamRepository;
import com.swiss_stage.domain.service.TeamRosterValidationService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 団体戦のチーム・メンバー管理(05_swiss_pairing_algorithm.md §5.1)。定義の変更は原則PREPARING中のみ */
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final GroupRepository groupRepository;
    private final TournamentAccessSupport access;
    private final TeamCsvParser csvParser;
    private final SharedViewCache sharedViewCache;
    private final TeamRosterValidationService rosterValidation = new TeamRosterValidationService();

    public TeamService(
            TeamRepository teamRepository,
            GroupRepository groupRepository,
            TournamentAccessSupport access,
            TeamCsvParser csvParser,
            SharedViewCache sharedViewCache) {
        this.teamRepository = teamRepository;
        this.groupRepository = groupRepository;
        this.access = access;
        this.csvParser = csvParser;
        this.sharedViewCache = sharedViewCache;
    }

    public List<TeamDto> list(TournamentId tournamentId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        return teamRepository.findAllByTournamentId(tournamentId).stream()
                .sorted(Comparator.comparingInt(Team::entryOrder))
                .map(TeamDto::from)
                .toList();
    }

    public TeamDto create(TournamentId tournamentId, String ownerSub, CreateTeamRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        requirePreparing(tournament, "チームの追加は大会開始前のみ可能です");
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        GroupId groupId = request.groupId() == null
                ? firstGroup(groups).id()
                : resolveGroup(groups, request.groupId()).id();
        Team team = createTeam(request.name(), nextEntryOrder(tournamentId), groupId);
        teamRepository.save(tournamentId, team);
        sharedViewCache.evict(tournamentId);
        return TeamDto.from(team);
    }

    public TeamCsvImportResultDto importCsv(TournamentId tournamentId, String ownerSub, byte[] csv) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        requirePreparing(tournament, "CSVインポートは大会開始前のみ可能です");
        List<TeamCsvParser.Row> rows = csvParser.parse(csv, tournament.teamSize());
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        Map<String, GroupId> groupsByName = groups.stream()
                .collect(Collectors.toMap(Group::name, Group::id));
        validateGroupNames(rows, groupsByName);

        int entryOrder = nextEntryOrder(tournamentId);
        GroupId defaultGroupId = firstGroup(groups).id();
        List<Team> teams = new ArrayList<>();
        for (List<TeamCsvParser.Row> teamRows : csvParser.groupByTeam(rows)) {
            TeamCsvParser.Row first = teamRows.getFirst();
            GroupId groupId = first.groupName() == null
                    ? defaultGroupId : groupsByName.get(first.groupName());
            Team team = createTeam(first.teamName(), entryOrder++, groupId);
            for (TeamCsvParser.Row row : teamRows) {
                team = team.withMember(TeamMember.create(row.memberName(), row.rank(), row.boardPosition()));
            }
            validateReserveCount(team, tournament.teamSize());
            teams.add(team);
        }
        teamRepository.saveAll(tournamentId, teams);
        sharedViewCache.evict(tournamentId);
        return new TeamCsvImportResultDto(teams.size(), teams.stream().map(TeamDto::from).toList());
    }

    public TeamDto update(
            TournamentId tournamentId, TeamId teamId, String ownerSub, UpdateTeamRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        Team team = load(tournamentId, teamId);

        GroupId groupId = team.groupId();
        if (request.groupId() != null) {
            requirePreparing(tournament, "グループ割当の変更は大会開始前のみ可能です");
            groupId = resolveGroup(groupRepository.findAllByTournamentId(tournamentId), request.groupId()).id();
        }
        Team updated = team;
        if (request.name() != null) {
            updated = updated.rename(request.name());
        }
        if (!groupId.equals(team.groupId())) {
            updated = updated.withGroup(groupId);
        }
        if (request.status() != null) {
            updated = new Team(
                    updated.id(), updated.name(), updated.entryOrder(), request.status(),
                    updated.groupId(), updated.members());
        }
        teamRepository.save(tournamentId, updated);
        sharedViewCache.evict(tournamentId);
        return TeamDto.from(updated);
    }

    public void delete(TournamentId tournamentId, TeamId teamId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        requirePreparing(tournament, "チームの削除は大会開始前のみ可能です(開始後は棄権にしてください)");
        load(tournamentId, teamId);
        teamRepository.delete(tournamentId, teamId);
        sharedViewCache.evict(tournamentId);
    }

    public TeamDto addMember(
            TournamentId tournamentId, TeamId teamId, String ownerSub, AddTeamMemberRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        requirePreparing(tournament, "メンバーの追加は大会開始前のみ可能です");
        validateBoardPositionRange(request.boardPosition(), tournament.teamSize());
        Team team = load(tournamentId, teamId);
        Team updated = withDomainErrorsAsValidation(() ->
                team.withMember(TeamMember.create(request.name(), request.rank(), request.boardPosition())));
        validateReserveCount(updated, tournament.teamSize());
        teamRepository.save(tournamentId, updated);
        sharedViewCache.evict(tournamentId);
        return TeamDto.from(updated);
    }

    public TeamDto updateMember(
            TournamentId tournamentId, TeamId teamId, TeamMemberId memberId, String ownerSub,
            UpdateTeamMemberRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        requirePreparing(tournament, "メンバーの変更は大会開始前のみ可能です");
        boolean clearRank = Boolean.TRUE.equals(request.clearRank());
        if (clearRank && request.rank() != null) {
            throw new ValidationException("rank と clearRank は同時に指定できません");
        }
        boolean clearPosition = Boolean.TRUE.equals(request.clearBoardPosition());
        if (clearPosition && request.boardPosition() != null) {
            throw new ValidationException("boardPosition と clearBoardPosition は同時に指定できません");
        }
        Team team = load(tournamentId, teamId);
        TeamMember member = team.members().stream()
                .filter(m -> m.id().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_MEMBER_NOT_FOUND));
        Integer boardPosition = clearPosition ? null : request.boardPosition() != null
                ? request.boardPosition() : member.boardPosition();
        validateBoardPositionRange(boardPosition, tournament.teamSize());
        TeamMember replacement = new TeamMember(
                member.id(),
                request.name() != null ? request.name() : member.name(),
                clearRank ? null : request.rank() != null ? request.rank() : member.rank(),
                boardPosition);
        Team updated = withDomainErrorsAsValidation(() -> team.withReplacedMember(memberId, replacement));
        validateReserveCount(updated, tournament.teamSize());
        teamRepository.save(tournamentId, updated);
        sharedViewCache.evict(tournamentId);
        return TeamDto.from(updated);
    }

    public TeamDto deleteMember(
            TournamentId tournamentId, TeamId teamId, TeamMemberId memberId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        requirePreparing(tournament, "メンバーの削除は大会開始前のみ可能です");
        Team team = load(tournamentId, teamId);
        boolean exists = team.members().stream().anyMatch(m -> m.id().equals(memberId));
        if (!exists) {
            throw new NotFoundException(ErrorCode.TEAM_MEMBER_NOT_FOUND);
        }
        Team updated = team.withoutMember(memberId);
        teamRepository.save(tournamentId, updated);
        sharedViewCache.evict(tournamentId);
        return TeamDto.from(updated);
    }

    private Team load(TournamentId tournamentId, TeamId teamId) {
        return teamRepository.findById(tournamentId, teamId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_NOT_FOUND));
    }

    private static Group resolveGroup(List<Group> groups, String groupIdValue) {
        return groups.stream()
                .filter(g -> g.id().value().equals(groupIdValue))
                .findFirst()
                .orElseThrow(() -> new ValidationException("指定されたグループが存在しません"));
    }

    private static Group firstGroup(List<Group> groups) {
        if (groups.isEmpty()) {
            throw new InvalidStateException("大会にグループが存在しません");
        }
        return groups.getFirst();
    }

    private static void validateGroupNames(
            List<TeamCsvParser.Row> rows, Map<String, GroupId> groupsByName) {
        List<FieldErrorDto> errors = rows.stream()
                .filter(row -> row.groupName() != null && !groupsByName.containsKey(row.groupName()))
                .map(row -> new FieldErrorDto(
                        row.lineNumber() + "行目",
                        "グループ「" + row.groupName() + "」が定義されていません。先にグループを作成してください"))
                .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException(
                    ErrorCode.CSV_INVALID_FORMAT, "CSVの内容に誤りがあります", errors);
        }
    }

    private static void validateBoardPositionRange(Integer boardPosition, int teamSize) {
        if (boardPosition != null && boardPosition > teamSize) {
            throw new ValidationException("ボード位置は" + teamSize + "以下である必要があります");
        }
    }

    private void validateReserveCount(Team team, int teamSize) {
        try {
            rosterValidation.validateReserveCount(team, teamSize);
        } catch (DomainException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    private static Team createTeam(String name, int entryOrder, GroupId groupId) {
        try {
            return Team.create(name, entryOrder, groupId);
        } catch (DomainException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    private static Team withDomainErrorsAsValidation(Supplier<Team> action) {
        try {
            return action.get();
        } catch (DomainException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    private int nextEntryOrder(TournamentId tournamentId) {
        return teamRepository.findAllByTournamentId(tournamentId).stream()
                .mapToInt(Team::entryOrder)
                .max()
                .orElse(0) + 1;
    }

    private static void requireTeamCompetition(Tournament tournament) {
        if (tournament.competitionType() != CompetitionType.TEAM) {
            throw new InvalidStateException("この操作は団体戦の大会でのみ可能です");
        }
    }

    private static void requirePreparing(Tournament tournament, String message) {
        if (tournament.status() != TournamentStatus.PREPARING) {
            throw new InvalidStateException(message);
        }
    }
}
