package com.wzabek.rentengine.catalog.controller;

import com.wzabek.rentengine.catalog.dto.EquipmentFilter;
import com.wzabek.rentengine.catalog.dto.EquipmentRequest;
import com.wzabek.rentengine.catalog.dto.EquipmentResponse;
import com.wzabek.rentengine.catalog.service.EquipmentService;
import com.wzabek.rentengine.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@Tag(name = "Equipment")
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    // Np. ?categoryId=2&q=kubota&minPrice=100&maxPrice=500&from=2027-01-10T10:00&to=2027-01-12T10:00&page=0&size=20&sort=dailyPrice,desc
    @GetMapping
    public PageResponse<EquipmentResponse> findAll(@Valid @ParameterObject EquipmentFilter filter,
                                                   @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return equipmentService.findAll(filter, pageable);
    }

    @GetMapping("/{id}")
    public EquipmentResponse findById(@PathVariable Long id) {
        return equipmentService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse created = equipmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
