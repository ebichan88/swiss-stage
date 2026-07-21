package com.swiss_stage.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;

/**
 * 対局のDynamoDBアイテム(PK=TOURNAMENT#{id}, SK=ROUND#nn#MATCH#{id})。
 */
@DynamoDbBean
public class MatchItem {

    static final String ENTITY_TYPE = "MATCH";

    private String pk;
    private String sk;
    private String entityType;
    private String matchId;
    private Integer roundNumber;
    private Integer tableNumber;
    private String player1Id;
    private String player2Id;
    private String result;
    private String resultInputBy;
    private String player1ReportedResult;
    private String player2ReportedResult;
    private Long version;
    private String groupId;

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

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(String player1Id) {
        this.player1Id = player1Id;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResultInputBy() {
        return resultInputBy;
    }

    public void setResultInputBy(String resultInputBy) {
        this.resultInputBy = resultInputBy;
    }

    public String getPlayer1ReportedResult() {
        return player1ReportedResult;
    }

    public void setPlayer1ReportedResult(String player1ReportedResult) {
        this.player1ReportedResult = player1ReportedResult;
    }

    public String getPlayer2ReportedResult() {
        return player2ReportedResult;
    }

    public void setPlayer2ReportedResult(String player2ReportedResult) {
        this.player2ReportedResult = player2ReportedResult;
    }

    @DynamoDbVersionAttribute
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
