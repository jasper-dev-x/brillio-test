package com.listings.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.listings.search.FieldError;
import com.listings.search.InvalidSearchException;

/** Maps exceptions to consistent JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidSearchException.class)
    public ResponseEntity<ApiError> handleInvalidSearch(InvalidSearchException e) {
        return badRequest(e.getMessage(), e.getErrors());
    }

    /** e.g. ?minPrice=abc or ?page=1.5 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String expected = e.getRequiredType() == null ? "a valid value" : describe(e.getRequiredType());
        String message = e.getName() + " must be " + expected + " (got '" + e.getValue() + "')";
        return badRequest(message, List.of(new FieldError(e.getName(), message)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse er) {
            // Framework errors that already know their status (404 unknown path, 405 wrong method, ...)
            int status = er.getStatusCode().value();
            HttpStatus resolved = HttpStatus.resolve(status);
            String reason = resolved == null ? "Error" : resolved.getReasonPhrase();
            return ResponseEntity.status(status).body(new ApiError(status, reason, e.getMessage(), List.of()));
        }
        log.error("Unhandled error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Internal Server Error", "Something went wrong. Please try again.", List.of()));
    }

    private static ResponseEntity<ApiError> badRequest(String message, List<FieldError> details) {
        return ResponseEntity.badRequest().body(new ApiError(400, "Bad Request", message, details));
    }

    private static String describe(Class<?> type) {
        if (type == Long.class || type == Integer.class) return "a whole number";
        return "a valid " + type.getSimpleName();
    }
}
