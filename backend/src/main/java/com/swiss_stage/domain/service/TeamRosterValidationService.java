package com.swiss_stage.domain.service;

import com.swiss_stage.domain.DomainException;
import com.swiss_stage.domain.model.Team;
import com.swiss_stage.domain.model.TeamMember;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * チーム編成のルール(05_swiss_pairing_algorithm.md §5.1)。
 * 必須ボード位置(1..teamSize)の過不足・補欠人数上限はteamSize(Tournament側の属性)を
 * 要するため、Team単体(値オブジェクト)では検証できず、このドメインサービスで行う。
 */
public final class TeamRosterValidationService {

    /** teamSize=3→補欠最大2名、teamSize=5→補欠最大3名 */
    public static int maxReserves(int teamSize) {
        return teamSize == 3 ? 2 : 3;
    }

    /** 補欠人数の上限を超えていないか(メンバー追加時に呼ぶ) */
    public void validateReserveCount(Team team, int teamSize) {
        if (team.reserveCount() > maxReserves(teamSize)) {
            throw new DomainException(
                    "補欠は" + maxReserves(teamSize) + "名までです");
        }
    }

    /**
     * 大会開始時の検証。全ACTIVEチームの必須ボード位置(1..teamSize)が過不足なく
     * 1名ずつ埋まっていること。違反時はDomainException。
     */
    public void validateForStart(List<Team> teams, int teamSize) {
        Set<Integer> requiredPositions = IntStream.rangeClosed(1, teamSize)
                .boxed()
                .collect(Collectors.toSet());
        for (Team team : teams) {
            if (!team.isActive()) {
                continue;
            }
            Set<Integer> filled = team.members().stream()
                    .map(TeamMember::boardPosition)
                    .filter(p -> p != null)
                    .collect(Collectors.toSet());
            if (!filled.equals(requiredPositions)) {
                throw new DomainException(
                        "チーム「" + team.name() + "」の必須ポジション(主将〜"
                                + positionLabel(teamSize) + ")が揃っていません");
            }
        }
    }

    private static String positionLabel(int teamSize) {
        return switch (teamSize) {
            case 3 -> "三将";
            case 5 -> "五将";
            default -> teamSize + "将";
        };
    }
}
