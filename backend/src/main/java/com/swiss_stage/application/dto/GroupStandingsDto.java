package com.swiss_stage.application.dto;

import java.util.List;

/**
 * グループ別順位表。常にこの形で返し、グループなし大会は group=null の単一要素とする
 * (07_type_definitions.md の GroupStandings)。
 */
public record GroupStandingsDto(GroupDto group, List<StandingDto> standings) {}
