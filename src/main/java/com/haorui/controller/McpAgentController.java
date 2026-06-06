package com.haorui.controller;

import com.haorui.agent.McpAgentService;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * MCP Agent控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp-agent")
@RequiredArgsConstructor
public class McpAgentController {

    private final McpAgentService agentService;

    /**
     * MCP对话（Agent可调用MCP工具）
     */
    @PostMapping("/chat")
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("MCP Agent chat: message={}", request.getMessage());
        return agentService.chat(request);
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("MCP Agent stream: message={}", request.getMessage());
        return agentService.chatStream(request);
    }

    /**
     * MCP状态
     */
    @GetMapping("/status")
    public String status() {
        return agentService.getMcpStatus();
    }
}