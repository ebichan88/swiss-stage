package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.TeamMatch;
import com.swiss_stage.domain.model.TeamMatchId;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.TeamMatchRepository;
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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class DynamoDbTeamMatchRepository implements TeamMatchRepository {

    private final DynamoDbTable<TeamMatchItem> table;

    public DynamoDbTeamMatchRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(TeamMatchItem.class));
    }

    @Override
    public Optional<TeamMatch> findById(TournamentId tournamentId, TeamMatchId id) {
        // TeamMatchIdだけではSK(ROUND#nn#TEAM_MATCH#{id})を組み立てられないため全対局から探す
        return findAllByTournamentId(tournamentId).stream()
                .filter(m -> m.id().equals(id))
                .findFirst();
    }

    @Override
    public List<TeamMatch> findAllByTournamentId(TournamentId tournamentId) {
        return queryByPrefix(tournamentId, DynamoDbKeys.ROUND_PREFIX);
    }

    @Override
    public List<TeamMatch> findByRound(TournamentId tournamentId, int roundNumber) {
        return queryByPrefix(tournamentId, DynamoDbKeys.teamMatchSkPrefix(roundNumber));
    }

    @Override
    public void save(TournamentId tournamentId, TeamMatch match) {
        try {
            // @DynamoDbVersionAttribute によりversion一致の条件付き書き込みになる
            table.putItem(TeamMatchItemMapper.toItem(tournamentId, match));
        } catch (ConditionalCheckFailedException e) {
            throw new OptimisticLockException("対局が他の操作で更新されています: " + match.id().value());
        }
    }

    @Override
    public void saveAll(TournamentId tournamentId, List<TeamMatch> matches) {
        // BatchWriteItemは条件付き書き込み(楽観ロック)が効かないため逐次putで書く
        matches.forEach(m -> save(tournamentId, m));
    }

    /** SK前方一致で対局を取得する。プレフィックスがROUND#の場合Round/Matchアイテムも一致するためentityTypeで絞る */
    private List<TeamMatch> queryByPrefix(TournamentId tournamentId, String skPrefix) {
        var conditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(skPrefix)
                .build());
        List<TeamMatch> result = new ArrayList<>();
        table.query(conditional).forEach(page -> page.items().forEach(item -> {
            if (TeamMatchItem.ENTITY_TYPE.equals(item.getEntityType())) {
                result.add(TeamMatchItemMapper.toDomain(item));
            }
        }));
        return result;
    }
}
