package com.haorui.agent;

import com.haorui.config.OllamaProperties;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import com.haorui.exception.AgentException;
import com.haorui.tool.CalculatorTools;
import com.haorui.tool.TextTools;
import com.haorui.tool.TimeTools;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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

    @PostConstruct
    public void init() {
        Path workspace = Paths.get(properties.getWorkspacePath());
        String modelId = "ollama:" + properties.getModel();

        // 创建Toolkit并定义分组
        toolkit = new Toolkit();
        toolkit.createToolGroup("time", "时间工具", true);
        toolkit.createToolGroup("calc", "计算工具", true);
        toolkit.createToolGroup("text", "文本工具", false);

        // 注册工具到分组
        toolkit.registration().tool(timeTools).group("time").apply();
        toolkit.registration().tool(calculatorTools).group("calc").apply();
        toolkit.registration().tool(textTools).group("text").apply();

        // 构建Agent
        this.agent = HarnessAgent.builder()
                .name("tool-assistant")
                .sysPrompt(buildSystemPrompt())
                .model(modelId)
                .toolkit(toolkit)
                .workspace(workspace)
                .build();

        log.info("ToolGroupedAgent initialized: model={}, groups=time,calc,text", modelId);
    }

    private String buildSystemPrompt() {
        return properties.getSysPrompt() + "\n\n你可以使用以下工具：\n" +
                "- 时间工具：获取当前时间、星期几\n" +
                "- 计算工具：数学计算、单位转换\n" +
                "- 文本工具：字符统计、文本反转、数字提取";
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        String sessionId = resolveSessionId(request);
        RuntimeContext ctx = buildContext(sessionId, request);

        return Mono.fromCallable(() -> request.getMessage())
                .flatMap(message -> agent.call(new UserMessage(message), ctx))
                .map(response -> ChatResponse.success(sessionId, response.getContent().toString()))
                .onErrorMap(e -> new AgentException("对话处理失败: " + e.getMessage(), e, sessionId));
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        String sessionId = resolveSessionId(request);
        RuntimeContext ctx = buildContext(sessionId, request);

        return agent.streamEvents(new UserMessage(request.getMessage()), ctx)
                .filter(event -> event.getType() == AgentEventType.TEXT_BLOCK_DELTA)
                .cast(TextBlockDeltaEvent.class)
                .map(TextBlockDeltaEvent::getDelta)
                .onErrorResume(e -> Flux.just("[错误] " + e.getMessage()));
    }

    @Override
    public String getToolGroupStatus() {
        return "工具分组状态:\n- time: 激活\n- calc: 激活\n- text: 未激活";
    }

    @Override
    public void toggleToolGroup(String groupName, boolean active) {
        toolkit.updateToolGroups(java.util.List.of(groupName), active);
        log.info("Tool group '{}' toggled: {}", groupName, active ? "active" : "inactive");
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