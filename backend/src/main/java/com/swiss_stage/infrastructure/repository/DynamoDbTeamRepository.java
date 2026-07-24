package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamId;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.TeamRepository;
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
public class DynamoDbTeamRepository implements TeamRepository {

    private final DynamoDbTable<TeamItem> table;

    public DynamoDbTeamRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(TeamItem.class));
    }

    @Override
    public Optional<Team> findById(TournamentId tournamentId, TeamId id) {
        TeamItem item = table.getItem(key(tournamentId, id));
        return Optional.ofNullable(item).map(TeamItemMapper::toDomain);
    }

    @Override
    public List<Team> findAllByTournamentId(TournamentId tournamentId) {
        var conditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.TEAM_PREFIX)
                .build());
        List<Team> result = new ArrayList<>();
        table.query(conditional).forEach(page ->
                page.items().forEach(item -> result.add(TeamItemMapper.toDomain(item))));
        return result;
    }

    @Override
    public void save(TournamentId tournamentId, Team team) {
        table.putItem(TeamItemMapper.toItem(tournamentId, team));
    }

    @Override
    public void saveAll(TournamentId tournamentId, List<Team> teams) {
        teams.forEach(t -> save(tournamentId, t));
    }

    @Override
    public void delete(TournamentId tournamentId, TeamId id) {
        table.deleteItem(key(tournamentId, id));
    }

    private Key key(TournamentId tournamentId, TeamId id) {
        return Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.teamSk(id))
                .build();
    }
}
