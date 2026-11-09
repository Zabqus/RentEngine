package com.wzabek.rentengine.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Wstrzykiwany zegar, żeby w testach dało się sterować czasem (np. wygasanie tokena)
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
