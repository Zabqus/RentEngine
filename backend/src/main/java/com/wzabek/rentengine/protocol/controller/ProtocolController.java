package com.wzabek.rentengine.protocol.controller;

import com.wzabek.rentengine.protocol.dto.DamagePhoto;
import com.wzabek.rentengine.protocol.dto.DamageRequest;
import com.wzabek.rentengine.protocol.dto.DamageResponse;
import com.wzabek.rentengine.protocol.dto.ProtocolRequest;
import com.wzabek.rentengine.protocol.dto.ProtocolResponse;
import com.wzabek.rentengine.protocol.service.ProtocolService;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // JPEG/PNG do 5 MB; nowe zdjęcie zastępuje poprzednie
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PutMapping(path = "/damages/{damageId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DamageResponse uploadPhoto(@PathVariable Long damageId, @RequestPart("photo") MultipartFile photo) {
        return protocolService.uploadPhoto(damageId, photo);
    }

    // Klient widzi zdjęcia ze swoich umów, pracownik i admin wszystkie
    @GetMapping("/damages/{damageId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long damageId,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        DamagePhoto photo = protocolService.loadPhoto(damageId, user);
        return ResponseEntity.ok()
                .contentType(photo.mediaType())
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(photo.resource());
    }
}
