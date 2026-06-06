package com.haorui.service.impl;

import com.haorui.config.OllamaProperties;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import com.haorui.exception.AgentException;
import com.haorui.service.AgentChatService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Ollama Agent服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaAgentServiceImpl implements AgentChatService {

    private final OllamaProperties properties;
    private HarnessAgent agent;

    @PostConstruct
    public void init() {
        Path workspace = Paths.get(properties.getWorkspacePath());
        String modelId = "ollama:" + properties.getModel();

        this.agent = HarnessAgent.builder()
                .name(properties.getAgentName())
                .sysPrompt(properties.getSysPrompt())
                .model(modelId)
                .workspace(workspace)
                .build();

        log.info("OllamaAgent initialized: name={}, model={}", properties.getAgentName(), modelId);
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        String sessionId = resolveSessionId(request);
        RuntimeContext ctx = buildContext(sessionId, request);

        return Mono.fromCallable(() -> request.getMessage())
                .flatMap(message -> agent.call(new UserMessage(message), ctx))
                .map(response -> ChatResponse.success(sessionId, response.getContent().toString()))
                .onErrorMap(e -> new AgentException("对话处理失败: " + e.getMessage(), e, sessionId))
                .doOnNext(r -> log.info("Chat completed: sessionId={}", sessionId));
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String sessionId = resolveSessionId(request);
        RuntimeContext ctx = buildContext(sessionId, request);

        return agent.streamEvents(new UserMessage(request.getMessage()), ctx)
                .filter(event -> event.getType() == AgentEventType.TEXT_BLOCK_DELTA)
                .cast(TextBlockDeltaEvent.class)
                .map(TextBlockDeltaEvent::getDelta)
                .onErrorResume(e -> Flux.just("[错误] 流式输出失败: " + e.getMessage()));
    }

    @Override
    public String getStatus() {
        return "OllamaAgent状态: 就绪 (model=" + properties.getModel() + ")";
    }

    private RuntimeContext buildContext(String sessionId, ChatRequest request) {
        return RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(resolveUserId(request))
                .build();
    }

    private String resolveSessionId(ChatRequest request) {
        return request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getSessionId();
    }

    private String resolveUserId(ChatRequest request) {
        return request.getUserId() == null || request.getUserId().isBlank()
                ? properties.getDefaultUserId()
                : request.getUserId();
    }
}