package com.haorui.controller;

import com.haorui.service.ToolGroupedAgentService;
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
 * 带工具分组的Agent控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tool-agent")
@RequiredArgsConstructor
public class ToolAgentController {

    private final ToolGroupedAgentService agentService;

    /**
     * 对话接口（Agent可调用工具）
     */
    @PostMapping("/chat")
    public Mono<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("ToolAgent chat: message={}", request.getMessage());
        return agentService.chat(request);
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("ToolAgent stream chat: message={}", request.getMessage());
        return agentService.chatStream(request);
    }

    /**
     * 获取工具分组状态
     */
    @GetMapping("/tools/status")
    public String getToolStatus() {
        return agentService.getToolGroupStatus();
    }

    /**
     * 动态切换工具分组
     */
    @PostMapping("/tools/toggle")
    public String toggleToolGroup(
            @RequestParam String group,
            @RequestParam boolean active) {
        agentService.toggleToolGroup(group, active);
        return "工具分组 '" + group + "' 已切换为: " + (active ? "激活" : "关闭");
    }
}