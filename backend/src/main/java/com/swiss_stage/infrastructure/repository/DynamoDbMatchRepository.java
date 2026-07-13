package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.MatchId;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.MatchRepository;
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
public class DynamoDbMatchRepository implements MatchRepository {

    private final DynamoDbTable<MatchItem> table;

    public DynamoDbMatchRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(MatchItem.class));
    }

    @Override
    public Optional<Match> findById(TournamentId tournamentId, MatchId id) {
        // MatchIdだけではSK(ROUND#nn#MATCH#{id})を組み立てられないため全対局から探す。
        // 対局数は最大でも 300名 × 8回戦 = 1200件程度(AP3の範囲)
        return findAllByTournamentId(tournamentId).stream()
                .filter(m -> m.id().equals(id))
                .findFirst();
    }

    @Override
    public List<Match> findAllByTournamentId(TournamentId tournamentId) {
        return queryByPrefix(tournamentId, DynamoDbKeys.ROUND_PREFIX);
    }

    @Override
    public List<Match> findByRound(TournamentId tournamentId, int roundNumber) {
        return queryByPrefix(tournamentId, DynamoDbKeys.matchSkPrefix(roundNumber));
    }

    @Override
    public void save(TournamentId tournamentId, Match match) {
        try {
            // @DynamoDbVersionAttribute によりversion一致の条件付き書き込みになる
            table.putItem(MatchItemMapper.toItem(tournamentId, match));
        } catch (ConditionalCheckFailedException e) {
            throw new OptimisticLockException("対局が他の操作で更新されています: " + match.id().value());
        }
    }

    @Override
    public void saveAll(TournamentId tournamentId, List<Match> matches) {
        // BatchWriteItemは条件付き書き込み(楽観ロック)が効かないため逐次putで書く
        matches.forEach(m -> save(tournamentId, m));
    }

    /** SK前方一致で対局を取得する。プレフィックスがROUND#の場合Roundアイテムも一致するためentityTypeで絞る */
    private List<Match> queryByPrefix(TournamentId tournamentId, String skPrefix) {
        var conditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(skPrefix)
                .build());
        List<Match> result = new ArrayList<>();
        table.query(conditional).forEach(page -> page.items().forEach(item -> {
            if (MatchItem.ENTITY_TYPE.equals(item.getEntityType())) {
                result.add(MatchItemMapper.toDomain(item));
            }
        }));
        return result;
    }
}
