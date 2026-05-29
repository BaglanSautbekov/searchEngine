package com.baglan.searchEngine.api;

import com.baglan.searchEngine.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
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
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("validation_error");
        problem.setDetail("Request validation failed");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestampUtc", Instant.now().toString());

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("invalid_json");
        problem.setDetail("Request body is invalid or unreadable");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestampUtc", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle(ex.getMessage());
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestampUtc", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle(ex.getMessage());
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestampUtc", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleInternalError(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("internal_error");
        problem.setDetail("Unexpected server error");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestampUtc", Instant.now().toString());
        return problem;
    }
}