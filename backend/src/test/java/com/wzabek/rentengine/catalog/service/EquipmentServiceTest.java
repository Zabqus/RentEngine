package com.wzabek.rentengine.catalog.service;

import com.wzabek.rentengine.catalog.dto.EquipmentFilter;
import com.wzabek.rentengine.catalog.mapper.EquipmentMapper;
import com.wzabek.rentengine.catalog.repository.EquipmentRepository;
import com.wzabek.rentengine.catalog.repository.EquipmentUnitRepository;
import com.wzabek.rentengine.common.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final EquipmentFilter NO_FILTER = new EquipmentFilter(null, null, null, null, null, null);

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private EquipmentUnitRepository unitRepository;
    @Mock
    private CategoryService categoryService;

    private EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(equipmentRepository, unitRepository, categoryService, new EquipmentMapper());
    }

    private void assertRejected(EquipmentFilter filter, Pageable pageable, String field) {
        assertThatThrownBy(() -> service.findAll(filter, pageable))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errors").asInstanceOf(MAP).containsKey(field);
        verify(equipmentRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void minPriceAboveMaxPriceIsRejected() {
        assertRejected(new EquipmentFilter(null, null, new BigDecimal("500"), new BigDecimal("100"), null, null),
                PageRequest.of(0, 20), "maxPrice");
    }

    @Test
    void periodNeedsBothEnds() {
        assertRejected(new EquipmentFilter(null, null, null, null, FROM, null), PageRequest.of(0, 20), "to");
    }

    @Test
    void periodMustEndAfterStart() {
        assertRejected(new EquipmentFilter(null, null, null, null, FROM, FROM), PageRequest.of(0, 20), "to");
    }

    @Test
    void unknownSortPropertyIsRejected() {
        assertRejected(NO_FILTER, PageRequest.of(0, 20, Sort.by("passwordHash")), "sort");
    }

    @SuppressWarnings("unchecked")
    @Test
    void idIsAddedAsTieBreakerSort() {
        when(equipmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service.findAll(NO_FILTER, PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "dailyPrice")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(equipmentRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getSort())
                .containsExactly(Sort.Order.desc("dailyPrice"), Sort.Order.asc("id"));
    }
}
