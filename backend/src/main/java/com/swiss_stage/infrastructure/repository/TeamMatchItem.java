package com.swiss_stage.infrastructure.repository;

import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * 団体戦対局のDynamoDBアイテム(PK=TOURNAMENT#{id}, SK=ROUND#nn#TEAM_MATCH#{id})。
 * boardResults は埋め込みリスト属性(長さ=teamSize。BYEの場合は空)。
 */
@DynamoDbBean
public class TeamMatchItem {

    static final String ENTITY_TYPE = "TEAM_MATCH";

    private String pk;
    private String sk;
    private String entityType;
    private String teamMatchId;
    private Integer roundNumber;
    private Integer tableNumber;
    private String team1Id;
    private String team2Id;
    private List<BoardResultItem> boardResults;
    private String resultInputBy;
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

    public String getTeamMatchId() {
        return teamMatchId;
    }

    public void setTeamMatchId(String teamMatchId) {
        this.teamMatchId = teamMatchId;
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

    public String getTeam1Id() {
        return team1Id;
    }

    public void setTeam1Id(String team1Id) {
        this.team1Id = team1Id;
    }

    public String getTeam2Id() {
        return team2Id;
    }

    public void setTeam2Id(String team2Id) {
        this.team2Id = team2Id;
    }

    public List<BoardResultItem> getBoardResults() {
        return boardResults;
    }

    public void setBoardResults(List<BoardResultItem> boardResults) {
        this.boardResults = boardResults;
    }

    public String getResultInputBy() {
        return resultInputBy;
    }

    public void setResultInputBy(String resultInputBy) {
        this.resultInputBy = resultInputBy;
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
