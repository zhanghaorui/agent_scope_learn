package com.haorui.service;

import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Agent对话服务接口
 */
public interface AgentChatService {

    /**
     * 对话
     *
     * @param request 对话请求
     * @return 对话响应
     */
    Mono<ChatResponse> chat(ChatRequest request);

    /**
     * 流式对话
     *
     * @param request 对话请求
     * @return 流式文本片段
     */
    Flux<String> chatStream(ChatRequest request);

    /**
     * 获取服务状态
     *
     * @return 状态描述
     */
    String getStatus();
}