package com.swiss_stage.application.dto;

import java.util.List;

/** グループ別チーム順位表。グループごとに1要素(group は常に非null) */
public record GroupTeamStandingsDto(GroupDto group, List<TeamStandingDto> standings) {}
