package com.haorui.util;

import com.haorui.dto.ChatRequest;
import io.agentscope.core.agent.RuntimeContext;

import java.util.UUID;

/**
 * Agent上下文工具类（消除各 ServiceImpl 中的重复逻辑）
 */
public final class AgentContextUtils {

    private AgentContextUtils() {
    }

    /**
     * 解析会话ID，若请求未携带则自动生成
     */
    public static String resolveSessionId(ChatRequest request) {
        return request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getSessionId();
    }

    /**
     * 解析用户ID，若请求未携带则使用默认值
     */
    public static String resolveUserId(ChatRequest request, String defaultUserId) {
        return request.getUserId() == null || request.getUserId().isBlank()
                ? defaultUserId
                : request.getUserId();
    }

    /**
     * 根据请求构建 RuntimeContext
     */
    public static RuntimeContext buildContext(ChatRequest request, String defaultUserId) {
        return RuntimeContext.builder()
                .sessionId(resolveSessionId(request))
                .userId(resolveUserId(request, defaultUserId))
                .build();
    }
}

