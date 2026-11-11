package com.wzabek.rentengine.rental.event;

// Scheduler anulował rezerwację, której nikt nie odebrał
public record ReservationExpiredEvent(Long agreementId) {
}
