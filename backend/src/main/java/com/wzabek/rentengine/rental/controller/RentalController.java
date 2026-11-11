package com.wzabek.rentengine.rental.controller;

import com.wzabek.rentengine.rental.dto.RentalRequest;
import com.wzabek.rentengine.rental.dto.RentalResponse;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.service.RentalService;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Rentals")
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<RentalResponse> reserve(@Valid @RequestBody RentalRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        RentalResponse created = rentalService.reserve(request, user.id());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    @GetMapping
    public List<RentalResponse> findAll(@RequestParam(required = false) AgreementStatus status) {
        return rentalService.findAll(status);
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my")
    public List<RentalResponse> findMy(@AuthenticationPrincipal AuthenticatedUser user) {
        return rentalService.findByClient(user.id());
    }

    // Klient widzi tylko swoje umowy, pracownik i admin wszystkie (sprawdzane w serwisie)
    @GetMapping("/{id}")
    public RentalResponse findById(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return rentalService.findById(id, user);
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @PatchMapping("/{id}/cancel")
    public RentalResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return rentalService.cancel(id, user);
    }
}
