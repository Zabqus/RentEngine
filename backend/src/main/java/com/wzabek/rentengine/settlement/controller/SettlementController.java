package com.wzabek.rentengine.settlement.controller;

import com.wzabek.rentengine.settlement.dto.SettlementResponse;
import com.wzabek.rentengine.settlement.service.SettlementService;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Tag(name = "Settlements")
@RequestMapping("/api")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping("/rentals/{rentalId}/settlement")
    public ResponseEntity<SettlementResponse> create(@PathVariable Long rentalId) {
        SettlementResponse created = settlementService.create(rentalId);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri()).body(created);
    }

    // Klient widzi rozliczenie swojej umowy, pracownik i admin wszystkie
    @GetMapping("/rentals/{rentalId}/settlement")
    public SettlementResponse findByAgreement(@PathVariable Long rentalId,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        return settlementService.findByAgreement(rentalId, user);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @PatchMapping("/settlements/{id}/pay")
    public SettlementResponse pay(@PathVariable Long id) {
        return settlementService.pay(id);
    }
}
