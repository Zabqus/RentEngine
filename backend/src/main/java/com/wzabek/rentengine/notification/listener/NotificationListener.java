package com.wzabek.rentengine.notification.listener;

import com.wzabek.rentengine.notification.service.NotificationService;
import com.wzabek.rentengine.rental.event.AgreementOverdueEvent;
import com.wzabek.rentengine.rental.event.ReservationCreatedEvent;
import com.wzabek.rentengine.rental.event.ReservationExpiredEvent;
import com.wzabek.rentengine.settlement.event.SettlementCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// AFTER_COMMIT: mail idzie tylko wtedy, gdy zmiana naprawdę trafiła do bazy (rollback = brak maila).
// @Async: wysyłka SMTP nie wydłuża odpowiedzi HTTP i nie blokuje schedulera.
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener
    public void on(ReservationCreatedEvent event) {
        notificationService.reservationCreated(event.agreementId());
    }

    @Async
    @TransactionalEventListener
    public void on(AgreementOverdueEvent event) {
        notificationService.agreementOverdue(event.agreementId());
    }

    @Async
    @TransactionalEventListener
    public void on(ReservationExpiredEvent event) {
        notificationService.reservationExpired(event.agreementId());
    }

    @Async
    @TransactionalEventListener
    public void on(SettlementCreatedEvent event) {
        notificationService.settlementCreated(event.settlementId());
    }
}
