package com.swiss_stage.domain.model;

import java.util.Comparator;

/**
 * 棋力(段級位)。囲碁・将棋共通の29段階。最弱は20級、最強は9段。
 * 1級の次は初段(DAN_1)であり「1段」は存在しない。
 * (仕様: .claude/01_development_docs/05_swiss_pairing_algorithm.md §1.1)
 *
 * <p>強さの比較は宣言順(ordinal)ではなく sortOrder(小さいほど強い。段=負、級=正)で行う。
 * 将来定数を追加・並べ替えしても強さの定義が壊れないようにするため。
 * 宣言順と sortOrder の整合はテストで検証している。
 */
public enum Rank {
    KYU_20("20級", 20),
    KYU_19("19級", 19),
    KYU_18("18級", 18),
    KYU_17("17級", 17),
    KYU_16("16級", 16),
    KYU_15("15級", 15),
    KYU_14("14級", 14),
    KYU_13("13級", 13),
    KYU_12("12級", 12),
    KYU_11("11級", 11),
    KYU_10("10級", 10),
    KYU_9("9級", 9),
    KYU_8("8級", 8),
    KYU_7("7級", 7),
    KYU_6("6級", 6),
    KYU_5("5級", 5),
    KYU_4("4級", 4),
    KYU_3("3級", 3),
    KYU_2("2級", 2),
    KYU_1("1級", 1),
    DAN_1("初段", -1),
    DAN_2("2段", -2),
    DAN_3("3段", -3),
    DAN_4("4段", -4),
    DAN_5("5段", -5),
    DAN_6("6段", -6),
    DAN_7("7段", -7),
    DAN_8("8段", -8),
    DAN_9("9段", -9);

    private final String displayName;

    /** ソート優先度。小さいほど強い(9段=-9 … 初段=-1、1級=1 … 20級=20) */
    private final int sortOrder;

    Rank(String displayName, int sortOrder) {
        this.displayName = displayName;
        this.sortOrder = sortOrder;
    }

    public String displayName() {
        return displayName;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public boolean isStrongerThan(Rank other) {
        return sortOrder < other.sortOrder;
    }

    /** 強い順の比較器。棋力未入力(null)は最弱(20級)よりさらに弱い扱いで末尾に並ぶ */
    public static Comparator<Rank> strongestFirst() {
        return Comparator.nullsLast(Comparator.comparingInt(Rank::sortOrder));
    }
}
