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

import java.util.Map;

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
     * 初始化MCP服务（手动触发）
     */
    @PostMapping("/initialize")
    public Map<String, Object> initialize() {
        agentService.initialize();
        return Map.of(
                "success", agentService.isReady(),
                "message", agentService.isReady() ? "MCP服务初始化成功" : "MCP服务初始化失败"
        );
    }

    /**
     * MCP对话
     */
    @PostMapping("/chat")
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("MCP Agent chat: sessionId={}, message={}", 
                request.getSessionId(), 
                request.getMessage().length() > 50 ? request.getMessage().substring(0, 50) + "..." : request.getMessage());
        return agentService.chat(request);
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("MCP Agent stream: sessionId={}", request.getSessionId());
        return agentService.chatStream(request);
    }

    /**
     * 获取详细状态
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return agentService.getStatusDetails();
    }

    /**
     * 动态激活/关闭MCP服务器
     */
    @PostMapping("/servers/{name}/toggle")
    public Map<String, Object> toggleServer(
            @PathVariable String name,
            @RequestParam boolean active) {
        try {
            agentService.toggleServer(name, active);
            return Map.of(
                    "success", true,
                    "server", name,
                    "active", active
            );
        } catch (IllegalArgumentException e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 检查服务就绪状态
     */
    @GetMapping("/ready")
    public Map<String, Object> ready() {
        return Map.of(
                "ready", agentService.isReady(),
                "servers", agentService.getMcpClients().keySet()
        );
    }
}