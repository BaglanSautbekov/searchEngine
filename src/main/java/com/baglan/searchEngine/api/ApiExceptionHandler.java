package com.baglan.searchEngine.api;

import com.baglan.searchEngine.common.NotFoundException;
import com.baglan.searchEngine.search.SearchException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.BAD_REQUEST, "validation_error", "Request validation failed", request);

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.BAD_REQUEST, "validation_error", "Request validation failed", request);

        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            errors.put(field, violation.getMessage());
        }

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return baseProblem(HttpStatus.BAD_REQUEST, "invalid_json", "Request body is invalid or unreadable", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return baseProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return baseProblem(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getMessage(), request);
    }

    @ExceptionHandler(SearchException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleSearchException(SearchException ex, HttpServletRequest request) {
        return baseProblem(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), "Search backend is unavailable or returned an error", request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleInternalError(Exception ex, HttpServletRequest request) {
        return baseProblem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Unexpected server error", request);
    }

    private ProblemDetail baseProblem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestampUtc", Instant.now().toString());
        return problem;
    }
}