package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Rank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * nullの項目は変更しない。補欠に戻す場合は clearBoardPosition=true
 * (boardPositionとの同時指定は400)。
 */
public record UpdateTeamMemberRequest(
        @Size(max = 50, message = "氏名は50文字以内で入力してください")
        String name,

        Rank rank,

        Boolean clearRank,

        @Min(value = 1, message = "ボード位置は1以上で入力してください")
        @Max(value = 5, message = "ボード位置は5以下で入力してください")
        Integer boardPosition,

        Boolean clearBoardPosition) {}
