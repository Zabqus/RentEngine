package com.wzabek.rentengine.catalog.mapper;

import com.wzabek.rentengine.catalog.dto.CategoryResponse;
import com.wzabek.rentengine.catalog.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                List.copyOf(category.getRequiredParams())
        );
    }
}
