package com.swiss_stage.infrastructure.repository;

import com.swiss_stage.domain.OptimisticLockException;
import com.swiss_stage.domain.RoundConfirmedException;
import com.swiss_stage.domain.model.TournamentId;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * 対局の保存とラウンド未確定チェックを同一トランザクションで行う(14_tournament_collaboration.md §4.9)。
 * MatchRepository/TeamMatchRepositoryの両実装から共有する。
 */
final class RoundConfirmationGuard {

  private RoundConfirmationGuard() {}

  /**
   * {@code matchTable}へのput(第1アイテム)と、ラウンド(第2アイテム。SK=ROUND#nn)への
   * status<>CONFIRMEDのConditionCheckを1つのトランザクションで実行する。 matchTable側の競合(version不一致)は{@link
   * OptimisticLockException}、 ラウンド側の競合(確定済みへの割り込み)は{@link RoundConfirmedException}を送出する。
   */
  static <T> void putIfRoundNotConfirmed(
      DynamoDbEnhancedClient enhancedClient,
      MappedTableResource<T> matchTable,
      T matchItem,
      DynamoDbTable<RoundItem> roundTable,
      TournamentId tournamentId,
      int roundNumber,
      String matchIdForMessage) {
    try {
      enhancedClient.transactWriteItems(
          TransactWriteItemsEnhancedRequest.builder()
              .addPutItem(matchTable, matchItem)
              .addConditionCheck(
                  roundTable,
                  ConditionCheck.builder()
                      .key(
                          Key.builder()
                              .partitionValue(DynamoDbKeys.pk(tournamentId))
                              .sortValue(DynamoDbKeys.roundSk(roundNumber))
                              .build())
                      .conditionExpression(
                          Expression.builder()
                              .expression("#status <> :confirmed")
                              .expressionNames(Map.of("#status", "status"))
                              .expressionValues(
                                  Map.of(":confirmed", AttributeValue.fromS("CONFIRMED")))
                              .build())
                      .build())
              .build());
    } catch (TransactionCanceledException e) {
      List<CancellationReason> reasons = e.cancellationReasons();
      // addの順番どおり: index0=Match(put), index1=Round(conditionCheck)
      if (reasons.size() > 1 && "ConditionalCheckFailed".equals(reasons.get(1).code())) {
        throw new RoundConfirmedException("確定済みラウンドの結果は変更できません");
      }
      throw new OptimisticLockException("対局が他の操作で更新されています: " + matchIdForMessage);
    }
  }
}
