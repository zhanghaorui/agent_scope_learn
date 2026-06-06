package com.haorui.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 会话ID，用于保持对话上下文
     * 相同sessionId的对话会保持上下文记忆
     */
    private String sessionId;

    /**
     * 用户ID（可选）
     */
    private String userId;
}