package com.haorui.controller;

import com.haorui.agent.OllamaAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Agent REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final OllamaAgentService agentService;

    /**
     * 简单对话接口
     *
     * @param message 用户消息
     * @param sessionId 会话ID（可选，默认为"default"）
     * @return AI回复
     */
    @PostMapping("/chat")
    public Mono<String> chat(
            @RequestBody String message,
            @RequestParam(defaultValue = "default") String sessionId) {
        log.info("Received chat request: {}", message);
        return agentService.chat(message, sessionId);
    }

    /**
     * 流式对话接口
     *
     * @param message 用户消息
     * @param sessionId 会话ID（可选，默认为"default"）
     * @return 流式AI回复
     */
    @PostMapping("/chat/stream")
    public Flux<String> chatStream(
            @RequestBody String message,
            @RequestParam(defaultValue = "default") String sessionId) {
        log.info("Received streaming chat request: {}", message);
        return agentService.chatStream(message, sessionId);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "Agent service is running!";
    }
}