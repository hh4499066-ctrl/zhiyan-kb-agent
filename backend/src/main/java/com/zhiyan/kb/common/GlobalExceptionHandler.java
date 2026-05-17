package com.zhiyan.kb.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException ex) {
        HttpStatus status = resolveStatus(ex.getCode());
        return ResponseEntity.status(status).body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> validation(Exception ex) {
        log.warn("Request validation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Result.fail(400, "Validation failed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> exception(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("System error traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "System error. traceId=" + traceId));
    }

    private HttpStatus resolveStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null && status.isError()) {
            return status;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
