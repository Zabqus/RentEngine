package com.wzabek.rentengine.catalog.controller;

import com.wzabek.rentengine.catalog.dto.UnitRequest;
import com.wzabek.rentengine.catalog.dto.UnitResponse;
import com.wzabek.rentengine.catalog.dto.UnitStatusRequest;
import com.wzabek.rentengine.catalog.service.UnitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Units")
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public List<UnitResponse> findAll(@RequestParam(required = false) Long equipmentId) {
        return unitService.findAll(equipmentId);
    }

    @GetMapping("/{id}")
    public UnitResponse findById(@PathVariable Long id) {
        return unitService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UnitResponse> create(@Valid @RequestBody UnitRequest request) {
        UnitResponse created = unitService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UnitResponse update(@PathVariable Long id, @Valid @RequestBody UnitRequest request) {
        return unitService.update(id, request);
    }

    // Pracownik też może wysłać sprzęt do serwisu i z niego zwrócić
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @PatchMapping("/{id}/status")
    public UnitResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UnitStatusRequest request) {
        return unitService.changeStatus(id, request.status());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
