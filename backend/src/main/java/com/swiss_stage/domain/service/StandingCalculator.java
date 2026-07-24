package com.swiss_stage.domain.service;

import com.swiss_stage.domain.model.Match;
import com.swiss_stage.domain.model.Participant;
import com.swiss_stage.domain.model.ParticipantId;
import com.swiss_stage.domain.model.Standing;
import java.util.List;

/**
 * 順位計算(個人戦)。アルゴリズム本体は{@link StandingEngine}に共通化し、このクラスは
 * Participant/Match をエンジンの汎用形に変換するアダプタに徹する(団体戦は
 * {@code TeamStandingCalculator} が同じ StandingEngine を使う)。入力(参加者+全対局)→
 * 出力(順位リスト)の純粋関数。
 *
 * <p>順位決定基準(上から順に適用): 勝点 → SOS → SOSOS → 直接対決 → 同順位。
 * SOSはBYEラウンドを除外する(除外方式を採用)。
 * 仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §3
 */
public final class StandingCalculator {

    private final StandingEngine engine = new StandingEngine();

    public List<Standing> calculate(List<Participant> participants, List<Match> matches) {
        List<StandingEngine.DecidedResult<ParticipantId>> decided = matches.stream()
                .filter(m -> m.result().isDecided())
                .map(m -> new StandingEngine.DecidedResult<>(
                        m.player1Id(),
                        m.isBye() ? null : m.player2Id(),
                        m.result().pointsForPlayer1(),
                        m.isBye() ? 0 : m.result().pointsForPlayer2()))
                .toList();

        return engine.calculate(participants, Participant::id, Participant::entryOrder, decided).stream()
                .map(row -> new Standing(
                        row.rank(), row.id(), row.wins(), row.losses(), row.draws(), row.points(),
                        row.sos(), row.sosos(), row.hadBye()))
                .toList();
    }
}
