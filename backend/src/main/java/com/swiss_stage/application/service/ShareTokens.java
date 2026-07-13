package com.swiss_stage.application.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 共有トークンの生成(13_security_design.md §2)。
 * SecureRandom 32バイトをURL-safe Base64化した43文字(推測不能・URLにそのまま使える)。
 */
public final class ShareTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private ShareTokens() {}

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
