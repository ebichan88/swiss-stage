package com.swiss_stage.application.dto;

import java.util.List;

/** 共同管理者一覧+招待リンクの状態(schema/openapi.yaml の TournamentMembersView) */
public record TournamentMembersViewDto(
    List<TournamentMemberDto> members, TournamentInviteDto invite, int maxMembers) {}
