package com.listings.search.exception;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.listings.search.dto.FieldErrorResponse;

/**
 * Maps exceptions to RFC 9457 {@code application/problem+json} responses.
 * Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own errors (404, 405, ...)
 * also use the problem format; validation errors add an {@code errors} array with every
 * invalid field so the UI can highlight all of them at once.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    static final URI VALIDATION_TYPE = URI.create("urn:problem-type:validation-error");

    /** Bean Validation and binding (type mismatch) failures on {@code @ModelAttribute} parameters. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();
        String detail = errors.size() == 1 ? errors.get(0).message() : errors.size() + " invalid search parameters";
        return ResponseEntity.badRequest().body(validationProblem(detail, errors));
    }

    @ExceptionHandler(PageOutOfRangeException.class)
    public ProblemDetail handlePageOutOfRange(PageOutOfRangeException ex) {
        return validationProblem(ex.getMessage(), List.of(new FieldErrorResponse("page", ex.getMessage())));
    }

    /** Last resort: never leak stack traces or internals to clients. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
        problem.setTitle("Internal Server Error");
        return problem;
    }

    private static ProblemDetail validationProblem(String detail, List<FieldErrorResponse> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Invalid request parameters");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static FieldErrorResponse toFieldError(FieldError error) {
        if (error.isBindingFailure()) {
            // e.g. ?minPrice=abc — Spring's default message is a Java conversion stack; replace it.
            return new FieldErrorResponse(error.getField(),
                    error.getField() + " must be a number (got '" + error.getRejectedValue() + "')");
        }
        return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
    }
}
