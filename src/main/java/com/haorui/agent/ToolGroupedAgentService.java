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
 * 带工具分组的Agent服务示例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolGroupedAgentService {

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
        toolkit.createToolGroup("time", "时间工具", true);       // 默认激活
        toolkit.createToolGroup("calc", "计算工具", true);       // 默认激活
        toolkit.createToolGroup("text", "文本工具", false);      // 默认关闭
        
        // 注册工具到分组
        toolkit.registration()
                .tool(timeTools)
                .group("time")
                .apply();
        
        toolkit.registration()
                .tool(calculatorTools)
                .group("calc")
                .apply();
        
        toolkit.registration()
                .tool(textTools)
                .group("text")
                .apply();
        
        // 构建带自定义Toolkit的Agent
        this.agent = HarnessAgent.builder()
                .name("tool-assistant")
                .sysPrompt(properties.getSysPrompt() + "\n\n你可以使用以下工具：\n" +
                        "- 时间工具：获取当前时间、星期几\n" +
                        "- 计算工具：数学计算、单位转换\n" +
                        "- 文本工具：字符统计、文本反转、数字提取")
                .model(modelId)
                .toolkit(toolkit)  // 使用自定义Toolkit
                .workspace(workspace)
                .build();

        log.info("ToolGroupedAgent initialized: model={}, groups={}", 
                modelId, "time(active), calc(active), text(inactive)");
    }
    
    /**
     * 获取工具分组状态
     */
    public String getToolGroupStatus() {
        return "当前分组状态:\n" +
                "- time: 激活 (时间工具)\n" +
                "- calc: 激活 (计算工具)\n" +
                "- text: 未激活 (文本工具)";
    }
    
    /**
     * 动态激活/关闭工具分组
     */
    public void toggleToolGroup(String groupName, boolean active) {
        toolkit.updateToolGroups(java.util.List.of(groupName), active);
        log.info("Tool group '{}' toggled to: {}", groupName, active ? "active" : "inactive");
    }

    /**
     * 对话（带工具调用能力）
     */
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
                    log.debug("ToolAgent response: sessionId={}, contentLength={}", sessionId, content.length());
                    return ChatResponse.success(sessionId, content);
                })
                .onErrorMap(e -> new AgentException("对话处理失败: " + e.getMessage(), e, sessionId));
    }

    /**
     * 流式对话
     */
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