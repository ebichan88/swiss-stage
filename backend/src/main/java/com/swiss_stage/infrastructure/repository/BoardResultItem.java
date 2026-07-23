package com.swiss_stage.infrastructure.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * ボード結果の埋め込みBean(TeamMatchItem#boardResultsのリスト要素。
 * 単体ではDynamoDBアイテムにならない)。
 */
@DynamoDbBean
public class BoardResultItem {

    private Integer boardPosition;
    private String result;
    private String team1ReportedResult;
    private String team2ReportedResult;

    public Integer getBoardPosition() {
        return boardPosition;
    }

    public void setBoardPosition(Integer boardPosition) {
        this.boardPosition = boardPosition;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTeam1ReportedResult() {
        return team1ReportedResult;
    }

    public void setTeam1ReportedResult(String team1ReportedResult) {
        this.team1ReportedResult = team1ReportedResult;
    }

    public String getTeam2ReportedResult() {
        return team2ReportedResult;
    }

    public void setTeam2ReportedResult(String team2ReportedResult) {
        this.team2ReportedResult = team2ReportedResult;
    }
}
