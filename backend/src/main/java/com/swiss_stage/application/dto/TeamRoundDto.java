package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.RoundStatus;
import java.util.List;

public record TeamRoundDto(int roundNumber, RoundStatus status, List<TeamMatchDto> matches) {}
