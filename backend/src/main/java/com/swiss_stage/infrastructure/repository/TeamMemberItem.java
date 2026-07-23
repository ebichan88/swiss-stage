package com.swiss_stage.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * チームメンバーの埋め込みBean(TeamItem#membersのリスト要素。単体ではDynamoDBアイテムにならない)。
 */
@DynamoDbBean
public class TeamMemberItem {

    private String memberId;
    private String name;
    private String rank;
    private Integer boardPosition;

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Integer getBoardPosition() {
        return boardPosition;
    }

    public void setBoardPosition(Integer boardPosition) {
        this.boardPosition = boardPosition;
    }
}
