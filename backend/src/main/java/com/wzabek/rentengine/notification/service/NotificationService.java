package com.wzabek.rentengine.notification.service;

import com.wzabek.rentengine.notification.config.MailProperties;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.settlement.entity.Settlement;
import com.wzabek.rentengine.settlement.repository.SettlementRepository;
import com.wzabek.rentengine.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Treść maili do klienta (po polsku, bo czyta je klient wypożyczalni, a nie frontend).
// Wołane asynchronicznie z NotificationListener, już po commicie transakcji, która zmieniła dane.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RentalAgreementRepository agreementRepository;
    private final SettlementRepository settlementRepository;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void reservationCreated(Long agreementId) {
        RentalAgreement a = agreement(agreementId);
        send(a.getClient(), "Potwierdzenie rezerwacji nr " + a.getId(), """
                Dzień dobry %s,

                potwierdzamy rezerwację nr %d.

                Sprzęt: %s (%s)
                Termin: %s – %s
                Cena za wynajem: %s
                Kaucja: %s

                Za każdą rozpoczętą dobę spóźnienia doliczamy 10%% stawki dobowej.
                """.formatted(a.getClient().getFirstName(), a.getId(), equipment(a), a.getUnit().getSerialOrPlate(),
                date(a.getStartDate()), date(a.getEndDate()), money(a.getBasePrice()), money(a.getDeposit())));
    }

    public void agreementOverdue(Long agreementId) {
        RentalAgreement a = agreement(agreementId);
        send(a.getClient(), "Minął termin zwrotu – umowa nr " + a.getId(), """
                Dzień dobry %s,

                termin zwrotu sprzętu %s (%s) minął %s.
                Prosimy o jak najszybszy zwrot. Za każdą rozpoczętą dobę spóźnienia doliczamy 10%% stawki dobowej.
                """.formatted(a.getClient().getFirstName(), equipment(a), a.getUnit().getSerialOrPlate(),
                date(a.getEndDate())));
    }

    public void reservationExpired(Long agreementId) {
        RentalAgreement a = agreement(agreementId);
        send(a.getClient(), "Rezerwacja nr " + a.getId() + " została anulowana", """
                Dzień dobry %s,

                rezerwacja nr %d na sprzęt %s (od %s) została anulowana, bo sprzęt nie został odebrany.
                Zapraszamy do złożenia nowej rezerwacji.
                """.formatted(a.getClient().getFirstName(), a.getId(), equipment(a), date(a.getStartDate())));
    }

    public void settlementCreated(Long settlementId) {
        Settlement s = settlementRepository.findById(settlementId).orElse(null);
        if (s == null) {
            log.warn("Settlement {} not found, mail skipped", settlementId);
            return;
        }
        RentalAgreement a = s.getAgreement();
        send(a.getClient(), "Rozliczenie umowy nr " + a.getId(), """
                Dzień dobry %s,

                przygotowaliśmy rozliczenie umowy nr %d (%s).

                Wynajem: %s
                Kara za spóźnienie: %s
                Brakujące paliwo: %s
                Szkody ponad kaucję: %s
                Do zapłaty: %s

                Z kaucji %s potrącono %s na poczet szkód, do zwrotu: %s.
                Do czasu opłacenia rozliczenia nie można składać nowych rezerwacji.
                """.formatted(a.getClient().getFirstName(), a.getId(), equipment(a),
                money(s.getBaseAmount()), money(s.getLateFee()), money(s.getFuelFee()), money(s.getDamageFee()),
                money(s.getTotal()), money(a.getDeposit()), money(s.getDepositDeduction()),
                money(a.getDeposit().subtract(s.getDepositDeduction()))));
    }

    private RentalAgreement agreement(Long id) {
        return agreementRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Rental agreement %d not found".formatted(id)));
    }

    // Błąd SMTP nie może wpłynąć na operację biznesową, która już się wykonała, więc tylko logujemy
    private void send(User to, String subject, String text) {
        if (!mailProperties.enabled()) {
            log.info("Mail disabled, skipping '{}' to {}", subject, to.getEmail());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(to.getEmail());
        message.setSubject("RentEngine: " + subject);
        message.setText(text);
        try {
            mailSender.send(message);
            log.info("Mail '{}' sent to {}", subject, to.getEmail());
        } catch (MailException e) {
            log.warn("Cannot send mail '{}' to {}: {}", subject, to.getEmail(), e.getMessage());
        }
    }

    private static String equipment(RentalAgreement a) {
        return a.getUnit().getEquipment().getName();
    }

    private static String date(LocalDateTime value) {
        return DATE.format(value);
    }

    private static String money(BigDecimal value) {
        return value.setScale(2) + " zł";
    }
}
