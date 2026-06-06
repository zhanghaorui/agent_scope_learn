package com.haorui.service;

import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP Agent服务接口
 */
public interface McpAgentService {

    /**
     * 初始化MCP服务
     */
    void initialize();

    /**
     * 对话
     */
    Mono<ChatResponse> chat(ChatRequest request);

    /**
     * 流式对话
     */
    Flux<String> chatStream(ChatRequest request);

    /**
     * 检查服务就绪状态
     */
    boolean isReady();

    /**
     * 动态激活/关闭MCP服务器
     */
    void toggleServer(String serverName, boolean active);

    /**
     * 获取服务状态详情
     */
    Map<String, Object> getStatusDetails();

    /**
     * 获取MCP客户端映射
     */
    Map<String, ?> getMcpClients();
}