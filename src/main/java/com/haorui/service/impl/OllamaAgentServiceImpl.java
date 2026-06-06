package com.haorui.service.impl;

import com.haorui.config.OllamaProperties;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import com.haorui.exception.AgentException;
import com.haorui.service.AgentChatService;
import com.haorui.util.AgentContextUtils;
import io.agentscope.core.model.GenerateOptions;
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
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

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
                .generateOptions(GenerateOptions.builder().build())  // 防止 streamEvents 时 options NPE
                .build();

        log.info("OllamaAgent initialized: name={}, model={}", properties.getAgentName(), modelId);
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        RuntimeContext ctx = AgentContextUtils.buildContext(request, properties.getDefaultUserId());

        return Mono.defer(() -> agent.call(new UserMessage(request.getMessage()), ctx))
                .subscribeOn(Schedulers.boundedElastic())   // 避免在 NIO 线程上阻塞
                .map(response -> ChatResponse.success(ctx.getSessionId(), response.getContent().toString()))
                .onErrorMap(e -> new AgentException("对话处理失败: " + e.getMessage(), e, ctx.getSessionId()))
                .doOnNext(r -> log.info("Chat completed: sessionId={}", ctx.getSessionId()));
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        return Flux.defer(() -> {
            RuntimeContext ctx = AgentContextUtils.buildContext(request, properties.getDefaultUserId());
            AtomicBoolean hasEmitted = new AtomicBoolean(false);

            return agent.streamEvents(new UserMessage(request.getMessage()), ctx)
                    .filter(event -> event.getType() == AgentEventType.TEXT_BLOCK_DELTA)
                    .cast(TextBlockDeltaEvent.class)
                    .map(delta -> {
                        hasEmitted.set(true);
                        return delta.getDelta();
                    })
                    .onErrorResume(e -> {
                        if (hasEmitted.get()) {
                            // 响应已完成，框架后处理（memory flush）的 NPE，静默忽略
                            log.warn("Suppressed post-response framework error: {}", e.getMessage());
                            return Flux.empty();
                        }
                        return Flux.just("[错误] 流式输出失败: " + e.getMessage());
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String getStatus() {
        return "OllamaAgent状态: 就绪 (model=" + properties.getModel() + ")";
    }
}