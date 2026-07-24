package com.swiss_stage.application.dto;

import java.util.List;

/**
 * 団体戦ラウンド生成結果。relaxations が空でない場合はUIに警告を表示する
 * (05_swiss_pairing_algorithm.md §2.2/§5.2)。
 */
public record GeneratedTeamRoundDto(TeamRoundDto round, List<String> relaxations) {}
