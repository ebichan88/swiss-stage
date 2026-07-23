package com.swiss_stage.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank(message = "チーム名は必須です")
        @Size(max = 50, message = "チーム名は50文字以内で入力してください")
        String name,

        String groupId) {}
