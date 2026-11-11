package com.wzabek.rentengine.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Bez filtrów security: tu testujemy tylko mapowanie wyjątków (security ma własny test)
@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void notFoundReturns404() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Equipment with id 7 not found"));
    }

    @Test
    void businessRuleReturns409WithCode() throws Exception {
        mvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UNIT_IN_SERVICE"));
    }

    @Test
    void forbiddenReturns403() throws Exception {
        mvc.perform(get("/test/forbidden")).andExpect(status().isForbidden());
    }

    @Test
    void optimisticLockReturns409() throws Exception {
        mvc.perform(get("/test/lock")).andExpect(status().isConflict());
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void unknownPathReturns404ProblemDetail() throws Exception {
        mvc.perform(get("/test/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unexpectedErrorReturns500WithoutDetails() throws Exception {
        mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected server error"));
    }

    @Test
    void wrongEnumInQueryListsAllowedValues() throws Exception {
        mvc.perform(get("/test/params").param("color", "PINK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").value("must be one of [RED, GREEN]"));
    }

    @Test
    void wrongNumberInPathIsReadable() throws Exception {
        mvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.id").value("must be a number"));
    }

    @Test
    void wrongEnumInBodyPointsToField() throws Exception {
        mvc.perform(post("/test/paint").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\": \"PINK\", \"when\": \"2030-01-01T10:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").value("must be one of [RED, GREEN]"));
    }

    @Test
    void wrongDateInBodyIsReadable() throws Exception {
        mvc.perform(post("/test/paint").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\": \"RED\", \"when\": \"tomorrow\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.when").value("must be a date-time in format yyyy-MM-ddTHH:mm"));
    }

    @Test
    void malformedJsonReturns400WithoutInternals() throws Exception {
        mvc.perform(post("/test/paint").contentType(MediaType.APPLICATION_JSON).content("{\"color\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request body is missing or is not valid JSON"));
    }

    record Body(@NotBlank String name) {
    }

    enum Color { RED, GREEN }

    record Paint(Color color, LocalDateTime when) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("Equipment", 7);
        }

        @GetMapping("/test/business")
        void business() {
            throw new BusinessRuleException("UNIT_IN_SERVICE", "Unit is in service");
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new ForbiddenOperationException("Not your agreement");
        }

        @GetMapping("/test/lock")
        void lock() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @PostMapping("/test/valid")
        void valid(@Valid @RequestBody Body body) {
        }

        @GetMapping("/test/params")
        void params(@RequestParam Color color) {
        }

        @GetMapping("/test/items/{id}")
        void item(@PathVariable Long id) {
        }

        @PostMapping("/test/paint")
        void paint(@RequestBody Paint paint) {
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("secret internals");
        }
    }
}
