package com.wzabek.rentengine.catalog.service;

import com.wzabek.rentengine.catalog.dto.CategoryRequest;
import com.wzabek.rentengine.catalog.dto.CategoryResponse;
import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.mapper.CategoryMapper;
import com.wzabek.rentengine.catalog.repository.CategoryRepository;
import com.wzabek.rentengine.catalog.repository.EquipmentRepository;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EquipmentRepository equipmentRepository;
    private final CategoryMapper mapper;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return mapper.toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw nameTaken(name);
        }
        Category category = new Category(name, request.type(), normalize(request.requiredParams()));
        return mapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw nameTaken(name);
        }
        List<String> requiredParams = normalize(request.requiredParams());
        ensureExistingEquipmentHasParams(id, requiredParams);

        category.setName(name);
        category.setType(request.type());
        category.setRequiredParams(requiredParams);
        return mapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (equipmentRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("CATEGORY_IN_USE",
                    "Category '%s' still contains equipment".formatted(category.getName()));
        }
        categoryRepository.delete(category);
    }

    // Używane też przez EquipmentService
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));
    }

    // Nowy wymagany parametr nie może unieważnić sprzętu, który już jest w kategorii
    private void ensureExistingEquipmentHasParams(Long categoryId, List<String> requiredParams) {
        List<String> invalid = equipmentRepository.findByCategoryId(categoryId).stream()
                .filter(e -> !e.getParams().keySet().containsAll(requiredParams))
                .map(Equipment::getName)
                .toList();
        if (!invalid.isEmpty()) {
            throw new BusinessRuleException("CATEGORY_PARAMS_CONFLICT",
                    "Equipment missing new required params: " + String.join(", ", invalid));
        }
    }

    private static List<String> normalize(List<String> params) {
        return new ArrayList<>(new LinkedHashSet<>(params.stream().map(String::trim).toList()));
    }

    private static BusinessRuleException nameTaken(String name) {
        return new BusinessRuleException("CATEGORY_NAME_TAKEN", "Category '%s' already exists".formatted(name));
    }
}
