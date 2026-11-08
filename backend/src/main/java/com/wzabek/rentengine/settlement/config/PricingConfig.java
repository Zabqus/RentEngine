package com.wzabek.rentengine.settlement.config;

import com.wzabek.rentengine.settlement.pricing.PricingCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingConfig {

    @Bean
    PricingCalculator pricingCalculator() {
        return new PricingCalculator();
    }
}
