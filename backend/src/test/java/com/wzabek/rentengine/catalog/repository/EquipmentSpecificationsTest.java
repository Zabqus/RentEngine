package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.TestcontainersConfiguration;
import com.wzabek.rentengine.catalog.dto.EquipmentFilter;
import com.wzabek.rentengine.catalog.entity.CategoryType;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Filtry katalogu na danych z seeda (8 modeli sprzętu); każdy test w transakcji wycofywanej po teście
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EquipmentSpecificationsTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final LocalDateTime TO = FROM.plusDays(3);

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TestEntityManager em;

    private Page<Equipment> find(EquipmentFilter filter) {
        return equipmentRepository.findAll(EquipmentSpecifications.matching(filter),
                PageRequest.of(0, 20, Sort.by("name")));
    }

    private static EquipmentFilter filter(Long categoryId, String q, String min, String max,
                                          LocalDateTime from, LocalDateTime to) {
        return new EquipmentFilter(categoryId, q, min == null ? null : new BigDecimal(min),
                max == null ? null : new BigDecimal(max), from, to);
    }

    private Long categoryId(String type) {
        return em.getEntityManager()
                .createQuery("select c.id from Category c where c.type = :type", Long.class)
                .setParameter("type", CategoryType.valueOf(type))
                .getSingleResult();
    }

    private EquipmentUnit unit(String serial) {
        return em.getEntityManager()
                .createQuery("select u from EquipmentUnit u where u.serialOrPlate = :s", EquipmentUnit.class)
                .setParameter("s", serial)
                .getSingleResult();
    }

    private RentalAgreement book(String serial, LocalDateTime start, LocalDateTime end) {
        RentalAgreement agreement = new RentalAgreement(em.find(User.class, 1L), unit(serial), start, end,
                new BigDecimal("100.00"), new BigDecimal("100.00"));
        return em.persistAndFlush(agreement);
    }

    @Test
    void emptyFilterReturnsWholeCatalog() {
        assertThat(find(filter(null, null, null, null, null, null)).getTotalElements()).isEqualTo(8);
    }

    @Test
    void filtersByCategory() {
        assertThat(find(filter(categoryId("EXCAVATOR"), null, null, null, null, null)))
                .extracting(Equipment::getName)
                .containsExactly("Bobcat E35z", "Kubota U17-3");
    }

    @Test
    void searchesNameCaseInsensitivelyAndTreatsWildcardsLiterally() {
        assertThat(find(filter(null, "  kUBOTA ", null, null, null, null))).extracting(Equipment::getName)
                .containsExactly("Kubota U17-3");
        assertThat(find(filter(null, "%", null, null, null, null))).isEmpty();
    }

    @Test
    void filtersByPriceRangeInclusive() {
        assertThat(find(filter(null, null, "140.00", "189.00", null, null))).extracting(Equipment::getName)
                .containsExactlyInAnyOrder("Toyota Corolla 1.8 Hybrid", "Husqvarna TC 242TX",
                        "Zagęszczarka Wacker Neuson DPU 2560");
    }

    @Test
    void combinesFilters() {
        assertThat(find(filter(categoryId("CAR"), "octavia", null, "200.00", null, null))).isEmpty();
        assertThat(find(filter(categoryId("CAR"), "octavia", null, "250.00", null, null))).hasSize(1);
    }

    @Test
    void equipmentWithOnlyBookedOrServicedUnitsIsNotAvailable() {
        // Kubota ma 2 egzemplarze: 0001 zarezerwujemy, 0002 jest w serwisie
        book("KUB-U17-0001", FROM.minusDays(1), FROM.plusDays(1));
        EquipmentFilter period = filter(categoryId("EXCAVATOR"), null, null, null, FROM, TO);

        assertThat(find(period)).extracting(Equipment::getName).containsExactly("Bobcat E35z");
    }

    @Test
    void bookingEndingAtStartOfPeriodDoesNotBlock() {
        book("KUB-U17-0001", FROM.minusDays(2), FROM);

        assertThat(find(filter(categoryId("EXCAVATOR"), null, null, null, FROM, TO))).hasSize(2);
    }

    @Test
    void cancelledBookingDoesNotBlockButOverdueBlocksForever() {
        RentalAgreement cancelled = book("KUB-U17-0001", FROM, TO);
        cancelled.changeStatus(AgreementStatus.CANCELLED);
        em.flush();
        assertThat(find(filter(categoryId("EXCAVATOR"), null, null, null, FROM, TO))).hasSize(2);

        RentalAgreement overdue = book("KUB-U17-0001", FROM.minusDays(30), FROM.minusDays(29));
        overdue.changeStatus(AgreementStatus.ACTIVE);
        overdue.changeStatus(AgreementStatus.OVERDUE);
        em.flush();
        assertThat(find(filter(categoryId("EXCAVATOR"), null, null, null, FROM, TO)))
                .extracting(Equipment::getName).containsExactly("Bobcat E35z");
    }

    @Test
    void pagesThroughResults() {
        Page<Equipment> second = equipmentRepository.findAll(
                EquipmentSpecifications.matching(filter(null, null, null, null, null, null)),
                PageRequest.of(1, 3, Sort.by("name")));

        assertThat(second.getTotalElements()).isEqualTo(8);
        assertThat(second.getTotalPages()).isEqualTo(3);
        assertThat(second.getContent()).hasSize(3);
    }
}
