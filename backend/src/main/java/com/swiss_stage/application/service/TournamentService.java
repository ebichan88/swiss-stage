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
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentRole;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.repository.TeamRepository;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import com.swiss_stage.domain.repository.TournamentRepository;
import com.swiss_stage.domain.service.GroupAssignmentService;
import com.swiss_stage.domain.service.TeamRosterValidationService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

  private static final Set<Integer> VALID_TEAM_SIZES = Set.of(3, 5);

  private final TournamentRepository tournamentRepository;
  private final ParticipantRepository participantRepository;
  private final TeamRepository teamRepository;
  private final GroupRepository groupRepository;
  private final TournamentMemberRepository memberRepository;
  private final TournamentAccessSupport access;
  private final SharedViewCache sharedViewCache;
  private final GroupAssignmentService assignmentService = new GroupAssignmentService();
  private final TeamRosterValidationService rosterValidation = new TeamRosterValidationService();
  private final Clock clock;

  public TournamentService(
      TournamentRepository tournamentRepository,
      ParticipantRepository participantRepository,
      TeamRepository teamRepository,
      GroupRepository groupRepository,
      TournamentMemberRepository memberRepository,
      TournamentAccessSupport access,
      SharedViewCache sharedViewCache,
      Clock clock) {
    this.tournamentRepository = tournamentRepository;
    this.participantRepository = participantRepository;
    this.teamRepository = teamRepository;
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
    this.access = access;
    this.sharedViewCache = sharedViewCache;
    this.clock = clock;
  }

  /** 所有大会と共同管理大会を1つのリストにマージして返す(新しい順。TRN-AC-027) */
  public List<TournamentDto> list(String sub) {
    List<Tournament> owned = tournamentRepository.findByOwnerSub(sub);
    List<Tournament> memberTournaments =
        memberRepository.findTournamentIdsByUserSub(sub).stream()
            .map(tournamentRepository::findById)
            .flatMap(Optional::stream)
            .toList();
    return Stream.concat(owned.stream(), memberTournaments.stream())
        .sorted(Comparator.comparing(Tournament::createdAt).reversed())
        .map(t -> TournamentDto.from(t, roleOf(t, sub)))
        .toList();
  }

  public TournamentDto create(String ownerSub, CreateTournamentRequest request) {
    validateCompetitionType(request);
    Tournament tournament =
        Tournament.create(
            request.name(),
            request.gameType(),
            request.competitionType(),
            request.teamSize(),
            request.eventDate(),
            request.totalRounds(),
            ownerSub,
            Instant.now(clock));
    tournamentRepository.save(tournament);
    // 大会は常に1つ以上のグループを持つ(05 §2.4)。デフォルトグループを同時に作成する
    try {
      groupRepository.save(tournament.id(), Group.create(Group.DEFAULT_NAME));
    } catch (RuntimeException e) {
      // グループのない大会を残さない(残ると以後の参加者追加・自動振り分けが失敗し続ける)
      tournamentRepository.delete(tournament.id());
      throw e;
    }
    return reload(tournament.id(), TournamentRole.OWNER);
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

  public TournamentDto get(TournamentId id, String sub) {
    Tournament tournament = access.loadMember(id, sub);
    return TournamentDto.from(tournament, roleOf(tournament, sub));
  }

  public TournamentDto update(TournamentId id, String ownerSub, UpdateTournamentRequest request) {
    Tournament tournament = access.loadOwner(id, ownerSub);
    if (tournament.version() != request.version()) {
      throw new ConflictException();
    }
    validateEventDate(request);
    if (request.name() != null) {
      tournament = tournament.rename(request.name());
    }
    if (Boolean.TRUE.equals(request.clearEventDate())) {
      tournament = tournament.withEventDate(null);
    } else if (request.eventDate() != null) {
      tournament = tournament.withEventDate(request.eventDate());
    }
    if (request.visibility() != null) {
      tournament = tournament.withVisibility(request.visibility());
    }
    if (request.resultInputEnabled() != null) {
      tournament = tournament.withResultInputEnabled(request.resultInputEnabled());
    }
    tournamentRepository.save(tournament.touched(Instant.now(clock)));
    sharedViewCache.evict(id);
    return reload(id, TournamentRole.OWNER);
  }

  /** 開催日の設定と未設定化を同時に指示されたらどちらを優先すべきか決まらないため、明示的に弾く */
  private void validateEventDate(UpdateTournamentRequest request) {
    if (Boolean.TRUE.equals(request.clearEventDate()) && request.eventDate() != null) {
      throw new ValidationException("開催日の変更と未設定化は同時に指定できません");
    }
  }

  /** 共有トークンの発行・再発行(13_security_design.md §2)。 上書き保存のため旧トークンは即時無効になる(キャッシュも同時に破棄)。 */
  public TournamentDto regenerateShareToken(TournamentId id, String ownerSub) {
    Tournament tournament = access.loadOwner(id, ownerSub);
    tournamentRepository.save(
        tournament.withShareToken(ShareTokens.generate()).touched(Instant.now(clock)));
    sharedViewCache.evict(id);
    return reload(id, TournamentRole.OWNER);
  }

  public void delete(TournamentId id, String ownerSub) {
    access.loadOwner(id, ownerSub);
    tournamentRepository.delete(id);
    sharedViewCache.evict(id);
  }

  public TournamentDto start(TournamentId id, String sub) {
    Tournament tournament = access.loadMember(id, sub);
    if (tournament.isTeamCompetition()) {
      validateTeamsForStart(id, tournament.teamSize());
    } else {
      validateParticipantsForStart(id);
    }
    tournamentRepository.save(tournament.start().touched(Instant.now(clock)));
    sharedViewCache.evict(id);
    return reload(id, roleOf(tournament, sub));
  }

  private void validateParticipantsForStart(TournamentId id) {
    List<Participant> participants = participantRepository.findAllByTournamentId(id);
    long activeCount = participants.stream().filter(Participant::isActive).count();
    if (activeCount < 2) {
      throw new InvalidStateException("大会の開始には参加者が2名以上必要です");
    }
    // グループ大会は全ACTIVE参加者の割当済み・各グループ2名以上を検証(05 §2.4)
    try {
      assignmentService.validateForStart(groupRepository.findAllByTournamentId(id), participants);
    } catch (DomainException e) {
      throw new InvalidStateException(e.getMessage());
    }
  }

  private void validateTeamsForStart(TournamentId id, int teamSize) {
    List<Team> teams = teamRepository.findAllByTournamentId(id);
    long activeCount = teams.stream().filter(Team::isActive).count();
    if (activeCount < 2) {
      throw new InvalidStateException("大会の開始には2チーム以上が必要です");
    }
    try {
      assignmentService.validateTeamsForStart(groupRepository.findAllByTournamentId(id), teams);
      rosterValidation.validateForStart(teams, teamSize);
    } catch (DomainException e) {
      throw new InvalidStateException(e.getMessage());
    }
  }

  public TournamentDto finish(TournamentId id, String sub) {
    Tournament tournament = access.loadMember(id, sub);
    tournamentRepository.save(tournament.finish().touched(Instant.now(clock)));
    sharedViewCache.evict(id);
    return reload(id, roleOf(tournament, sub));
  }

  /** 保存でversionが進むため、レスポンスは保存後の状態を読み直して返す */
  private TournamentDto reload(TournamentId id, TournamentRole role) {
    return tournamentRepository
        .findById(id)
        .map(t -> TournamentDto.from(t, role))
        .orElseThrow(() -> new NotFoundException(ErrorCode.TOURNAMENT_NOT_FOUND));
  }

  /** loadOwner/loadMemberで所属済みと確定した後の役割判定(追加のクエリを要しない) */
  private static TournamentRole roleOf(Tournament tournament, String sub) {
    return tournament.isOwnedBy(sub) ? TournamentRole.OWNER : TournamentRole.MAINTAINER;
  }
}
