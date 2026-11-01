package com.hrms.common.exceptions;

import com.hrms.common.responses.ApiError;
import com.hrms.common.responses.ApiResponse;
import com.hrms.common.responses.ClockSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ClockSource clockSource;

    public GlobalExceptionHandler(ClockSource clockSource) {
        this.clockSource = clockSource;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<ApiError>> handleApiException(ApiException exception, HttpServletRequest request) {
        return error(exception.getStatus(), exception.getErrorCode(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<ApiError>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Request validation failed", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<ApiError>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                details.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Request validation failed", request, details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<ApiError>> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                "Request body or parameters are invalid", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<ApiError>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Request parameter value is invalid", request, Map.of(exception.getName(), "invalid value"));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiResponse<ApiError>> handleDuplicateKey(
            DuplicateKeyException exception,
            HttpServletRequest request
    ) {
        log.info("A unique database constraint rejected the request at {}", request.getRequestURI());
        return error(HttpStatus.CONFLICT, ErrorCode.CONFLICT,
                "A resource with the same unique value already exists", request, Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<ApiError>> handleOptimisticLock(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ErrorCode.CONFLICT,
                "The record changed concurrently; retry the operation", request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<ApiError>> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access is forbidden", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<ApiError>> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled request failure at {}", request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred", request, Map.of());
    }

    private ResponseEntity<ApiResponse<ApiError>> error(
            HttpStatus status,
            ErrorCode code,
            String message,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        ApiError apiError = new ApiError(code.name(), request.getRequestURI(), details);
        return ResponseEntity.status(status).body(ApiResponse.failure(message, apiError, clockSource));
    }
}
