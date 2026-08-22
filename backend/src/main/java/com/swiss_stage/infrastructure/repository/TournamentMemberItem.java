package com.swiss_stage.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * 共同管理者のDynamoDBアイテム(PK=TOURNAMENT#{id}, SK=MEMBER#{sub})。GSI1に相乗りする (GSI1PK=USER#{sub},
 * GSI1SK=大会のcreatedAt)ため、大会一覧のクエリでは必ずentityTypeで TOURNAMENTアイテムと区別する(14_tournament_collaboration.md
 * §4.3の実装時の落とし穴)。
 */
@DynamoDbBean
public class TournamentMemberItem {

  static final String ENTITY_TYPE = "MEMBER";

  private String pk;
  private String sk;
  private String entityType;
  private String memberId;
  private String sub;
  private String role;
  private String displayName;
  private String joinedAt;
  private String gsi1Pk;
  private String gsi1Sk;

  @DynamoDbPartitionKey
  @DynamoDbAttribute("PK")
  public String getPk() {
    return pk;
  }

  public void setPk(String pk) {
    this.pk = pk;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute("SK")
  public String getSk() {
    return sk;
  }

  public void setSk(String sk) {
    this.sk = sk;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public String getSub() {
    return sub;
  }

  public void setSub(String sub) {
    this.sub = sub;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  /** 承諾時のGoogle表示名(個人情報)。OWNER向けレスポンス以外に出さない・ログに出さない */
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(String joinedAt) {
    this.joinedAt = joinedAt;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = "GSI1")
  @DynamoDbAttribute("GSI1PK")
  public String getGsi1Pk() {
    return gsi1Pk;
  }

  public void setGsi1Pk(String gsi1Pk) {
    this.gsi1Pk = gsi1Pk;
  }

  @DynamoDbSecondarySortKey(indexNames = "GSI1")
  @DynamoDbAttribute("GSI1SK")
  public String getGsi1Sk() {
    return gsi1Sk;
  }

  public void setGsi1Sk(String gsi1Sk) {
    this.gsi1Sk = gsi1Sk;
  }
}
