package com.swiss_stage.application.dto;

import java.util.List;

/**
 * グループ別順位表。グループごとに1要素(group は常に非null)
 * (schema/openapi.yaml の GroupStandings)。
 */
public record GroupStandingsDto(GroupDto group, List<StandingDto> standings) {}
