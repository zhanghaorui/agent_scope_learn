package com.haorui.service;

import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 带工具分组的Agent服务接口
 */
public interface ToolGroupedAgentService {

    /**
     * 对话（Agent可调用工具）
     */
    Mono<ChatResponse> chat(ChatRequest request);

    /**
     * 流式对话
     */
    Flux<String> chatStream(ChatRequest request);

    /**
     * 获取工具分组状态
     */
    String getToolGroupStatus();

    /**
     * 动态激活/关闭工具分组
     */
    void toggleToolGroup(String groupName, boolean active);
}