package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.CreateParticipantRequest;
import com.swiss_stage.application.dto.CsvImportResultDto;
import com.swiss_stage.application.dto.FieldErrorDto;
import com.swiss_stage.application.dto.ParticipantDto;
import com.swiss_stage.application.dto.UpdateParticipantRequest;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.repository.GroupRepository;
import com.swiss_stage.domain.repository.ParticipantRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final GroupRepository groupRepository;
    private final TournamentAccessSupport access;
    private final ParticipantCsvParser csvParser;
    private final SharedViewCache sharedViewCache;

    public ParticipantService(
            ParticipantRepository participantRepository,
            GroupRepository groupRepository,
            TournamentAccessSupport access,
            ParticipantCsvParser csvParser,
            SharedViewCache sharedViewCache) {
        this.participantRepository = participantRepository;
        this.groupRepository = groupRepository;
        this.access = access;
        this.csvParser = csvParser;
        this.sharedViewCache = sharedViewCache;
    }

    public List<ParticipantDto> list(TournamentId tournamentId, String ownerSub) {
        access.loadOwned(tournamentId, ownerSub);
        return participantRepository.findAllByTournamentId(tournamentId).stream()
                .sorted(Comparator.comparingInt(Participant::entryOrder))
                .map(ParticipantDto::from)
                .toList();
    }

    public ParticipantDto add(
            TournamentId tournamentId, String ownerSub, CreateParticipantRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "参加者の追加は大会開始前のみ可能です");
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        GroupId groupId = request.groupId() == null
                ? firstGroup(groups).id()
                : resolveGroup(groups, request.groupId()).id();
        Participant participant = Participant.create(
                request.name(), normalize(request.organization()), request.rank(),
                nextEntryOrder(tournamentId), groupId);
        participantRepository.save(tournamentId, participant);
        sharedViewCache.evict(tournamentId);
        return ParticipantDto.from(participant);
    }

    public CsvImportResultDto importCsv(TournamentId tournamentId, String ownerSub, byte[] csv) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "CSVインポートは大会開始前のみ可能です");
        List<ParticipantCsvParser.Row> rows = csvParser.parse(csv);
        List<Group> groups = groupRepository.findAllByTournamentId(tournamentId);
        Map<String, GroupId> groupsByName = groups.stream()
                .collect(Collectors.toMap(Group::name, Group::id));
        validateGroupNames(rows, groupsByName);

        int entryOrder = nextEntryOrder(tournamentId);
        GroupId defaultGroupId = firstGroup(groups).id();
        List<Participant> participants = new ArrayList<>();
        for (ParticipantCsvParser.Row row : rows) {
            GroupId groupId = row.groupName() == null
                    ? defaultGroupId
                    : groupsByName.get(row.groupName());
            participants.add(Participant.create(
                    row.name(), normalize(row.organization()), row.rank(), entryOrder++, groupId));
        }
        participantRepository.saveAll(tournamentId, participants);
        sharedViewCache.evict(tournamentId);
        return new CsvImportResultDto(
                participants.size(),
                participants.stream().map(ParticipantDto::from).toList());
    }

    public ParticipantDto update(
            TournamentId tournamentId, ParticipantId participantId,
            String ownerSub, UpdateParticipantRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        boolean clearRank = Boolean.TRUE.equals(request.clearRank());
        if (clearRank && request.rank() != null) {
            throw new ValidationException("rank と clearRank は同時に指定できません");
        }
        Participant participant = participantRepository.findById(tournamentId, participantId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PARTICIPANT_NOT_FOUND));

        GroupId groupId = participant.groupId();
        if (request.groupId() != null) {
            requirePreparing(tournament, "グループ割当の変更は大会開始前のみ可能です");
            groupId = resolveGroup(
                    groupRepository.findAllByTournamentId(tournamentId), request.groupId()).id();
        }
        Participant updated = new Participant(
                participant.id(),
                request.name() != null ? request.name() : participant.name(),
                request.organization() != null
                        ? normalize(request.organization()) : participant.organization(),
                clearRank ? null : request.rank() != null ? request.rank() : participant.rank(),
                participant.entryOrder(),
                request.status() != null ? request.status() : participant.status(),
                groupId);
        participantRepository.save(tournamentId, updated);
        sharedViewCache.evict(tournamentId);
        return ParticipantDto.from(updated);
    }

    public void delete(TournamentId tournamentId, ParticipantId participantId, String ownerSub) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "参加者の削除は大会開始前のみ可能です(開始後は棄権にしてください)");
        participantRepository.findById(tournamentId, participantId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PARTICIPANT_NOT_FOUND));
        participantRepository.delete(tournamentId, participantId);
        sharedViewCache.evict(tournamentId);
    }

    /**
     * グループの解決。ユーザー入力をキー組み立てに使わないため、
     * 大会のグループ一覧(最大10件)から突き合わせる
     */
    private static Group resolveGroup(List<Group> groups, String groupIdValue) {
        return groups.stream()
                .filter(g -> g.id().value().equals(groupIdValue))
                .findFirst()
                .orElseThrow(() -> new ValidationException("指定されたグループが存在しません"));
    }

    /** グループ指定の省略時に割り当てる先頭グループ(定義順。大会は常に1つ以上のグループを持つ) */
    private static Group firstGroup(List<Group> groups) {
        if (groups.isEmpty()) {
            throw new InvalidStateException("大会にグループが存在しません");
        }
        return groups.getFirst();
    }

    /** CSVのグループ列は定義済みグループ名に完全一致(未知の名前は行エラー。自動作成しない) */
    private static void validateGroupNames(
            List<ParticipantCsvParser.Row> rows, Map<String, GroupId> groupsByName) {
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

    private int nextEntryOrder(TournamentId tournamentId) {
        return participantRepository.findAllByTournamentId(tournamentId).stream()
                .mapToInt(Participant::entryOrder)
                .max()
                .orElse(0) + 1;
    }

    private static void requirePreparing(Tournament tournament, String message) {
        if (tournament.status() != TournamentStatus.PREPARING) {
            throw new InvalidStateException(message);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
