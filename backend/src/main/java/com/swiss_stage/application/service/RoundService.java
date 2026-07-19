package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.GeneratedRoundDto;
import com.swiss_stage.application.dto.InputResultRequest;
import com.swiss_stage.application.dto.MatchDto;
import com.swiss_stage.application.dto.RoundDto;
import com.swiss_stage.application.exception.ConflictException;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.PairingException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.DuplicateRoundException;
import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.MatchResult;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.ResultInputBy;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.RoundStatus;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.MatchRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.repository.RoundRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import com.swiss_stage.domain.service.PairingOptions;
import com.swiss_stage.domain.service.PairingRelaxation;
import com.swiss_stage.domain.service.PairingResult;
import com.swiss_stage.domain.service.SwissPairingService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoundService {

    private final TournamentRepository tournamentRepository;
    private final ParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;
    private final GroupRepository groupRepository;
    private final TournamentAccessSupport access;
    private final SharedViewCache sharedViewCache;
    private final SwissPairingService pairingService = new SwissPairingService();
    private final Clock clock;

    public RoundService(
            TournamentRepository tournamentRepository,
            ParticipantRepository participantRepository,
            RoundRepository roundRepository,
            MatchRepository matchRepository,
            GroupRepository groupRepository,
            TournamentAccessSupport access,
            SharedViewCache sharedViewCache,
            Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
        this.roundRepository = roundRepository;
        this.matchRepository = matchRepository;
        this.groupRepository = groupRepository;
        this.access = access;
        this.sharedViewCache = sharedViewCache;
        this.clock = clock;
    }

    public List<RoundDto> list(TournamentId tournamentId, String ownerSub) {
        access.loadOwned(tournamentId, ownerSub);
        return assembleRounds(tournamentId);
    }

    /** ラウンド一覧の組み立て(認可済みの呼び出し元専用。共有ページからも使う) */
    List<RoundDto> assembleRounds(TournamentId tournamentId) {
        Map<ParticipantId, Participant> participants = participantMap(tournamentId);
        Map<GroupId, Group> groups = groupMap(tournamentId);
        Map<Integer, List<Match>> matchesByRound = matchRepository
                .findAllByTournamentId(tournamentId).stream()
                .collect(Collectors.groupingBy(Match::roundNumber));
        return roundRepository.findAllByTournamentId(tournamentId).stream()
                .sorted(Comparator.comparingInt(Round::roundNumber))
                .map(round -> toRoundDto(
                        round, matchesByRound.getOrDefault(round.roundNumber(), List.of()),
                        participants, groups))
                .toList();
    }

    /**
     * 次ラウンドの組み合わせ生成(05_swiss_pairing_algorithm.md §2)。
     * ラウンドは条件付き書き込みで作成してから対局を保存し、二重生成を防ぐ。
     */
    public GeneratedRoundDto generateNextRound(TournamentId tournamentId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
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

        List<Participant> participants = participantRepository.findAllByTournamentId(tournamentId);
        List<Match> previousMatches = matchRepository.findAllByTournamentId(tournamentId);
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        GeneratedMatches generated =
                generateGrouped(groups, participants, previousMatches, nextRoundNumber);

        Round round = Round.pairing(nextRoundNumber);
        try {
            roundRepository.create(tournamentId, round);
        } catch (DuplicateRoundException e) {
            throw new ConflictException(ErrorCode.ROUND_ALREADY_EXISTS);
        }
        matchRepository.saveAll(tournamentId, generated.matches());
        Round playing = round.startPlaying();
        roundRepository.save(tournamentId, playing);
        tournamentRepository.save(tournament.advanceRound().touched(Instant.now(clock)));
        sharedViewCache.evict(tournamentId);

        List<Match> saved = matchRepository.findByRound(tournamentId, nextRoundNumber);
        return new GeneratedRoundDto(
                toRoundDto(playing, saved, participantMap(tournamentId), groupMap(tournamentId)),
                generated.relaxations());
    }

    private record GeneratedMatches(List<Match> matches, List<String> relaxations) {}

    /**
     * ラウンドの一括生成(05_swiss_pairing_algorithm.md §2.4)。
     * グループごとに独立ペアリングし、relaxations は全グループの和集合を返す。
     */
    private GeneratedMatches generateGrouped(
            List<Group> groups, List<Participant> participants,
            List<Match> previousMatches, int roundNumber) {
        List<Match> matches = new ArrayList<>();
        TreeSet<String> relaxations = new TreeSet<>();
        boolean single = groups.size() == 1;
        for (Group group : groups) {
            List<Participant> members = participants.stream()
                    .filter(p -> group.id().equals(p.groupId()))
                    .toList();
            List<Match> groupPrevious = previousMatches.stream()
                    .filter(m -> group.id().equals(m.groupId()))
                    .toList();
            List<Participant> active = members.stream().filter(Participant::isActive).toList();
            if (active.isEmpty()) {
                continue;
            }
            if (active.size() == 1) {
                // 棄権で1名だけ残ったグループはBYE直付与(BYE済ならBYE_REPEATを記録して重複付与)
                Participant only = active.getFirst();
                boolean hadBye = groupPrevious.stream()
                        .anyMatch(m -> m.isBye() && m.player1Id().equals(only.id()));
                if (hadBye) {
                    relaxations.add(PairingRelaxation.BYE_REPEAT.name());
                }
                matches.add(Match.byeOf(roundNumber, 1, only.id(), group.id()));
                continue;
            }
            // グループが1つだけの大会はエラー文言にグループ名を出さない(表示上グループ概念を見せないため)
            PairingResult pairing = pair(members, groupPrevious, roundNumber, single ? null : group);
            matches.addAll(toMatches(pairing, roundNumber, group.id()));
            pairing.relaxations().forEach(r -> relaxations.add(r.name()));
        }
        if (matches.isEmpty()) {
            throw new PairingException("組み合わせを生成できる参加者がいません");
        }
        return new GeneratedMatches(matches, List.copyOf(relaxations));
    }

    private PairingResult pair(
            List<Participant> participants, List<Match> previousMatches,
            int roundNumber, Group labeledGroup) {
        try {
            return pairingService.pair(
                    participants, previousMatches, roundNumber, PairingOptions.defaults());
        } catch (DomainException e) {
            String prefix = labeledGroup == null ? "" : "グループ「" + labeledGroup.name() + "」: ";
            throw new PairingException(prefix + e.getMessage());
        }
    }

    /** ラウンド確定。全対局の結果入力が完了している必要がある */
    public RoundDto confirm(TournamentId tournamentId, int roundNumber, String ownerSub) {
        access.loadOwned(tournamentId, ownerSub);
        Round round = roundRepository.findByRoundNumber(tournamentId, roundNumber)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROUND_NOT_FOUND));
        List<Match> matches = matchRepository.findByRound(tournamentId, roundNumber);
        long undecided = matches.stream().filter(m -> !m.result().isDecided()).count();
        if (undecided > 0) {
            throw new InvalidStateException(
                    "結果未入力の対局が" + undecided + "件あります。全て入力してから確定してください");
        }
        Round confirmed;
        try {
            confirmed = round.confirm();
        } catch (DomainException e) {
            throw new InvalidStateException(e.getMessage());
        }
        roundRepository.save(tournamentId, confirmed);
        sharedViewCache.evict(tournamentId);
        return toRoundDto(confirmed, matches, participantMap(tournamentId), groupMap(tournamentId));
    }

    /** 対局結果入力(PUT・べき等)。確定済みラウンドの対局は変更不可 */
    public MatchDto inputResult(
            TournamentId tournamentId, MatchId matchId, String ownerSub, InputResultRequest request) {
        access.loadOwned(tournamentId, ownerSub);
        return applyResult(tournamentId, matchId, request, ResultInputBy.OWNER);
    }

    /** 結果入力の本体(認可済みの呼び出し元専用。共有トークン経由はSharedServiceから呼ぶ) */
    MatchDto applyResult(
            TournamentId tournamentId, MatchId matchId, InputResultRequest request,
            ResultInputBy inputBy) {
        if (request.result() == MatchResult.NONE || request.result() == MatchResult.BYE) {
            throw new ValidationException("結果には勝敗・引き分け・両者敗けのいずれかを指定してください");
        }
        Match match = matchRepository.findById(tournamentId, matchId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MATCH_NOT_FOUND));
        Round round = roundRepository.findByRoundNumber(tournamentId, match.roundNumber())
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROUND_NOT_FOUND));
        if (round.status() == RoundStatus.CONFIRMED) {
            throw new InvalidStateException("確定済みラウンドの結果は変更できません");
        }
        if (match.version() != request.version()) {
            throw new ConflictException();
        }
        Match updated;
        try {
            updated = match.withResult(request.result(), inputBy);
        } catch (DomainException e) {
            throw new ValidationException(e.getMessage());
        }
        try {
            matchRepository.save(tournamentId, updated);
        } catch (OptimisticLockException e) {
            throw new ConflictException();
        }
        sharedViewCache.evict(tournamentId);
        Match saved = matchRepository.findById(tournamentId, matchId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MATCH_NOT_FOUND));
        return MatchDto.from(saved, participantMap(tournamentId), groupMap(tournamentId));
    }

    /** PairingResultをMatchへ変換する。卓番号はペア順に1始まり(グループ内採番)、BYEは末尾の卓 */
    private static List<Match> toMatches(PairingResult pairing, int roundNumber, GroupId groupId) {
        List<Match> matches = new ArrayList<>();
        int tableNumber = 1;
        for (PairingResult.Pair pair : pairing.pairs()) {
            matches.add(Match.pairOf(
                    roundNumber, tableNumber++, pair.player1Id(), pair.player2Id(), groupId));
        }
        if (pairing.hasBye()) {
            matches.add(Match.byeOf(roundNumber, tableNumber, pairing.byeParticipantId(), groupId));
        }
        return matches;
    }

    private Map<ParticipantId, Participant> participantMap(TournamentId tournamentId) {
        return participantRepository.findAllByTournamentId(tournamentId).stream()
                .collect(Collectors.toMap(Participant::id, Function.identity()));
    }

    private Map<GroupId, Group> groupMap(TournamentId tournamentId) {
        return groupRepository.findAllByTournamentId(tournamentId).stream()
                .collect(Collectors.toMap(Group::id, Function.identity()));
    }

    private static RoundDto toRoundDto(
            Round round, List<Match> matches, Map<ParticipantId, Participant> participants,
            Map<GroupId, Group> groups) {
        List<MatchDto> matchDtos = matches.stream()
                .sorted(Comparator.comparing((Match m) -> m.groupId().value())
                        .thenComparingInt(Match::tableNumber))
                .map(m -> MatchDto.from(m, participants, groups))
                .toList();
        return new RoundDto(round.roundNumber(), round.status(), matchDtos);
    }
}
