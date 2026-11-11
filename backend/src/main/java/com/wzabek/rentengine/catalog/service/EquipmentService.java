package com.wzabek.rentengine.catalog.service;

import com.wzabek.rentengine.catalog.dto.EquipmentFilter;
import com.wzabek.rentengine.catalog.dto.EquipmentRequest;
import com.wzabek.rentengine.catalog.dto.EquipmentResponse;
import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.mapper.EquipmentMapper;
import com.wzabek.rentengine.catalog.repository.EquipmentRepository;
import com.wzabek.rentengine.catalog.repository.EquipmentSpecifications;
import com.wzabek.rentengine.catalog.repository.EquipmentUnitRepository;
import com.wzabek.rentengine.common.dto.PageResponse;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.InvalidRequestException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private static final List<String> SORTABLE = List.of("name", "dailyPrice", "deposit", "id");

    private final EquipmentRepository equipmentRepository;
    private final EquipmentUnitRepository unitRepository;
    private final CategoryService categoryService;
    private final EquipmentMapper mapper;

    public PageResponse<EquipmentResponse> findAll(EquipmentFilter filter, Pageable pageable) {
        validateFilter(filter, pageable.getSort());
        // Dosortowanie po id, żeby kolejność między stronami była stabilna przy równych nazwach/cenach
        Pageable stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
        return PageResponse.from(equipmentRepository.findAll(EquipmentSpecifications.matching(filter), stable),
                mapper::toResponse);
    }

    public EquipmentResponse findById(Long id) {
        return mapper.toResponse(getEquipment(id));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        Category category = categoryService.getCategory(request.categoryId());
        validateParams(category, request.params());

        Equipment equipment = new Equipment(request.name().trim(), category, money(request.dailyPrice()),
                money(request.deposit()), money(request.tankCapacity()), new LinkedHashMap<>(request.params()));
        return mapper.toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = getEquipment(id);
        Category category = categoryService.getCategory(request.categoryId());
        validateParams(category, request.params());

        equipment.setName(request.name().trim());
        equipment.setCategory(category);
        equipment.setDailyPrice(money(request.dailyPrice()));
        equipment.setDeposit(money(request.deposit()));
        equipment.setTankCapacity(money(request.tankCapacity()));
        equipment.setParams(new LinkedHashMap<>(request.params()));
        return mapper.toResponse(equipment);
    }

    @Transactional
    public void delete(Long id) {
        Equipment equipment = getEquipment(id);
        if (unitRepository.existsByEquipmentId(id)) {
            throw new BusinessRuleException("EQUIPMENT_HAS_UNITS",
                    "Equipment '%s' still has units".formatted(equipment.getName()));
        }
        equipmentRepository.delete(equipment);
    }

    // Używane też przez UnitService
    public Equipment getEquipment(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment", id));
    }

    private static void validateFilter(EquipmentFilter filter, Sort sort) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            errors.put("maxPrice", "must be greater than or equal to minPrice");
        }
        if ((filter.from() == null) != (filter.to() == null)) {
            errors.put(filter.from() == null ? "from" : "to", "from and to must be provided together");
        } else if (filter.from() != null && !filter.to().isAfter(filter.from())) {
            errors.put("to", "must be after from");
        }
        // Nieznane pole sortowania skończyłoby się błędem 500 w zapytaniu
        sort.stream().filter(order -> !SORTABLE.contains(order.getProperty())).findFirst().ifPresent(order ->
                errors.put("sort", "unsupported property '%s', allowed: %s".formatted(order.getProperty(), SORTABLE)));
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }

    // @Digits gwarantuje max 2 miejsca po przecinku, więc setScale niczego nie zaokrągla
    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2);
    }

    // Każdy parametr wymagany przez kategorię musi być podany i niepusty; dodatkowe są dozwolone
    private void validateParams(Category category, Map<String, Object> params) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (String key : category.getRequiredParams()) {
            Object value = params.get(key);
            if (value == null || (value instanceof String s && s.isBlank())) {
                errors.put("params." + key, "is required for category '%s'".formatted(category.getName()));
            }
        }
        params.forEach((key, value) -> {
            if (value == null && !errors.containsKey("params." + key)) {
                errors.put("params." + key, "must not be null");
            }
        });
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }
}
