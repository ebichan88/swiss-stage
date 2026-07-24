package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.CreateTournamentRequest;
import com.swiss_stage.application.dto.TournamentDto;
import com.swiss_stage.application.dto.UpdateTournamentRequest;
import com.swiss_stage.application.exception.ConflictException;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.CompetitionType;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import com.swiss_stage.domain.service.GroupAssignmentService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

    private static final Set<Integer> VALID_TEAM_SIZES = Set.of(3, 5);

    private final TournamentRepository tournamentRepository;
    private final ParticipantRepository participantRepository;
    private final GroupRepository groupRepository;
    private final TournamentAccessSupport access;
    private final SharedViewCache sharedViewCache;
    private final GroupAssignmentService assignmentService = new GroupAssignmentService();
    private final Clock clock;

    public TournamentService(
            TournamentRepository tournamentRepository,
            ParticipantRepository participantRepository,
            GroupRepository groupRepository,
            TournamentAccessSupport access,
            SharedViewCache sharedViewCache,
            Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
        this.groupRepository = groupRepository;
        this.access = access;
        this.sharedViewCache = sharedViewCache;
        this.clock = clock;
    }

    public List<TournamentDto> list(String ownerSub) {
        return tournamentRepository.findByOwnerSub(ownerSub).stream()
                .map(TournamentDto::from)
                .toList();
    }

    public TournamentDto create(String ownerSub, CreateTournamentRequest request) {
        validateCompetitionType(request);
        Tournament tournament = Tournament.create(
                request.name(), request.gameType(), request.competitionType(), request.teamSize(),
                request.totalRounds(), ownerSub, Instant.now(clock));
        tournamentRepository.save(tournament);
        // 大会は常に1つ以上のグループを持つ(05 §2.4)。デフォルトグループを同時に作成する
        try {
            groupRepository.save(tournament.id(), Group.create(Group.DEFAULT_NAME));
        } catch (RuntimeException e) {
            // グループのない大会を残さない(残ると以後の参加者追加・自動振り分けが失敗し続ける)
            tournamentRepository.delete(tournament.id());
            throw e;
        }
        return reload(tournament.id());
    }

    /**
     * competitionType=TEAM は teamSize(3/5)必須、INDIVIDUALはteamSize指定不可
     * (OpenAPIのCreateTournamentRequest.descriptionが示すクロスフィールド制約)。
     */
    private void validateCompetitionType(CreateTournamentRequest request) {
        if (request.competitionType() == CompetitionType.TEAM
                && (request.teamSize() == null || !VALID_TEAM_SIZES.contains(request.teamSize()))) {
            throw new ValidationException("団体戦のチーム制は3人制または5人制である必要があります");
        }
        if (request.competitionType() == CompetitionType.INDIVIDUAL && request.teamSize() != null) {
            throw new ValidationException("個人戦にチーム制は指定できません");
        }
    }

    public TournamentDto get(TournamentId id, String ownerSub) {
        return TournamentDto.from(access.loadOwned(id, ownerSub));
    }

    public TournamentDto update(TournamentId id, String ownerSub, UpdateTournamentRequest request) {
        Tournament tournament = access.loadOwned(id, ownerSub);
        if (tournament.version() != request.version()) {
            throw new ConflictException();
        }
        if (request.name() != null) {
            tournament = tournament.rename(request.name());
        }
        if (request.visibility() != null) {
            tournament = tournament.withVisibility(request.visibility());
        }
        if (request.resultInputEnabled() != null) {
            tournament = tournament.withResultInputEnabled(request.resultInputEnabled());
        }
        tournamentRepository.save(tournament.touched(Instant.now(clock)));
        sharedViewCache.evict(id);
        return reload(id);
    }

    /**
     * 共有トークンの発行・再発行(13_security_design.md §2)。
     * 上書き保存のため旧トークンは即時無効になる(キャッシュも同時に破棄)。
     */
    public TournamentDto regenerateShareToken(TournamentId id, String ownerSub) {
        Tournament tournament = access.loadOwned(id, ownerSub);
        tournamentRepository.save(
                tournament.withShareToken(ShareTokens.generate()).touched(Instant.now(clock)));
        sharedViewCache.evict(id);
        return reload(id);
    }

    public void delete(TournamentId id, String ownerSub) {
        access.loadOwned(id, ownerSub);
        tournamentRepository.delete(id);
        sharedViewCache.evict(id);
    }

    public TournamentDto start(TournamentId id, String ownerSub) {
        Tournament tournament = access.loadOwned(id, ownerSub);
        List<Participant> participants = participantRepository.findAllByTournamentId(id);
        long activeCount = participants.stream()
                .filter(Participant::isActive)
                .count();
        if (activeCount < 2) {
            throw new InvalidStateException("大会の開始には参加者が2名以上必要です");
        }
        // グループ大会は全ACTIVE参加者の割当済み・各グループ2名以上を検証(05 §2.4)
        try {
            assignmentService.validateForStart(
                    groupRepository.findAllByTournamentId(id), participants);
        } catch (DomainException e) {
            throw new InvalidStateException(e.getMessage());
        }
        tournamentRepository.save(tournament.start().touched(Instant.now(clock)));
        sharedViewCache.evict(id);
        return reload(id);
    }

    public TournamentDto finish(TournamentId id, String ownerSub) {
        Tournament tournament = access.loadOwned(id, ownerSub);
        tournamentRepository.save(tournament.finish().touched(Instant.now(clock)));
        sharedViewCache.evict(id);
        return reload(id);
    }

    /** 保存でversionが進むため、レスポンスは保存後の状態を読み直して返す */
    private TournamentDto reload(TournamentId id) {
        return tournamentRepository.findById(id)
                .map(TournamentDto::from)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TOURNAMENT_NOT_FOUND));
    }
}
