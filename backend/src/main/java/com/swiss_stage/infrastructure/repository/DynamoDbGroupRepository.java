package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.Group;
import com.swiss_stage.domain.model.GroupId;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.GroupRepository;
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
public class DynamoDbGroupRepository implements GroupRepository {

    private final DynamoDbTable<GroupItem> table;

    public DynamoDbGroupRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(GroupItem.class));
    }

    @Override
    public Optional<Group> findById(TournamentId tournamentId, GroupId id) {
        GroupItem item = table.getItem(key(tournamentId, id));
        return Optional.ofNullable(item).map(GroupItemMapper::toDomain);
    }

    @Override
    public List<Group> findAllByTournamentId(TournamentId tournamentId) {
        var conditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.GROUP_PREFIX)
                .build());
        List<Group> result = new ArrayList<>();
        table.query(conditional).forEach(page ->
                page.items().forEach(item -> result.add(GroupItemMapper.toDomain(item))));
        return result;
    }

    @Override
    public void save(TournamentId tournamentId, Group group) {
        table.putItem(GroupItemMapper.toItem(tournamentId, group));
    }

    @Override
    public void delete(TournamentId tournamentId, GroupId id) {
        table.deleteItem(key(tournamentId, id));
    }

    private Key key(TournamentId tournamentId, GroupId id) {
        return Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.groupSk(id))
                .build();
    }
}
