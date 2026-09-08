package com.swiss_stage.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * 招待リンクのDynamoDBアイテム(PK=TOURNAMENT#{id}, SK=INVITE)。1大会につき常に0または1件。
 * GSI2に相乗りする(GSI2PK=INVITE#{token})。共有トークン(SHARE#接頭辞)と名前空間が異なるため
 * 同じGSI2上で共存できる(14_tournament_collaboration.md §4.3)。
 */
@DynamoDbBean
public class TournamentInviteItem {

  static final String ENTITY_TYPE = "INVITE";

  private String pk;
  private String sk;
  private String entityType;
  private String token;
  private String expiresAt;
  private Integer maxUses;
  private Integer usedCount;
  private String createdAt;
  private String gsi2Pk;
  private Long version;

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

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Integer getMaxUses() {
    return maxUses;
  }

  public void setMaxUses(Integer maxUses) {
    this.maxUses = maxUses;
  }

  public Integer getUsedCount() {
    return usedCount;
  }

  public void setUsedCount(Integer usedCount) {
    this.usedCount = usedCount;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = "GSI2")
  @DynamoDbAttribute("GSI2PK")
  public String getGsi2Pk() {
    return gsi2Pk;
  }

  public void setGsi2Pk(String gsi2Pk) {
    this.gsi2Pk = gsi2Pk;
  }

  @DynamoDbVersionAttribute
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
