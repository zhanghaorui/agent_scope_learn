package com.haorui.exception;

import com.haorui.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（兼容 WebFlux）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid 校验失败（WebFlux 中为 WebExchangeBindException）
     */
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ChatResponse handleValidationException(WebExchangeBindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return ChatResponse.failure(null, "参数校验失败: " + message);
    }

    /**
     * 处理Agent业务异常
     */
    @ExceptionHandler(AgentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ChatResponse handleAgentException(AgentException ex) {
        log.error("Agent error: sessionId={}, message={}", ex.getSessionId(), ex.getMessage());
        return ChatResponse.failure(ex.getSessionId(), ex.getMessage());
    }

    /**
     * 处理参数非法异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ChatResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ChatResponse.failure(null, "参数错误: " + ex.getMessage());
    }

    /**
     * 兜底：处理其他未预期异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ChatResponse handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ChatResponse.failure(null, "服务内部错误: " + ex.getMessage());
    }
}