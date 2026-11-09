package com.wzabek.rentengine.rental.service;

import com.wzabek.rentengine.TestcontainersConfiguration;
import com.wzabek.rentengine.rental.dto.RentalRequest;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RentalConcurrencyTest {

    private static final long CLIENT_ID = 1L;
    private static final long CLIENT2_ID = 2L;
    private static final long UNIT_ID = 1L;

    @Autowired
    private RentalService rentalService;
    @Autowired
    private RentalAgreementRepository agreementRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        agreementRepository.deleteAll();
    }

    @Test
    void secondConcurrentReservationOfSameUnitFails() {
        LocalDateTime start = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.HOURS);

        // Transakcja A rezerwuje, ale zanim zrobi commit, transakcja B (inny wątek) rezerwuje i commituje.
        // Obie przeszły sprawdzenie kolizji (R1), więc chroni nas tylko @Version egzemplarza.
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            rentalService.reserve(new RentalRequest(CLIENT_ID, UNIT_ID, start, start.plusDays(2)));
            CompletableFuture.runAsync(() ->
                    rentalService.reserve(new RentalRequest(CLIENT2_ID, UNIT_ID, start.plusDays(1), start.plusDays(3)))
            ).join();
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(agreementRepository.count()).isEqualTo(1);
    }
}
