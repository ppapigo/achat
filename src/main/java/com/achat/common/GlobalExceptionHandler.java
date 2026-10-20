package com.achat.common;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(com.achat.security.UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(com.achat.security.UnauthorizedException e) {
        return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
    }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
    }
    @ExceptionHandler(com.achat.security.RateLimitService.RateLimitedException.class)
    public ResponseEntity<ApiResponse<Void>> limited(com.achat.security.RateLimitService.RateLimitedException e) {
        return ResponseEntity.status(429).header("Retry-After", Long.toString(e.retryAfter)).body(ApiResponse.error(e.getMessage()));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformed(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("요청 형식을 확인해 주세요"));
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(Exception e) {
        return ResponseEntity.status(404).body(ApiResponse.error("요청한 경로를 찾을 수 없습니다"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Request failed: {}", e.getClass().getSimpleName());
        return ResponseEntity.status(500).body(ApiResponse.error("요청 처리 중 오류가 발생했습니다"));
    }

    @ExceptionHandler(com.achat.augment.AugmentConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleAugmentConflict(com.achat.augment.AugmentConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("중복되거나 유효하지 않은 데이터입니다"));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentUpdate(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("다른 요청이 데이터를 변경했습니다. 다시 시도해 주세요"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        boolean applicationError = e.getStackTrace().length > 0 && e.getStackTrace()[0].getClassName().startsWith("com.achat.");
        return ResponseEntity.badRequest().body(ApiResponse.error(applicationError ? e.getMessage() : "입력값을 확인해 주세요"));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> invalidState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("현재 상태에서는 요청을 처리할 수 없습니다"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("유효하지 않은 입력입니다");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }
}
