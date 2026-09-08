package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentInvite;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.repository.TournamentInviteRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@Repository
public class DynamoDbTournamentInviteRepository implements TournamentInviteRepository {

  private final DynamoDbEnhancedClient enhancedClient;
  private final DynamoDbTable<TournamentInviteItem> table;
  private final DynamoDbTable<TournamentMemberItem> memberTable;

  public DynamoDbTournamentInviteRepository(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${app.dynamodb.table-name}") String tableName) {
    this.enhancedClient = enhancedClient;
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(TournamentInviteItem.class));
    this.memberTable =
        enhancedClient.table(tableName, TableSchema.fromBean(TournamentMemberItem.class));
  }

  @Override
  public Optional<TournamentInvite> findByTournamentId(TournamentId tournamentId) {
    TournamentInviteItem item =
        table.getItem(
            Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.INVITE_SK)
                .build());
    return Optional.ofNullable(item).map(TournamentInviteItemMapper::toDomain);
  }

  @Override
  public Optional<TournamentInvite> findByToken(String token) {
    DynamoDbIndex<TournamentInviteItem> gsi2 = table.index("GSI2");
    var request =
        QueryEnhancedRequest.builder()
            .queryConditional(
                QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(DynamoDbKeys.gsi2PkForInvite(token)).build()))
            .limit(1)
            .build();
    for (var page : gsi2.query(request)) {
      for (TournamentInviteItem item : page.items()) {
        return Optional.of(TournamentInviteItemMapper.toDomain(item));
      }
    }
    return Optional.empty();
  }

  @Override
  public void save(TournamentInvite invite) {
    try {
      // @DynamoDbVersionAttribute によりversion一致の条件付き書き込みになる
      table.putItem(TournamentInviteItemMapper.toItem(invite));
    } catch (ConditionalCheckFailedException e) {
      throw new OptimisticLockException("招待が他の操作で更新されています: " + invite.tournamentId().value());
    }
  }

  @Override
  public void delete(TournamentId tournamentId) {
    table.deleteItem(
        Key.builder()
            .partitionValue(DynamoDbKeys.pk(tournamentId))
            .sortValue(DynamoDbKeys.INVITE_SK)
            .build());
  }

  @Override
  public boolean acceptWithMember(
      TournamentInvite acceptedInvite, TournamentMember member, Instant tournamentCreatedAt) {
    try {
      enhancedClient.transactWriteItems(
          TransactWriteItemsEnhancedRequest.builder()
              // @DynamoDbVersionAttribute によりversion一致の条件付き書き込みになる
              .addPutItem(table, TournamentInviteItemMapper.toItem(acceptedInvite))
              .addPutItem(
                  memberTable,
                  TransactPutItemEnhancedRequest.builder(TournamentMemberItem.class)
                      .item(
                          TournamentMemberItemMapper.toItem(
                              acceptedInvite.tournamentId(), member, tournamentCreatedAt))
                      // 二重承諾(同じsubの同時承諾)を弾く。MEMBERにversion属性は無いため明示条件で表現する
                      .conditionExpression(
                          Expression.builder().expression("attribute_not_exists(SK)").build())
                      .build())
              .build());
      return true;
    } catch (TransactionCanceledException e) {
      return false;
    }
  }
}
