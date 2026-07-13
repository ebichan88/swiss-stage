package com.swiss_stage.infrastructure.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * DynamoDBクライアント設定(.claude/03_library_docs/02_dynamodb_enhanced_client.md §2)。
 * endpoint はローカル(DynamoDB Local)のみ設定し、本番は空 = EC2インスタンスロールから自動取得。
 */
@Configuration
public class DynamoDbConfig {

    @Value("${app.dynamodb.endpoint:}")
    private String endpoint;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder().region(Region.AP_NORTHEAST_1);
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    // DynamoDB Localはダミー認証情報でよいが、-sharedDb事故防止のため固定値に統一する
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }
}
