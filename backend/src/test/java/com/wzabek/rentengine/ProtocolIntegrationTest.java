package com.wzabek.rentengine;

import com.jayway.jsonpath.JsonPath;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.catalog.entity.UnitStatus;
import com.wzabek.rentengine.catalog.repository.EquipmentUnitRepository;
import com.wzabek.rentengine.protocol.entity.Damage;
import com.wzabek.rentengine.protocol.repository.DamageRepository;
import com.wzabek.rentengine.protocol.repository.ProtocolRepository;
import com.wzabek.rentengine.protocol.storage.PhotoStorage;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Wydanie → zwrot → uszkodzenie MAJOR (R7) → zdjęcie, przez HTTP na prawdziwej bazie
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProtocolIntegrationTest {

    // HUS-TC242-0001 z seeda: zbiornik 8.50 l, 210 motogodzin
    private static final long UNIT_ID = 7;
    private static final int UNIT_HOURS = 210;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RentalAgreementRepository agreementRepository;
    @Autowired
    private ProtocolRepository protocolRepository;
    @Autowired
    private EquipmentUnitRepository unitRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DamageRepository damageRepository;
    @Autowired
    private PhotoStorage photoStorage;

    @AfterEach
    void cleanUp() {
        damageRepository.findAll().stream().map(Damage::getPhotoPath).forEach(photoStorage::delete);
        protocolRepository.deleteAll();
        agreementRepository.deleteAll();
        EquipmentUnit unit = unitRepository.findById(UNIT_ID).orElseThrow();
        unit.setStatus(UnitStatus.AVAILABLE);
        unit.setMileageOrHours(UNIT_HOURS);
        unitRepository.save(unit);
    }

    @Test
    void employeeIssuesAndReceivesUnitAndRecordsMajorDamageWithPhoto() throws Exception {
        String employee = login("employee@rentengine.local", "Employee123!");
        String client = login("client@rentengine.local", "Client123!");
        // Umowa trwająca już od godziny; przez API nie da się zarezerwować wstecz
        LocalDateTime start = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MINUTES);
        RentalAgreement agreement = agreementRepository.save(new RentalAgreement(
                userRepository.findById(1L).orElseThrow(), unitRepository.findById(UNIT_ID).orElseThrow(),
                start, start.plusDays(2), new BigDecimal("320.00"), new BigDecimal("800.00")));
        Long id = agreement.getId();

        mvc.perform(get("/api/rentals").param("status", "RESERVED").header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(post("/api/rentals/{id}/protocols", id).header(HttpHeaders.AUTHORIZATION, bearer(client))
                        .contentType(MediaType.APPLICATION_JSON).content(protocolJson("ISSUE", "8.50", 210)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/rentals/{id}/protocols", id).header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content(protocolJson("ISSUE", "8.50", 210)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ISSUE"));
        mvc.perform(get("/api/rentals/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(client)))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        String returned = mvc.perform(post("/api/rentals/{id}/protocols", id).header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content(protocolJson("RETURN", "3.00", 225)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer returnId = JsonPath.read(returned, "$.id");

        String damage = mvc.perform(post("/api/protocols/{id}/damages", returnId).header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Bent blade\",\"severity\":\"MAJOR\",\"cost\":1200.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolId").value(returnId))
                .andReturn().getResponse().getContentAsString();
        Integer damageId = JsonPath.read(damage, "$.id");

        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
        mvc.perform(multipart(HttpMethod.PUT, "/api/damages/{id}/photo", damageId)
                        .file(new MockMultipartFile("photo", "fake.jpg", "image/jpeg", "not an image".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.photo").value("must be a JPEG or PNG image"));
        mvc.perform(multipart(HttpMethod.PUT, "/api/damages/{id}/photo", damageId)
                        .file(new MockMultipartFile("photo", "blade.jpg", "image/jpeg", jpeg))
                        .header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("/api/damages/" + damageId + "/photo"));
        mvc.perform(get("/api/damages/{id}/photo", damageId).header(HttpHeaders.AUTHORIZATION, bearer(client)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(content().bytes(jpeg));
        mvc.perform(get("/api/damages/{id}/photo", damageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("client2@rentengine.local", "Client123!"))))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/rentals/{id}/protocols", id).header(HttpHeaders.AUTHORIZATION, bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].damages", hasSize(1)));
        EquipmentUnit unit = unitRepository.findById(UNIT_ID).orElseThrow();
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.IN_SERVICE);
        assertThat(unit.getMileageOrHours()).isEqualTo(225);
        assertThat(agreementRepository.findById(id).orElseThrow().getStatus().name()).isEqualTo("RETURNED");
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String protocolJson(String type, String fuel, int hours) {
        return "{\"type\":\"%s\",\"fuelLevel\":%s,\"mileageOrHours\":%d}".formatted(type, fuel, hours);
    }
}
