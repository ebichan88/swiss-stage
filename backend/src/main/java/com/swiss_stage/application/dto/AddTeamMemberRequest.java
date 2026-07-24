package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Rank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** boardPosition省略(null)は補欠として追加する */
public record AddTeamMemberRequest(
        @NotBlank(message = "氏名は必須です")
        @Size(max = 50, message = "氏名は50文字以内で入力してください")
        String name,

        Rank rank,

        @Min(value = 1, message = "ボード位置は1以上で入力してください")
        @Max(value = 5, message = "ボード位置は5以下で入力してください")
        Integer boardPosition) {}
