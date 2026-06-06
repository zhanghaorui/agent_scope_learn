package com.haorui.controller;

import com.haorui.service.AgentChatService;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentChatService agentService;

    /**
     * 对话接口
     *
     * @param request 对话请求
     * @return 对话响应
     */
    @PostMapping("/chat")
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request: sessionId={}, messageLength={}",
                request.getSessionId(), request.getMessage().length());
        return agentService.chat(request);
    }

    /**
     * 流式对话接口（Server-Sent Events）
     *
     * @param request 对话请求
     * @return 流式文本片段
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("Stream chat request: sessionId={}, messageLength={}",
                request.getSessionId(), request.getMessage().length());
        return agentService.chatStream(request);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "AgentScope Ollama Service",
                "timestamp", LocalDateTime.now()
        );
    }
}