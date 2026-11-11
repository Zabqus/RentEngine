package com.wzabek.rentengine.protocol.controller;

import com.wzabek.rentengine.protocol.dto.DamageRequest;
import com.wzabek.rentengine.protocol.dto.DamageResponse;
import com.wzabek.rentengine.protocol.dto.ProtocolRequest;
import com.wzabek.rentengine.protocol.dto.ProtocolResponse;
import com.wzabek.rentengine.protocol.service.ProtocolService;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Protocols")
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProtocolController {

    private final ProtocolService protocolService;

    // Klient widzi protokoły swoich umów, pracownik i admin wszystkie (sprawdzane w serwisie)
    @GetMapping("/rentals/{rentalId}/protocols")
    public List<ProtocolResponse> findByAgreement(@PathVariable Long rentalId,
                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return protocolService.findByAgreement(rentalId, user);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping("/rentals/{rentalId}/protocols")
    public ResponseEntity<ProtocolResponse> create(@PathVariable Long rentalId,
                                                   @Valid @RequestBody ProtocolRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(protocolService.create(rentalId, request, user.id()));
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping("/protocols/{protocolId}/damages")
    public ResponseEntity<DamageResponse> addDamage(@PathVariable Long protocolId,
                                                    @Valid @RequestBody DamageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(protocolService.addDamage(protocolId, request));
    }
}
