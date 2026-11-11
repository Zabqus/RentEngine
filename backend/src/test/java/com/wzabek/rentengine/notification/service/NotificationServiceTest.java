package com.wzabek.rentengine.notification.service;

import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.CategoryType;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.notification.config.MailProperties;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.settlement.entity.Settlement;
import com.wzabek.rentengine.settlement.repository.SettlementRepository;
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 6, 1, 10, 0);

    @Mock
    private RentalAgreementRepository agreementRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private JavaMailSender mailSender;

    private RentalAgreement agreement;

    @BeforeEach
    void setUp() {
        Category category = new Category("Minikoparki", CategoryType.EXCAVATOR, List.of());
        Equipment equipment = new Equipment("Kubota U17-3", category, new BigDecimal("450.00"),
                new BigDecimal("3000.00"), null, Map.of());
        EquipmentUnit unit = new EquipmentUnit(equipment, "KUB-1", 100);
        User client = new User("jan@test.pl", "hash", "Jan", "Kowalski", Role.CLIENT);
        agreement = new RentalAgreement(client, unit, START, START.plusDays(2),
                new BigDecimal("900.00"), new BigDecimal("3000.00"));
        ReflectionTestUtils.setField(agreement, "id", 10L);
    }

    private NotificationService service(boolean enabled) {
        return new NotificationService(agreementRepository, settlementRepository, mailSender,
                new MailProperties("no-reply@rentengine.local", enabled));
    }

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void reservationMailContainsDetails() {
        when(agreementRepository.findById(10L)).thenReturn(Optional.of(agreement));

        service(true).reservationCreated(10L);

        SimpleMailMessage mail = sentMessage();
        assertThat(mail.getTo()).containsExactly("jan@test.pl");
        assertThat(mail.getFrom()).isEqualTo("no-reply@rentengine.local");
        assertThat(mail.getSubject()).isEqualTo("RentEngine: Potwierdzenie rezerwacji nr 10");
        assertThat(mail.getText()).contains("Kubota U17-3", "KUB-1", "01.06.2030 10:00", "900.00 zł", "10% stawki");
    }

    @Test
    void settlementMailShowsAmountsAndDepositRefund() {
        Settlement settlement = new Settlement(agreement, new BigDecimal("900.00"), new BigDecimal("45.00"),
                new BigDecimal("42.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("500.00"), new BigDecimal("987.00"));
        when(settlementRepository.findById(5L)).thenReturn(Optional.of(settlement));

        service(true).settlementCreated(5L);

        assertThat(sentMessage().getText())
                .contains("Do zapłaty: 987.00 zł", "potrącono 500.00 zł", "do zwrotu: 2500.00 zł");
    }

    @Test
    void smtpFailureIsSwallowed() {
        when(agreementRepository.findById(10L)).thenReturn(Optional.of(agreement));
        doThrow(new MailSendException("connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service(true).agreementOverdue(10L)).doesNotThrowAnyException();
    }

    @Test
    void disabledMailIsOnlyLogged() {
        when(agreementRepository.findById(10L)).thenReturn(Optional.of(agreement));

        service(false).reservationExpired(10L);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
