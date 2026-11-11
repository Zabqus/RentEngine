package com.wzabek.rentengine.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Klasa bazowa obsługuje standardowe błędy Springa (400, 404 brak ścieżki, 405...) jako ProblemDetail
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, "Business rule violated", ex.getMessage());
        pd.setProperty("code", ex.getCode());
        return pd;
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    ProblemDetail handleForbidden(ForbiddenOperationException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    // Brak wymaganej roli (@PreAuthorize); Spring daje tu tylko ogólne "Access Denied"
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this operation");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleUnauthorized(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "Resource was modified by another request, please retry");
    }

    // Np. naruszenie UNIQUE, gdy dwa requesty przejdą walidację w serwisie jednocześnie
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Data conflict", "Operation violates data constraints");
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return validationProblem(ex.getErrors());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return validationProblem(errors);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected server error");
    }

    // @Valid na @RequestBody
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Błąd konwersji (np. ?minPrice=abc) zamiast technicznego komunikatu Springa
            String message = fe.isBindingFailure()
                    ? invalidValue(ex.getBindingResult().getFieldType(fe.getField()), fe.getRejectedValue())
                    : fe.getDefaultMessage();
            errors.putIfAbsent(fe.getField(), message);
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    // Walidacja parametrów metody, np. @Positive na @PathVariable / @RequestParam
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(r -> {
            String name = r.getMethodParameter().getParameterName();
            r.getResolvableErrors().forEach(e -> errors.putIfAbsent(name, e.getDefaultMessage()));
        });
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    // Zły typ w @RequestParam / @PathVariable, np. ?status=FOO albo /api/rentals/abc
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return ResponseEntity.badRequest().body(validationProblem(
                Map.of(name == null ? "parameter" : name, invalidValue(ex.getRequiredType(), ex.getValue()))));
    }

    // Body nie da się odczytać: zły typ pola w JSON (np. "role": "BOSS") albo uszkodzony JSON
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
                Object value = mie instanceof InvalidFormatException ife ? ife.getValue() : null;
                return ResponseEntity.badRequest().body(validationProblem(
                        Map.of(jsonPath(mie.getPath()), invalidValue(mie.getTargetType(), value))));
            }
        }
        return ResponseEntity.badRequest().body(
                problem(HttpStatus.BAD_REQUEST, "Malformed request", "Request body is missing or is not valid JSON"));
    }

    // Komunikat zależny od oczekiwanego typu; dla enumów lista dozwolonych wartości
    private static String invalidValue(Class<?> type, Object value) {
        if (type != null && type.isEnum()) {
            return "must be one of " + Arrays.toString(type.getEnumConstants());
        }
        if (type != null && (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class))) {
            return "must be a number";
        }
        if (type != null && Temporal.class.isAssignableFrom(type)) {
            return "must be a date-time in format yyyy-MM-ddTHH:mm";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "must be true or false";
        }
        return value == null ? "has invalid format" : "invalid value '%s'".formatted(value);
    }

    // Ścieżka pola w JSON, np. params.weightKg albo requiredParams[1]
    private static String jsonPath(List<JacksonException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (JacksonException.Reference ref : path) {
            if (ref.getPropertyName() != null) {
                sb.append(sb.isEmpty() ? "" : ".").append(ref.getPropertyName());
            } else {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.isEmpty() ? "body" : sb.toString();
    }

    private ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid data");
        pd.setProperty("errors", errors);
        return pd;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
