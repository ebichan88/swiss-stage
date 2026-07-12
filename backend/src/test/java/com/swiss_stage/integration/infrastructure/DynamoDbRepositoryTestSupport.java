package com.swiss_stage.integration.infrastructure;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * DynamoDB Localに対するリポジトリ統合テストの共通基盤
 * (.claude/03_library_docs/03_dynamodb_local_testing.md)。
 *
 * <p>テーブルは消さず、テストごとに一意のULIDでデータを分離する。
 * テーブル作成は冪等(既存ならスキップ)なので、ローカル・CIのどちらでも動く。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class DynamoDbRepositoryTestSupport {

    private static volatile boolean tableEnsured = false;

    @Autowired
    private DynamoDbClient client;

    @Value("${app.dynamodb.table-name}")
    private String tableName;

    @BeforeEach
    void ensureTable() {
        if (tableEnsured) {
            return;
        }
        synchronized (DynamoDbRepositoryTestSupport.class) {
            if (!tableEnsured) {
                createTableIfMissing();
                tableEnsured = true;
            }
        }
    }

    private void createTableIfMissing() {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(
                            attr("PK"), attr("SK"), attr("GSI1PK"), attr("GSI1SK"), attr("GSI2PK"))
                    .keySchema(
                            KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("GSI1")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("GSI1PK").keyType(KeyType.HASH).build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("GSI1SK").keyType(KeyType.RANGE).build())
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL).build())
                                    .build(),
                            GlobalSecondaryIndex.builder()
                                    .indexName("GSI2")
                                    .keySchema(KeySchemaElement.builder()
                                            .attributeName("GSI2PK").keyType(KeyType.HASH).build())
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL).build())
                                    .build())
                    .build());
            client.waiter().waitUntilTableExists(b -> b.tableName(tableName));
        } catch (ResourceInUseException e) {
            // 既に存在する(作成済み・並行作成)場合は何もしない
        }
    }

    private static AttributeDefinition attr(String name) {
        return AttributeDefinition.builder()
                .attributeName(name)
                .attributeType(ScalarAttributeType.S)
                .build();
    }

    /** GSIは結果整合のため、空でなくなるまで少し待って取得する */
    protected static <T> List<T> awaitNonEmpty(Supplier<List<T>> query) {
        for (int i = 0; i < 50; i++) {
            List<T> result = query.get();
            if (!result.isEmpty()) {
                return result;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        return query.get();
    }
}
