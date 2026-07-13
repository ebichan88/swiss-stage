package com.swiss_stage.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 時刻はClockをDIする(テストで固定Clockに差し替えるため。Instant.now()直呼び禁止) */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
