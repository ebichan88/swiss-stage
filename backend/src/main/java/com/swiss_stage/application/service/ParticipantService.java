package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.CreateParticipantRequest;
import com.swiss_stage.application.dto.CsvImportResultDto;
import com.swiss_stage.application.dto.ParticipantDto;
import com.swiss_stage.application.dto.UpdateParticipantRequest;
import com.swiss_stage.application.exception.ErrorCode;
import com.swiss_stage.application.exception.InvalidStateException;
import com.swiss_stage.application.exception.NotFoundException;
import com.swiss_stage.application.exception.ValidationException;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentStatus;
import com.swiss_stage.domain.repository.ParticipantRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final TournamentAccessSupport access;
    private final ParticipantCsvParser csvParser;
    private final SharedViewCache sharedViewCache;

    public ParticipantService(
            ParticipantRepository participantRepository,
            TournamentAccessSupport access,
            ParticipantCsvParser csvParser,
            SharedViewCache sharedViewCache) {
        this.participantRepository = participantRepository;
        this.access = access;
        this.csvParser = csvParser;
        this.sharedViewCache = sharedViewCache;
    }

    public List<ParticipantDto> list(TournamentId tournamentId, String ownerSub) {
        access.loadOwned(tournamentId, ownerSub);
        return participantRepository.findAllByTournamentId(tournamentId).stream()
                .sorted(Comparator.comparingInt(Participant::seedOrder))
                .map(ParticipantDto::from)
                .toList();
    }

    public ParticipantDto add(
            TournamentId tournamentId, String ownerSub, CreateParticipantRequest request) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "参加者の追加は大会開始前のみ可能です");
        Participant participant = Participant.create(
                request.name(), normalize(request.organization()), request.rank(),
                nextSeedOrder(tournamentId));
        participantRepository.save(tournamentId, participant);
        sharedViewCache.evict(tournamentId);
        return ParticipantDto.from(participant);
    }

    public CsvImportResultDto importCsv(TournamentId tournamentId, String ownerSub, byte[] csv) {
        Tournament tournament = access.loadOwned(tournamentId, ownerSub);
        requirePreparing(tournament, "CSVインポートは大会開始前のみ可能です");
        List<ParticipantCsvParser.Row> rows = csvParser.parse(csv);

        int seedOrder = nextSeedOrder(tournamentId);
        List<Participant> participants = new ArrayList<>();
        for (ParticipantCsvParser.Row row : rows) {
            participants.add(Participant.create(
                    row.name(), normalize(row.organization()), row.rank(), seedOrder++));
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
        access.loadOwned(tournamentId, ownerSub);
        boolean clearRank = Boolean.TRUE.equals(request.clearRank());
        if (clearRank && request.rank() != null) {
            throw new ValidationException("rank と clearRank は同時に指定できません");
        }
        Participant participant = participantRepository.findById(tournamentId, participantId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PARTICIPANT_NOT_FOUND));

        Participant updated = new Participant(
                participant.id(),
                request.name() != null ? request.name() : participant.name(),
                request.organization() != null
                        ? normalize(request.organization()) : participant.organization(),
                clearRank ? null : request.rank() != null ? request.rank() : participant.rank(),
                participant.seedOrder(),
                request.status() != null ? request.status() : participant.status());
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

    private int nextSeedOrder(TournamentId tournamentId) {
        return participantRepository.findAllByTournamentId(tournamentId).stream()
                .mapToInt(Participant::seedOrder)
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
