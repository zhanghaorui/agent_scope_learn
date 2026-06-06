package com.haorui.exception;

import lombok.Getter;

/**
 * Agent业务异常
 */
@Getter
public class AgentException extends RuntimeException {

    private final String sessionId;

    public AgentException(String message) {
        super(message);
        this.sessionId = null;
    }

    public AgentException(String message, String sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public AgentException(String message, Throwable cause, String sessionId) {
        super(message, cause);
        this.sessionId = sessionId;
    }
}