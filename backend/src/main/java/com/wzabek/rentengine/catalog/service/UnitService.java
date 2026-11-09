package com.wzabek.rentengine.catalog.service;

import com.wzabek.rentengine.catalog.dto.UnitRequest;
import com.wzabek.rentengine.catalog.dto.UnitResponse;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.catalog.entity.UnitStatus;
import com.wzabek.rentengine.catalog.mapper.UnitMapper;
import com.wzabek.rentengine.catalog.repository.EquipmentUnitRepository;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitService {

    private static final Sort SORT = Sort.by("serialOrPlate");

    private final EquipmentUnitRepository unitRepository;
    private final RentalAgreementRepository agreementRepository;
    private final EquipmentService equipmentService;
    private final UnitMapper mapper;

    public List<UnitResponse> findAll(Long equipmentId) {
        List<EquipmentUnit> units = equipmentId == null
                ? unitRepository.findAll(SORT)
                : unitRepository.findByEquipmentId(equipmentService.getEquipment(equipmentId).getId(), SORT);
        return units.stream().map(mapper::toResponse).toList();
    }

    public UnitResponse findById(Long id) {
        return mapper.toResponse(getUnit(id));
    }

    @Transactional
    public UnitResponse create(UnitRequest request) {
        String serial = normalizeSerial(request.serialOrPlate());
        if (unitRepository.existsBySerialOrPlateValue(serial)) {
            throw serialTaken(serial);
        }
        EquipmentUnit unit = new EquipmentUnit(equipmentService.getEquipment(request.equipmentId()),
                serial, request.mileageOrHours());
        return mapper.toResponse(unitRepository.save(unit));
    }

    @Transactional
    public UnitResponse update(Long id, UnitRequest request) {
        EquipmentUnit unit = getUnit(id);
        String serial = normalizeSerial(request.serialOrPlate());
        if (unitRepository.existsBySerialOrPlateValueAndIdNot(serial, id)) {
            throw serialTaken(serial);
        }
        unit.setEquipment(equipmentService.getEquipment(request.equipmentId()));
        unit.setSerialOrPlate(serial);
        unit.setMileageOrHours(request.mileageOrHours());
        return mapper.toResponse(unit);
    }

    @Transactional
    public UnitResponse changeStatus(Long id, UnitStatus status) {
        EquipmentUnit unit = getUnit(id);
        // Sprzęt u klienta nie może trafić do serwisu; po zwrocie zrobi to protokół (R7) albo admin
        if (status == UnitStatus.IN_SERVICE
                && agreementRepository.existsByUnitIdAndStatusIn(id, List.of(AgreementStatus.ACTIVE, AgreementStatus.OVERDUE))) {
            throw new BusinessRuleException("UNIT_RENTED_OUT",
                    "Unit '%s' is currently rented out".formatted(unit.getSerialOrPlate()));
        }
        unit.setStatus(status);
        return mapper.toResponse(unit);
    }

    @Transactional
    public void delete(Long id) {
        EquipmentUnit unit = getUnit(id);
        // Umowy muszą zostać w historii, więc egzemplarza z umowami nie kasujemy
        if (agreementRepository.existsByUnitId(id)) {
            throw new BusinessRuleException("UNIT_HAS_AGREEMENTS",
                    "Unit '%s' has rental agreements".formatted(unit.getSerialOrPlate()));
        }
        unitRepository.delete(unit);
    }

    public EquipmentUnit getUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unit", id));
    }

    // "we 1234a" i "WE 1234A" to ta sama tablica
    private static String normalizeSerial(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static BusinessRuleException serialTaken(String serial) {
        return new BusinessRuleException("UNIT_SERIAL_TAKEN", "Unit '%s' already exists".formatted(serial));
    }
}
