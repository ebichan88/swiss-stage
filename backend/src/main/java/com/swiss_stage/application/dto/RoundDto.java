package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.RoundStatus;
import java.util.List;

public record RoundDto(int roundNumber, RoundStatus status, List<MatchDto> matches) {}
