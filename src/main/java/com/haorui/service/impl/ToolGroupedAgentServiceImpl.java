package com.haorui.service.impl;

import com.haorui.config.OllamaProperties;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import com.haorui.exception.AgentException;
import com.haorui.service.ToolGroupedAgentService;
import com.haorui.tool.CalculatorTools;
import com.haorui.tool.TextTools;
import com.haorui.tool.TimeTools;
import com.haorui.util.AgentContextUtils;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 工具分组Agent服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolGroupedAgentServiceImpl implements ToolGroupedAgentService {

    private final OllamaProperties properties;
    private final TimeTools timeTools;
    private final CalculatorTools calculatorTools;
    private final TextTools textTools;

    private HarnessAgent agent;
    private Toolkit toolkit;

    /** 追踪各工具分组当前激活状态（动态，非硬编码） */
    private final Map<String, Boolean> groupStates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Path workspace = Paths.get(properties.getWorkspacePath());
        String modelId = "ollama:" + properties.getModel();

        toolkit = new Toolkit();
        toolkit.createToolGroup("time", "时间工具", true);
        toolkit.createToolGroup("calc", "计算工具", true);
        toolkit.createToolGroup("text", "文本工具", false);

        groupStates.put("time", true);
        groupStates.put("calc", true);
        groupStates.put("text", false);

        toolkit.registration().tool(timeTools).group("time").apply();
        toolkit.registration().tool(calculatorTools).group("calc").apply();
        toolkit.registration().tool(textTools).group("text").apply();

        this.agent = HarnessAgent.builder()
                .name("tool-assistant")
                .sysPrompt(buildSystemPrompt())
                .model(modelId)
                .toolkit(toolkit)
                .workspace(workspace)
                .generateOptions(GenerateOptions.builder().build())  // 防止 streamEvents 时 options NPE
                .build();

        log.info("ToolGroupedAgent initialized: model={}, groups=time,calc,text(disabled)", modelId);
    }

    private String buildSystemPrompt() {
        return properties.getSysPrompt() + "\n\n你可以使用以下工具：\n" +
                "- 时间工具：获取当前时间、星期几\n" +
                "- 计算工具：数学计算（支持括号和幂运算）、单位转换\n" +
                "- 文本工具：字符统计、文本反转、数字提取";
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        RuntimeContext ctx = AgentContextUtils.buildContext(request, properties.getDefaultUserId());

        return Mono.defer(() -> agent.call(new UserMessage(request.getMessage()), ctx))
                .subscribeOn(Schedulers.boundedElastic())   // 避免在 NIO 线程上阻塞
                .map(response -> ChatResponse.success(ctx.getSessionId(), response.getContent().toString()))
                .onErrorMap(e -> new AgentException("对话处理失败: " + e.getMessage(), e, ctx.getSessionId()));
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
                            log.warn("Suppressed post-response framework error: {}", e.getMessage());
                            return Flux.empty();
                        }
                        return Flux.just("[错误] " + e.getMessage());
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String getToolGroupStatus() {
        // 从 groupStates 动态生成，反映 toggle 之后的真实状态
        StringBuilder sb = new StringBuilder("工具分组状态:\n");
        // 使用 LinkedHashMap 保持插入顺序展示
        new LinkedHashMap<>(groupStates).forEach((group, active) ->
                sb.append("- ").append(group).append(": ").append(active ? "激活" : "未激活").append("\n"));
        return sb.toString().trim();
    }

    @Override
    public void toggleToolGroup(String groupName, boolean active) {
        if (!groupStates.containsKey(groupName)) {
            throw new IllegalArgumentException("未知工具分组: " + groupName + "，可用分组: " + groupStates.keySet());
        }
        toolkit.updateToolGroups(List.of(groupName), active);
        groupStates.put(groupName, active);
        log.info("Tool group '{}' toggled: {}", groupName, active ? "active" : "inactive");
    }
}