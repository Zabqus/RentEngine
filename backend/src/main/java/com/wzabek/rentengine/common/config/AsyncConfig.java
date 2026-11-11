package com.wzabek.rentengine.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @Async używa puli wątków z autokonfiguracji Springa (applicationTaskExecutor, wątki "async-")
@Configuration
@EnableAsync
public class AsyncConfig {
}
