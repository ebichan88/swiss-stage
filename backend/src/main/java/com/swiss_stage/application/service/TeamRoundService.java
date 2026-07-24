package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.GeneratedTeamRoundDto;
import com.swiss_stage.application.dto.InputTeamMatchResultRequest;
import com.swiss_stage.application.dto.TeamMatchDto;
import com.swiss_stage.application.dto.TeamRoundDto;
import com.swiss_stage.application.exception.ConflictException;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.PairingException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.DuplicateRoundException;
import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.RoundStatus;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamMatchId;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.RoundRepository;
import com.swiss_stage.domain.repository.TeamMatchRepository;
import com.swiss_stage.domain.repository.TeamRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import com.swiss_stage.domain.service.TeamSwissPairingService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 団体戦のラウンド生成・結果入力(05_swiss_pairing_algorithm.md §5)。RoundはRoundServiceと共有する */
@Service
public class TeamRoundService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final TeamMatchRepository teamMatchRepository;
    private final GroupRepository groupRepository;
    private final TournamentAccessSupport access;
    private final SharedViewCache sharedViewCache;
    private final TeamSwissPairingService pairingService = new TeamSwissPairingService();
    private final Clock clock;

    public TeamRoundService(
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository,
            RoundRepository roundRepository,
            TeamMatchRepository teamMatchRepository,
            GroupRepository groupRepository,
            TournamentAccessSupport access,
            SharedViewCache sharedViewCache,
            Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.roundRepository = roundRepository;
        this.teamMatchRepository = teamMatchRepository;
        this.groupRepository = groupRepository;
        this.access = access;
        this.sharedViewCache = sharedViewCache;
        this.clock = clock;
    }

    public List<TeamRoundDto> list(TournamentId tournamentId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        return assembleRounds(tournamentId);
    }

    /** ラウンド一覧の組み立て(認可済みの呼び出し元専用。共有ページからも使う) */
    List<TeamRoundDto> assembleRounds(TournamentId tournamentId) {
        Map<TeamId, Team> teams = teamMap(tournamentId);
        Map<GroupId, Group> groups = groupMap(tournamentId);
        Map<Integer, List<TeamMatch>> matchesByRound = teamMatchRepository
                .findAllByTournamentId(tournamentId).stream()
                .collect(Collectors.groupingBy(TeamMatch::roundNumber));
        return roundRepository.findAllByTournamentId(tournamentId).stream()
                .sorted(Comparator.comparingInt(Round::roundNumber))
                .map(round -> toRoundDto(
                        round, matchesByRound.getOrDefault(round.roundNumber(), List.of()),
                        teams, groups))
                .toList();
    }

    public GeneratedTeamRoundDto generateNextRound(TournamentId tournamentId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        if (tournament.status() != TournamentStatus.IN_PROGRESS) {
            throw new InvalidStateException("組み合わせ生成は開催中の大会のみ可能です");
        }
        int nextRoundNumber = tournament.currentRound() + 1;
        if (nextRoundNumber > tournament.totalRounds()) {
            throw new InvalidStateException("最終ラウンドまで終了しています");
        }
        if (tournament.currentRound() >= 1) {
            Round current = roundRepository
                    .findByRoundNumber(tournamentId, tournament.currentRound())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.ROUND_NOT_FOUND));
            if (!current.isConfirmed()) {
                throw new InvalidStateException(
                        "ラウンド" + current.roundNumber() + "を確定してから次のラウンドを生成してください");
            }
        }

        List<Team> teams = teamRepository.findAllByTournamentId(tournamentId);
        List<TeamMatch> previousMatches = teamMatchRepository.findAllByTournamentId(tournamentId);
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        GeneratedMatches generated = generateGrouped(
                groups, teams, previousMatches, nextRoundNumber, tournament.teamSize());

        Round round = Round.pairing(nextRoundNumber);
        try {
            roundRepository.create(tournamentId, round);
        } catch (DuplicateRoundException e) {
            throw new ConflictException(ErrorCode.ROUND_ALREADY_EXISTS);
        }
        teamMatchRepository.saveAll(tournamentId, generated.matches());
        Round playing = round.startPlaying();
        roundRepository.save(tournamentId, playing);
        tournamentRepository.save(tournament.advanceRound().touched(Instant.now(clock)));
        sharedViewCache.evict(tournamentId);

        List<TeamMatch> saved = teamMatchRepository.findByRound(tournamentId, nextRoundNumber);
        return new GeneratedTeamRoundDto(
                toRoundDto(playing, saved, teamMap(tournamentId), groupMap(tournamentId)),
                generated.relaxations());
    }

    private record GeneratedMatches(List<TeamMatch> matches, List<String> relaxations) {}

    private GeneratedMatches generateGrouped(
            List<Group> groups, List<Team> teams, List<TeamMatch> previousMatches,
            int roundNumber, int teamSize) {
        List<TeamMatch> matches = new ArrayList<>();
        TreeSet<String> relaxations = new TreeSet<>();
        boolean single = groups.size() == 1;
        for (Group group : groups) {
            List<Team> members = teams.stream()
                    .filter(t -> group.id().equals(t.groupId()))
                    .toList();
            List<TeamMatch> groupPrevious = previousMatches.stream()
                    .filter(m -> group.id().equals(m.groupId()))
                    .toList();
            List<Team> active = members.stream().filter(Team::isActive).toList();
            if (active.isEmpty()) {
                continue;
            }
            if (active.size() == 1) {
                Team only = active.getFirst();
                boolean hadBye = groupPrevious.stream()
                        .anyMatch(m -> m.isBye() && m.team1Id().equals(only.id()));
                if (hadBye) {
                    relaxations.add(com.swiss_stage.domain.service.PairingRelaxation.BYE_REPEAT.name());
                }
                matches.add(TeamMatch.byeOf(roundNumber, 1, only.id(), group.id()));
                continue;
            }
            TeamSwissPairingService.PairingResult pairing =
                    pair(members, groupPrevious, roundNumber, single ? null : group);
            matches.addAll(toMatches(pairing, roundNumber, group.id(), teamSize));
            pairing.relaxations().forEach(r -> relaxations.add(r.name()));
        }
        if (matches.isEmpty()) {
            throw new PairingException("組み合わせを生成できるチームがいません");
        }
        return new GeneratedMatches(matches, List.copyOf(relaxations));
    }

    private TeamSwissPairingService.PairingResult pair(
            List<Team> teams, List<TeamMatch> previousMatches, int roundNumber, Group labeledGroup) {
        try {
            return pairingService.pair(teams, previousMatches, roundNumber);
        } catch (DomainException e) {
            String prefix = labeledGroup == null ? "" : "グループ「" + labeledGroup.name() + "」: ";
            throw new PairingException(prefix + e.getMessage());
        }
    }

    public TeamRoundDto confirm(TournamentId tournamentId, int roundNumber, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        Round round = roundRepository.findByRoundNumber(tournamentId, roundNumber)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROUND_NOT_FOUND));
        List<TeamMatch> matches = teamMatchRepository.findByRound(tournamentId, roundNumber);
        long untouched = matches.stream().filter(TeamMatch::isUntouched).count();
        if (untouched > 0) {
            throw new InvalidStateException(
                    "結果未入力の対局が" + untouched + "件あります。全て入力してから確定してください");
        }
        Round confirmed;
        try {
            confirmed = round.confirm();
        } catch (DomainException e) {
            throw new InvalidStateException(e.getMessage());
        }
        roundRepository.save(tournamentId, confirmed);
        sharedViewCache.evict(tournamentId);
        return toRoundDto(confirmed, matches, teamMap(tournamentId), groupMap(tournamentId));
    }

    /** 対局結果入力(PUT・べき等)。確定済みラウンドの対局は変更不可 */
    public TeamMatchDto inputResult(
            TournamentId tournamentId, TeamMatchId matchId, String ownerSub,
            InputTeamMatchResultRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requireTeamCompetition(tournament);
        return applyResult(tournamentId, matchId, request);
    }

    /** 結果入力の本体(認可済みの呼び出し元専用。共有トークン経由はSharedServiceから呼ぶ) */
    TeamMatchDto applyResult(
            TournamentId tournamentId, TeamMatchId matchId, InputTeamMatchResultRequest request) {
        if (request.boardResults().stream().anyMatch(r -> r == MatchResult.BYE)) {
            throw new ValidationException("ボード結果にBYEは指定できません");
        }
        return editMatch(tournamentId, matchId, request.version(),
                match -> match.withBoardResults(request.boardResults()));
    }

    private TeamMatchDto editMatch(
            TournamentId tournamentId, TeamMatchId matchId, long expectedVersion,
            UnaryOperator<TeamMatch> edit) {
        TeamMatch match = teamMatchRepository.findById(tournamentId, matchId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_MATCH_NOT_FOUND));
        Round round = roundRepository.findByRoundNumber(tournamentId, match.roundNumber())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROUND_NOT_FOUND));
        if (round.status() == RoundStatus.CONFIRMED) {
            throw new InvalidStateException("確定済みラウンドの結果は変更できません");
        }
        if (match.version() != expectedVersion) {
            throw new ConflictException();
        }
        TeamMatch updated;
        try {
            updated = edit.apply(match);
        } catch (DomainException e) {
            throw new ValidationException(e.getMessage());
        }
        try {
            teamMatchRepository.save(tournamentId, updated);
        } catch (OptimisticLockException e) {
            throw new ConflictException();
        }
        sharedViewCache.evict(tournamentId);
        TeamMatch saved = teamMatchRepository.findById(tournamentId, matchId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_MATCH_NOT_FOUND));
        return TeamMatchDto.from(saved, teamMap(tournamentId), groupMap(tournamentId));
    }

    private static List<TeamMatch> toMatches(
            TeamSwissPairingService.PairingResult pairing, int roundNumber, GroupId groupId,
            int teamSize) {
        List<TeamMatch> matches = new ArrayList<>();
        int tableNumber = 1;
        for (TeamSwissPairingService.PairingResult.Pair pair : pairing.pairs()) {
            matches.add(TeamMatch.pairOf(
                    roundNumber, tableNumber++, pair.firstId(), pair.secondId(), teamSize, groupId));
        }
        if (pairing.hasBye()) {
            matches.add(TeamMatch.byeOf(roundNumber, tableNumber, pairing.byeId(), groupId));
        }
        return matches;
    }

    private Map<TeamId, Team> teamMap(TournamentId tournamentId) {
        return teamRepository.findAllByTournamentId(tournamentId).stream()
                .collect(Collectors.toMap(Team::id, Function.identity()));
    }

    private Map<GroupId, Group> groupMap(TournamentId tournamentId) {
        return groupRepository.findAllByTournamentId(tournamentId).stream()
                .collect(Collectors.toMap(Group::id, Function.identity()));
    }

    private static TeamRoundDto toRoundDto(
            Round round, List<TeamMatch> matches, Map<TeamId, Team> teams,
            Map<GroupId, Group> groups) {
        List<TeamMatchDto> matchDtos = matches.stream()
                .sorted(Comparator.comparing((TeamMatch m) -> m.groupId().value())
                        .thenComparingInt(TeamMatch::tableNumber))
                .map(m -> TeamMatchDto.from(m, teams, groups))
                .toList();
        return new TeamRoundDto(round.roundNumber(), round.status(), matchDtos);
    }

    private static void requireTeamCompetition(Tournament tournament) {
        if (tournament.competitionType() != CompetitionType.TEAM) {
            throw new InvalidStateException("この操作は団体戦の大会でのみ可能です");
        }
    }
}
