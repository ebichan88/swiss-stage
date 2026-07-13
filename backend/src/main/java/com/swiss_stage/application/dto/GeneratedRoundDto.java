package com.swiss_stage.application.dto;

import java.util.List;

/**
 * ラウンド生成結果。relaxations が空でない場合はUIに警告を表示する
 * (05_swiss_pairing_algorithm.md §2.2: 絶対制約は解がない場合のみ緩和し記録する)。
 */
public record GeneratedRoundDto(RoundDto round, List<String> relaxations) {}
