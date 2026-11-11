package com.wzabek.rentengine.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Wyłączane w testach (app.scheduler.enabled=false), żeby zadania nie zmieniały danych w trakcie testu
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
