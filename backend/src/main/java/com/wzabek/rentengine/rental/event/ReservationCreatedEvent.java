package com.wzabek.rentengine.rental.event;

// Publikowany po zapisaniu rezerwacji; mail wysyła listener po commicie
public record ReservationCreatedEvent(Long agreementId) {
}
