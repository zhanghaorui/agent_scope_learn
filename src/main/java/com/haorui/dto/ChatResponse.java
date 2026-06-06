package com.haorui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * AI回复内容
     */
    private String content;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    public static ChatResponse success(String sessionId, String content) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .content(content)
                .success(true)
                .build();
    }

    public static ChatResponse failure(String sessionId, String errorMessage) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}