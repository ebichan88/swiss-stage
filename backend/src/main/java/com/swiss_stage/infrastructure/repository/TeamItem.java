package com.swiss_stage.infrastructure.repository;

import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * 団体戦チームのDynamoDBアイテム(PK=TOURNAMENT#{id}, SK=TEAM#{id})。
 * members は埋め込みリスト属性(最大 teamSize+補欠上限 のため別アイテム化しない)。
 */
@DynamoDbBean
public class TeamItem {

    static final String ENTITY_TYPE = "TEAM";

    private String pk;
    private String sk;
    private String entityType;
    private String teamId;
    private String name;
    private Integer entryOrder;
    private String status;
    private String groupId;
    private List<TeamMemberItem> members;

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

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getEntryOrder() {
        return entryOrder;
    }

    public void setEntryOrder(Integer entryOrder) {
        this.entryOrder = entryOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<TeamMemberItem> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMemberItem> members) {
        this.members = members;
    }
}
