package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.ParticipantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
public class DynamoDbParticipantRepository implements ParticipantRepository {

    private final DynamoDbTable<ParticipantItem> table;

    public DynamoDbParticipantRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(ParticipantItem.class));
    }

    @Override
    public Optional<Participant> findById(TournamentId tournamentId, ParticipantId id) {
        ParticipantItem item = table.getItem(key(tournamentId, id));
        return Optional.ofNullable(item).map(ParticipantItemMapper::toDomain);
    }

    @Override
    public List<Participant> findAllByTournamentId(TournamentId tournamentId) {
        var conditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.PARTICIPANT_PREFIX)
                .build());
        List<Participant> result = new ArrayList<>();
        table.query(conditional).forEach(page ->
                page.items().forEach(item -> result.add(ParticipantItemMapper.toDomain(item))));
        return result;
    }

    @Override
    public void save(TournamentId tournamentId, Participant participant) {
        table.putItem(ParticipantItemMapper.toItem(tournamentId, participant));
    }

    @Override
    public void saveAll(TournamentId tournamentId, List<Participant> participants) {
        // 件数上限は300名(MVP)。逐次putで十分であり、条件・変換の一貫性を優先する
        participants.forEach(p -> save(tournamentId, p));
    }

    @Override
    public void delete(TournamentId tournamentId, ParticipantId id) {
        table.deleteItem(key(tournamentId, id));
    }

    private Key key(TournamentId tournamentId, ParticipantId id) {
        return Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.participantSk(id))
                .build();
    }
}
