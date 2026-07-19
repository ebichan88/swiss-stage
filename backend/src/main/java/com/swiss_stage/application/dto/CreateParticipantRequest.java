package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.Rank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateParticipantRequest(
        @NotBlank(message = "氏名は必須です")
        @Size(max = 50, message = "氏名は50文字以内で入力してください")
        String name,

        @Size(max = 100, message = "所属は100文字以内で入力してください")
        String organization,

        Rank rank,

        /** 割当先グループのID(省略時は先頭グループ〈定義順〉に割当) */
        String groupId) {}
