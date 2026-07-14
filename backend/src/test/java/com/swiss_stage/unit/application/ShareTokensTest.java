package com.swiss_stage.unit.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.swiss_stage.application.service.ShareTokens;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShareTokensTest {

    @Test
    @DisplayName("32文字以上のURL-safeなトークンが生成され、重複しない(13_security_design.md §2)")
    void トークン生成() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = ShareTokens.generate();
            assertThat(token)
                    .hasSizeGreaterThanOrEqualTo(32)
                    .matches("[A-Za-z0-9_-]+");
            tokens.add(token);
        }
        assertThat(tokens).hasSize(1000);
    }
}
