package com.wzabek.rentengine.rental.event;

// Scheduler oznaczył umowę jako OVERDUE
public record AgreementOverdueEvent(Long agreementId) {
}
