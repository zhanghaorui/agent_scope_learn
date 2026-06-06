package com.haorui.agent;

import com.haorui.config.OllamaProperties;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import com.haorui.exception.AgentException;
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
        String userId = resolveUserId(request);

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        return Mono.fromCallable(() -> request.getMessage())
                .flatMap(message -> agent.call(new UserMessage(message), ctx))
                .map(response -> {
                    String content = response.getContent().toString();
                    log.debug("Chat response: sessionId={}, length={}", sessionId, content.length());
                    return ChatResponse.success(sessionId, content);
                })
                .onErrorMap(e -> new AgentException("对话处理失败: " + e.getMessage(), e, sessionId))
                .doOnNext(r -> log.info("Chat completed: sessionId={}", sessionId));
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String sessionId = resolveSessionId(request);
        String userId = resolveUserId(request);

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        return agent.streamEvents(new UserMessage(request.getMessage()), ctx)
                .filter(event -> event.getType() == AgentEventType.TEXT_BLOCK_DELTA)
                .cast(TextBlockDeltaEvent.class)
                .map(TextBlockDeltaEvent::getDelta)
                .onErrorResume(e -> {
                    log.error("Stream error: sessionId={}, error={}", sessionId, e.getMessage());
                    return Flux.just("[错误] 流式输出失败: " + e.getMessage());
                });
    }

    @Override
    public String getStatus() {
        return "OllamaAgent状态: 就绪 (model=" + properties.getModel() + ")";
    }

    private String resolveSessionId(ChatRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            return UUID.randomUUID().toString();
        }
        return request.getSessionId();
    }

    private String resolveUserId(ChatRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            return properties.getDefaultUserId();
        }
        return request.getUserId();
    }
}