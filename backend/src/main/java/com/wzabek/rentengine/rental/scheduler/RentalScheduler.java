package com.wzabek.rentengine.rental.scheduler;

import com.wzabek.rentengine.rental.service.RentalMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Działa tylko z @EnableScheduling (SchedulingConfig); harmonogram w app.scheduler.cron
@Component
@RequiredArgsConstructor
public class RentalScheduler {

    private final RentalMaintenanceService maintenanceService;

    @Scheduled(cron = "${app.scheduler.cron}")
    public void run() {
        maintenanceService.markOverdue();
        maintenanceService.cancelUnclaimedReservations();
    }
}
