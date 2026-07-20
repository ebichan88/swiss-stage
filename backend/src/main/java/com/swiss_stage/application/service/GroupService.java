package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.GroupDto;
import com.swiss_stage.application.dto.GroupRequest;
import com.swiss_stage.application.dto.ParticipantDto;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import com.swiss_stage.domain.service.GroupAssignmentService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 棋力帯グループの管理(05_swiss_pairing_algorithm.md §2.4)。
 * 定義・割当の変更はすべて PREPARING 中のみ。
 */
@Service
public class GroupService {

    static final int MAX_GROUPS = 10;

    private final GroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final TournamentAccessSupport access;
    private final SharedViewCache sharedViewCache;
    private final GroupAssignmentService assignmentService = new GroupAssignmentService();

    public GroupService(
            GroupRepository groupRepository,
            ParticipantRepository participantRepository,
            TournamentAccessSupport access,
            SharedViewCache sharedViewCache) {
        this.groupRepository = groupRepository;
        this.participantRepository = participantRepository;
        this.access = access;
        this.sharedViewCache = sharedViewCache;
    }

    public List<GroupDto> list(TournamentId tournamentId, String ownerSub) {
        access.loadOwned(tournamentId, ownerSub);
        return groupRepository.findAllByTournamentId(tournamentId).stream()
                .map(GroupDto::from)
                .toList();
    }

    public GroupDto create(TournamentId tournamentId, String ownerSub, GroupRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "グループの作成は大会開始前のみ可能です");
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        if (groups.size() >= MAX_GROUPS) {
            throw new ValidationException("グループは最大" + MAX_GROUPS + "個までです");
        }
        requireUniqueName(groups, request.name(), null);
        Group group = createGroup(request.name());
        groupRepository.save(tournamentId, group);
        sharedViewCache.evict(tournamentId);
        return GroupDto.from(group);
    }

    public GroupDto rename(
            TournamentId tournamentId, GroupId groupId, String ownerSub, GroupRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "グループの変更は大会開始前のみ可能です");
        Group group = load(tournamentId, groupId);
        requireUniqueName(
                groupRepository.findAllByTournamentId(tournamentId), request.name(), groupId);
        Group renamed = group.rename(request.name());
        groupRepository.save(tournamentId, renamed);
        sharedViewCache.evict(tournamentId);
        return GroupDto.from(renamed);
    }

    /**
     * グループ削除。最後の1グループは削除できない(大会は常に1つ以上のグループを持つ)。
     * 割当済みの参加者は直前のグループ(先頭グループの削除時は直後のグループ)へ移す
     */
    public void delete(TournamentId tournamentId, GroupId groupId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "グループの削除は大会開始前のみ可能です");
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        int index = indexOf(groups, groupId);
        if (groups.size() <= 1) {
            throw new ValidationException("最後のグループは削除できません");
        }
        Group fallback = groups.get(index == 0 ? 1 : index - 1);
        List<Participant> assigned = participantRepository.findAllByTournamentId(tournamentId)
                .stream()
                .filter(p -> groupId.equals(p.groupId()))
                .map(p -> p.withGroup(fallback.id()))
                .toList();
        participantRepository.saveAll(tournamentId, assigned);
        groupRepository.delete(tournamentId, groupId);
        sharedViewCache.evict(tournamentId);
    }

    /** 段級位で全ACTIVE参加者を一括振り分けし、更新後の参加者一覧を返す */
    public List<ParticipantDto> autoAssign(TournamentId tournamentId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "自動振り分けは大会開始前のみ可能です");
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        if (groups.isEmpty()) {
            throw new ValidationException("グループを作成してから自動振り分けしてください");
        }
        List<Participant> participants = participantRepository.findAllByTournamentId(tournamentId);
        Map<ParticipantId, GroupId> assignment = assignmentService.propose(groups, participants);
        // 棄権中の参加者は振り分け対象外(propose結果に含まれない)のため現在の割当を維持する
        List<Participant> updated = participants.stream()
                .map(p -> p.withGroup(assignment.getOrDefault(p.id(), p.groupId())))
                .toList();
        participantRepository.saveAll(tournamentId, updated);
        sharedViewCache.evict(tournamentId);
        return updated.stream()
                .sorted(Comparator.comparingInt(Participant::entryOrder))
                .map(ParticipantDto::from)
                .toList();
    }

    private Group load(TournamentId tournamentId, GroupId groupId) {
        return groupRepository.findById(tournamentId, groupId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.GROUP_NOT_FOUND));
    }

    private static int indexOf(List<Group> groups, GroupId groupId) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id().equals(groupId)) {
                return i;
            }
        }
        throw new NotFoundException(ErrorCode.GROUP_NOT_FOUND);
    }

    private Group createGroup(String name) {
        try {
            return Group.create(name);
        } catch (DomainException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    private static void requireUniqueName(List<Group> groups, String name, GroupId self) {
        boolean duplicated = groups.stream()
                .anyMatch(g -> g.name().equals(name) && !g.id().equals(self));
        if (duplicated) {
            throw new ValidationException("同じ名前のグループが既にあります");
        }
    }

    private static void requirePreparing(Tournament tournament, String message) {
        if (tournament.status() != TournamentStatus.PREPARING) {
            throw new InvalidStateException(message);
        }
    }
}
