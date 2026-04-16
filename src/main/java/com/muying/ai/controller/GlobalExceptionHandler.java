package com.muying.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex, HttpServletRequest request) {
        String message = resolveValidationMessage(ex);
        log.warn("请求参数错误, path={}, message={}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        log.warn("上传文件超过限制, path={}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_UPLOAD_TOO_LARGE", "上传文件超过大小限制", request);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex, HttpServletRequest request) {
        log.error("文件处理失败, path={}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "IO_ERROR", "文件处理失败，请稍后重试", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("未预期异常, path={}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "服务内部错误，请稍后重试", request);
    }

    private String resolveValidationMessage(Exception ex) {
        String message = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException
                && methodArgumentNotValidException.getBindingResult().getFieldError() != null) {
            message = methodArgumentNotValidException.getBindingResult().getFieldError().getDefaultMessage();
        }
        if (ex instanceof BindException bindException
                && bindException.getBindingResult().getFieldError() != null) {
            message = bindException.getBindingResult().getFieldError().getDefaultMessage();
        }
        return (message == null || message.isBlank()) ? "请求参数不合法" : message;
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
