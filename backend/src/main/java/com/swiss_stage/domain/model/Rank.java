package com.swiss_stage.domain.model;

import java.util.Comparator;

/**
 * 棋力(段級位)。囲碁・将棋共通の29段階。
 *
 * <p>弱い順に宣言しており、宣言順(ordinal)がそのまま強さの順序になる。
 * 最弱は20級、最強は九段。1級の次は初段(DAN_1)であり「1段」は存在しない。
 * (仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §1.1)
 */
public enum Rank {
    KYU_20("20級"),
    KYU_19("19級"),
    KYU_18("18級"),
    KYU_17("17級"),
    KYU_16("16級"),
    KYU_15("15級"),
    KYU_14("14級"),
    KYU_13("13級"),
    KYU_12("12級"),
    KYU_11("11級"),
    KYU_10("10級"),
    KYU_9("9級"),
    KYU_8("8級"),
    KYU_7("7級"),
    KYU_6("6級"),
    KYU_5("5級"),
    KYU_4("4級"),
    KYU_3("3級"),
    KYU_2("2級"),
    KYU_1("1級"),
    DAN_1("初段"),
    DAN_2("二段"),
    DAN_3("三段"),
    DAN_4("四段"),
    DAN_5("五段"),
    DAN_6("六段"),
    DAN_7("七段"),
    DAN_8("八段"),
    DAN_9("九段");

    private final String displayName;

    Rank(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isStrongerThan(Rank other) {
        return compareTo(other) > 0;
    }

    /** 強い順の比較器。棋力未入力(null)は最弱(20級)よりさらに弱い扱いで末尾に並ぶ */
    public static Comparator<Rank> strongestFirst() {
        return Comparator.nullsLast(Comparator.reverseOrder());
    }
}
