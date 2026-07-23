package com.swiss_stage.domain.model;

import com.swiss_stage.domain.DomainException;

/**
 * チームメンバー(値オブジェクト。Teamに埋め込み)。
 * boardPosition は 1..teamSize の必須ポジション(1=主将,2=副将,3=三将,4=四将,5=五将)、
 * またはnull(補欠)。boardPositionがteamSizeの範囲内か・重複がないか・補欠人数の上限は
 * Team単体では判定できない(teamSizeはTournamentが持つ)ため、application/domain層の
 * 開始前検証で行う(05_swiss_pairing_algorithm.md §5.1)。
 * 対局結果はこのboardPositionではなくボード位置単位で記録され、実際に誰(正メンバーか
 * 補欠か)が対局したかは対局結果に紐付けない(オーダー管理はしない)。
 */
public record TeamMember(TeamMemberId id, String name, Rank rank, Integer boardPosition) {

    public static final int NAME_MAX_LENGTH = 50;

    public TeamMember {
        if (name == null || name.isBlank()) {
            throw new DomainException("メンバーの氏名は必須です");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new DomainException("メンバーの氏名は" + NAME_MAX_LENGTH + "文字以内で入力してください");
        }
        if (boardPosition != null && boardPosition < 1) {
            throw new DomainException("ボード位置は1以上である必要があります");
        }
    }

    public static TeamMember create(String name, Rank rank, Integer boardPosition) {
        return new TeamMember(TeamMemberId.generate(), name, rank, boardPosition);
    }

    public boolean isReserve() {
        return boardPosition == null;
    }
}
