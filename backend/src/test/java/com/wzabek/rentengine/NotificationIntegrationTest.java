package com.wzabek.rentengine;

import com.jayway.jsonpath.JsonPath;
import com.wzabek.rentengine.catalog.repository.EquipmentUnitRepository;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.rental.service.RentalMaintenanceService;
import com.wzabek.rentengine.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Event → listener po commicie → @Async → mail; SMTP zastąpiony mockiem JavaMailSender
@SpringBootTest(properties = "app.mail.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationIntegrationTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RentalAgreementRepository agreementRepository;
    @Autowired
    private EquipmentUnitRepository unitRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RentalMaintenanceService maintenanceService;

    @AfterEach
    void cleanUp() {
        agreementRepository.deleteAll();
    }

    @Test
    void reservationConfirmationIsSentAsynchronouslyAfterCommit() throws Exception {
        AtomicReference<String> thread = new AtomicReference<>();
        doAnswer(inv -> {
            thread.set(Thread.currentThread().getName());
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        reserve().andExpect(status().isCreated());

        SimpleMailMessage mail = sentMail();
        assertThat(mail.getTo()).containsExactly("client@rentengine.local");
        assertThat(mail.getSubject()).startsWith("RentEngine: Potwierdzenie rezerwacji nr");
        assertThat(thread.get()).startsWith("async-");
    }

    @Test
    void rejectedReservationSendsNoMail() throws Exception {
        reserve().andExpect(status().isCreated());
        reserve().andExpect(status().isConflict());

        // Tylko jeden mail: druga rezerwacja się nie zapisała, więc jej event nie przeszedł AFTER_COMMIT
        verify(mailSender, after(1000).times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void schedulerMarksOverdueAgreementAndNotifiesClient() {
        LocalDateTime start = LocalDateTime.now().minusDays(3).truncatedTo(ChronoUnit.MINUTES);
        RentalAgreement agreement = new RentalAgreement(userRepository.findById(1L).orElseThrow(),
                unitRepository.findById(3L).orElseThrow(), start, start.plusDays(2),
                new BigDecimal("438.00"), new BigDecimal("1500.00"));
        agreement.changeStatus(AgreementStatus.ACTIVE);
        Long id = agreementRepository.save(agreement).getId();

        assertThat(maintenanceService.markOverdue()).isEqualTo(1);

        assertThat(agreementRepository.findById(id).orElseThrow().getStatus()).isEqualTo(AgreementStatus.OVERDUE);
        assertThat(sentMail().getSubject()).isEqualTo("RentEngine: Minął termin zwrotu – umowa nr " + id);
    }

    private SimpleMailMessage sentMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        return captor.getValue();
    }

    private ResultActions reserve() throws Exception {
        String token = JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"client@rentengine.local\",\"password\":\"Client123!\"}"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        LocalDateTime start = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.HOURS);
        return mvc.perform(post("/api/rentals").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitId\":3,\"startDate\":\"%s\",\"endDate\":\"%s\"}".formatted(start, start.plusDays(2))));
    }
}
