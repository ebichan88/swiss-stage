package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.Tournament;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@Repository
public class DynamoDbTournamentRepository implements TournamentRepository {

    /** BatchWriteItemの上限 */
    private static final int BATCH_WRITE_LIMIT = 25;

    private final DynamoDbClient client;
    private final DynamoDbTable<TournamentItem> table;
    private final String tableName;

    public DynamoDbTournamentRepository(
            DynamoDbClient client,
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.dynamodb.table-name}") String tableName) {
        this.client = client;
        this.tableName = tableName;
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(TournamentItem.class));
    }

    @Override
    public Optional<Tournament> findById(TournamentId id) {
        TournamentItem item = table.getItem(Key.builder()
                .partitionValue(DynamoDbKeys.pk(id))
                .sortValue(DynamoDbKeys.METADATA_SK)
                .build());
        return Optional.ofNullable(item).map(TournamentItemMapper::toDomain);
    }

    @Override
    public List<Tournament> findByOwnerSub(String ownerSub) {
        DynamoDbIndex<TournamentItem> gsi1 = table.index("GSI1");
        var request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(DynamoDbKeys.gsi1Pk(ownerSub)).build()))
                .scanIndexForward(false) // GSI1SK=TOURNAMENT#{createdAt} の降順 = 新しい順
                .build();
        List<Tournament> result = new ArrayList<>();
        gsi1.query(request).forEach(page ->
                page.items().forEach(item -> result.add(TournamentItemMapper.toDomain(item))));
        return result;
    }

    @Override
    public Optional<Tournament> findByShareToken(String shareToken) {
        DynamoDbIndex<TournamentItem> gsi2 = table.index("GSI2");
        var request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(DynamoDbKeys.gsi2Pk(shareToken)).build()))
                .limit(1)
                .build();
        for (var page : gsi2.query(request)) {
            for (TournamentItem item : page.items()) {
                return Optional.of(TournamentItemMapper.toDomain(item));
            }
        }
        return Optional.empty();
    }

    @Override
    public void save(Tournament tournament) {
        try {
            // @DynamoDbVersionAttribute によりversion一致の条件付き書き込みになる
            table.putItem(TournamentItemMapper.toItem(tournament));
        } catch (ConditionalCheckFailedException e) {
            throw new OptimisticLockException("大会が他の操作で更新されています: " + tournament.id().value());
        }
    }

    @Override
    public void delete(TournamentId id) {
        // 大会配下の全アイテム(METADATA・参加者・ラウンド・対局)を物理削除する(個人情報保護方針)
        List<Map<String, AttributeValue>> keys = queryAllKeys(id);
        for (int from = 0; from < keys.size(); from += BATCH_WRITE_LIMIT) {
            List<WriteRequest> requests = keys.subList(from, Math.min(from + BATCH_WRITE_LIMIT, keys.size()))
                    .stream()
                    .map(key -> WriteRequest.builder()
                            .deleteRequest(DeleteRequest.builder().key(key).build())
                            .build())
                    .toList();
            batchDeleteWithRetry(requests);
        }
    }

    private List<Map<String, AttributeValue>> queryAllKeys(TournamentId id) {
        List<Map<String, AttributeValue>> keys = new ArrayList<>();
        Map<String, AttributeValue> exclusiveStartKey = null;
        do {
            QueryResponse response = client.query(QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("PK = :pk")
                    .expressionAttributeValues(
                            Map.of(":pk", AttributeValue.fromS(DynamoDbKeys.pk(id))))
                    .projectionExpression("PK, SK")
                    .exclusiveStartKey(exclusiveStartKey)
                    .build());
            keys.addAll(response.items());
            exclusiveStartKey = response.hasLastEvaluatedKey() ? response.lastEvaluatedKey() : null;
        } while (exclusiveStartKey != null);
        return keys;
    }

    private void batchDeleteWithRetry(List<WriteRequest> requests) {
        Map<String, List<WriteRequest>> remaining = Map.of(tableName, requests);
        // UnprocessedItemsは通常すぐ解消するため小さな固定リトライで足りる(MVP規模)
        for (int attempt = 0; attempt < 5 && !remaining.isEmpty(); attempt++) {
            Map<String, List<WriteRequest>> current = remaining;
            BatchWriteItemResponse response =
                    client.batchWriteItem(b -> b.requestItems(current));
            remaining = new HashMap<>(response.unprocessedItems());
            remaining.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
        if (!remaining.isEmpty()) {
            throw new IllegalStateException("大会データの一括削除が完了しませんでした");
        }
    }
}
