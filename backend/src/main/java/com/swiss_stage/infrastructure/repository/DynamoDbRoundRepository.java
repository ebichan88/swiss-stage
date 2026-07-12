package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.DuplicateRoundException;
import com.swiss_stage.domain.model.Round;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.RoundRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class DynamoDbRoundRepository implements RoundRepository {

    private final DynamoDbTable<RoundItem> table;

    public DynamoDbRoundRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(RoundItem.class));
    }

    @Override
    public Optional<Round> findByRoundNumber(TournamentId tournamentId, int roundNumber) {
        RoundItem item = table.getItem(Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.roundSk(roundNumber))
                .build());
        return Optional.ofNullable(item).map(RoundItemMapper::toDomain);
    }

    @Override
    public List<Round> findAllByTournamentId(TournamentId tournamentId) {
        var conditional = QueryConditional.sortBeginsWith(Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.ROUND_PREFIX)
                .build());
        List<Round> result = new ArrayList<>();
        // SKプレフィックスROUND#はMatchアイテムも一致するためentityTypeで絞る
        table.query(conditional).forEach(page -> page.items().forEach(item -> {
            if (RoundItem.ENTITY_TYPE.equals(item.getEntityType())) {
                result.add(RoundItemMapper.toDomain(item));
            }
        }));
        return result;
    }

    @Override
    public void create(TournamentId tournamentId, Round round) {
        var request = PutItemEnhancedRequest.builder(RoundItem.class)
                .item(RoundItemMapper.toItem(tournamentId, round))
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(PK)")
                        .build())
                .build();
        try {
            table.putItem(request);
        } catch (ConditionalCheckFailedException e) {
            throw new DuplicateRoundException(
                    "ラウンド" + round.roundNumber() + "は既に生成されています");
        }
    }

    @Override
    public void save(TournamentId tournamentId, Round round) {
        table.putItem(RoundItemMapper.toItem(tournamentId, round));
    }
}
