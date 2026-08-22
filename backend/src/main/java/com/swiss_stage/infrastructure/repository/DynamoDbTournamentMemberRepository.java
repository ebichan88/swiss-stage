package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.model.TournamentId;
import com.swiss_stage.domain.model.TournamentMember;
import com.swiss_stage.domain.model.TournamentMemberId;
import com.swiss_stage.domain.repository.TournamentMemberRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class DynamoDbTournamentMemberRepository implements TournamentMemberRepository {

  private final DynamoDbTable<TournamentMemberItem> table;

  public DynamoDbTournamentMemberRepository(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${app.dynamodb.table-name}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(TournamentMemberItem.class));
  }

  @Override
  public List<TournamentMember> findByTournamentId(TournamentId tournamentId) {
    var conditional =
        QueryConditional.sortBeginsWith(
            Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.MEMBER_PREFIX)
                .build());
    List<TournamentMember> result = new ArrayList<>();
    table
        .query(conditional)
        .forEach(
            page ->
                page.items()
                    .forEach(
                        item -> {
                          if (TournamentMemberItem.ENTITY_TYPE.equals(item.getEntityType())) {
                            result.add(TournamentMemberItemMapper.toDomain(item));
                          }
                        }));
    return result;
  }

  @Override
  public Optional<TournamentMember> findBySub(TournamentId tournamentId, String sub) {
    TournamentMemberItem item =
        table.getItem(
            Key.builder()
                .partitionValue(DynamoDbKeys.pk(tournamentId))
                .sortValue(DynamoDbKeys.memberSk(sub))
                .build());
    return Optional.ofNullable(item).map(TournamentMemberItemMapper::toDomain);
  }

  @Override
  public Optional<TournamentMember> findByMemberId(
      TournamentId tournamentId, TournamentMemberId memberId) {
    return findByTournamentId(tournamentId).stream()
        .filter(m -> m.id().equals(memberId))
        .findFirst();
  }

  @Override
  public List<TournamentId> findTournamentIdsByUserSub(String sub) {
    DynamoDbIndex<TournamentMemberItem> gsi1 = table.index("GSI1");
    var request =
        QueryEnhancedRequest.builder()
            .queryConditional(
                QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(DynamoDbKeys.gsi1Pk(sub)).build()))
            // GSI1PKはTournamentアイテム(USER#{ownerSub})とMEMBERアイテム(USER#{sub})が
            // 相乗りするため、entityTypeで絞らないとTOURNAMENTアイテムが混入する
            .filterExpression(
                Expression.builder()
                    .expression("entityType = :entityType")
                    .expressionValues(
                        Map.of(
                            ":entityType", AttributeValue.fromS(TournamentMemberItem.ENTITY_TYPE)))
                    .build())
            .build();
    List<TournamentId> result = new ArrayList<>();
    gsi1.query(request)
        .forEach(
            page ->
                page.items()
                    .forEach(item -> result.add(TournamentMemberItemMapper.tournamentIdOf(item))));
    return result;
  }

  @Override
  public void save(
      TournamentId tournamentId, TournamentMember member, Instant tournamentCreatedAt) {
    table.putItem(TournamentMemberItemMapper.toItem(tournamentId, member, tournamentCreatedAt));
  }

  @Override
  public void delete(TournamentId tournamentId, TournamentMemberId memberId) {
    findByMemberId(tournamentId, memberId)
        .ifPresent(
            member ->
                table.deleteItem(
                    Key.builder()
                        .partitionValue(DynamoDbKeys.pk(tournamentId))
                        .sortValue(DynamoDbKeys.memberSk(member.sub()))
                        .build()));
  }
}
