package com.wzabek.rentengine;

import com.jayway.jsonpath.JsonPath;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pełny przepływ przez HTTP: prawdziwe logowanie, filtr JWT, role i baza w Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RentalAgreementRepository agreementRepository;

    @AfterEach
    void cleanUp() {
        agreementRepository.deleteAll();
    }

    @Test
    void reservationWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/api/rentals").contentType(MediaType.APPLICATION_JSON).content(rentalJson(3, 10)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void clientCannotUseAdminEndpoints() throws Exception {
        String token = login("client@rentengine.local", "Client123!");

        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"type\":\"CAR\",\"requiredParams\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        String token = login("admin@rentengine.local", "Admin123!");

        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void clientReservesUnitAndOnlyOwnerSeesIt() throws Exception {
        String jan = login("client@rentengine.local", "Client123!");
        String anna = login("client2@rentengine.local", "Client123!");

        MvcResult created = mvc.perform(post("/api/rentals").header(HttpHeaders.AUTHORIZATION, bearer(jan))
                        .contentType(MediaType.APPLICATION_JSON).content(rentalJson(3, 10)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("http://localhost/api/rentals/")))
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn();
        Integer id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/rentals/my").header(HttpHeaders.AUTHORIZATION, bearer(jan)))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get("/api/rentals/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(anna)))
                .andExpect(status().isForbidden());
        // R1 przez HTTP: ten sam egzemplarz w tym samym terminie
        mvc.perform(post("/api/rentals").header(HttpHeaders.AUTHORIZATION, bearer(anna))
                        .contentType(MediaType.APPLICATION_JSON).content(rentalJson(3, 10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UNIT_NOT_AVAILABLE"));
    }

    @Test
    void invalidRequestsReturn400WithFieldErrors() throws Exception {
        String jan = login("client@rentengine.local", "Client123!");
        String employee = login("employee@rentengine.local", "Employee123!");
        String admin = login("admin@rentengine.local", "Admin123!");

        mvc.perform(post("/api/rentals").header(HttpHeaders.AUTHORIZATION, bearer(jan))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"startDate\":\"2020-01-01T10:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.unitId").value("must not be null"))
                .andExpect(jsonPath("$.errors.startDate").value("must be a future date"))
                .andExpect(jsonPath("$.errors.endDate").value("must not be null"));
        mvc.perform(get("/api/rentals").param("status", "LOST").header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status", startsWith("must be one of [RESERVED")));
        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"boss@test.pl\",\"password\":\"Secret123!\",\"firstName\":\"A\",\"lastName\":\"B\",\"role\":\"BOSS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").value("must be one of [CLIENT, EMPLOYEE, ADMIN]"));
        mvc.perform(get("/api/equipment").param("minPrice", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.minPrice").value("must be a number"));
    }

    @Test
    void staffPagesAndFiltersRentals() throws Exception {
        String jan = login("client@rentengine.local", "Client123!");
        String employee = login("employee@rentengine.local", "Employee123!");
        for (int days : new int[]{10, 20}) {
            mvc.perform(post("/api/rentals").header(HttpHeaders.AUTHORIZATION, bearer(jan))
                            .contentType(MediaType.APPLICATION_JSON).content(rentalJson(3, days)))
                    .andExpect(status().isCreated());
        }

        String later = LocalDateTime.now().plusDays(20).truncatedTo(ChronoUnit.HOURS).toString();
        mvc.perform(get("/api/rentals").param("size", "1").param("sort", "startDate,desc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].startDate", startsWith(later)));
        mvc.perform(get("/api/rentals").param("clientId", "2").header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/rentals").param("sort", "client.passwordHash").header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sort").exists());
    }

    @Test
    void swaggerDocsArePublicAndDescribeBearerAuth() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
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

    private static String rentalJson(long unitId, int daysFromNow) {
        LocalDateTime start = LocalDateTime.now().plusDays(daysFromNow).truncatedTo(ChronoUnit.HOURS);
        return "{\"unitId\":%d,\"startDate\":\"%s\",\"endDate\":\"%s\"}".formatted(unitId, start, start.plusDays(2));
    }
}
