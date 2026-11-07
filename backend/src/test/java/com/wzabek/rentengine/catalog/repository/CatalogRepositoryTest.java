package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.TestcontainersConfiguration;
import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.CategoryType;
import com.wzabek.rentengine.catalog.entity.Equipment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CatalogRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void seedLoadsCategoriesWithRequiredParams() {
        assertThat(categoryRepository.findAll()).hasSize(5);
        assertThat(categoryRepository.findAll())
                .filteredOn(c -> c.getType() == CategoryType.EXCAVATOR)
                .singleElement()
                .satisfies(c -> assertThat(c.getRequiredParams()).containsExactly("weightKg", "digDepthM", "powerKw"));
    }

    @Test
    void jsonbParamsRoundTrip() {
        Category category = categoryRepository.save(new Category("Test", CategoryType.MOWER, List.of("powerHp")));
        Equipment saved = equipmentRepository.save(new Equipment("Kosiarka testowa", category,
                new BigDecimal("100.00"), new BigDecimal("500.00"), null, Map.of("powerHp", 20, "color", "red")));
        em.flush();
        em.clear();

        Equipment loaded = equipmentRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getParams()).containsEntry("powerHp", 20).containsEntry("color", "red");
    }
}
