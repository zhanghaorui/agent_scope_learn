package com.haorui.agent;

import com.haorui.config.McpProperties;
import com.haorui.config.OllamaProperties;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import com.haorui.exception.AgentException;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

/**
 * 带MCP工具的Agent服务
 * 支持连接外部MCP服务器（filesystem, git, fetch等）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpAgentService {

    private final OllamaProperties ollamaProperties;
    private final McpProperties mcpProperties;
    
    private HarnessAgent agent;
    private Toolkit toolkit;

    @PostConstruct
    public void init() {
        if (!mcpProperties.isEnabled()) {
            log.info("MCP is disabled, skipping initialization");
            return;
        }

        Path workspace = Paths.get(ollamaProperties.getWorkspacePath());
        String modelId = "ollama:" + ollamaProperties.getModel();
        
        toolkit = new Toolkit();
        
        // 初始化MCP客户端
        for (McpProperties.McpServerConfig server : mcpProperties.getServers()) {
            try {
                McpClientWrapper mcpClient = createMcpClient(server);
                
                // 创建工具组
                String groupName = server.getName();
                toolkit.createToolGroup(groupName, 
                        "MCP工具: " + server.getName(), 
                        server.isActive());
                
                // 注册MCP工具到分组
                var registration = toolkit.registration()
                        .mcpClient(mcpClient)
                        .group(groupName);
                
                // 工具过滤
                if (!server.getEnableTools().isEmpty()) {
                    registration.enableTools(server.getEnableTools());
                }
                if (!server.getDisableTools().isEmpty()) {
                    registration.disableTools(server.getDisableTools());
                }
                
                registration.apply();
                
                log.info("MCP server '{}' initialized with {} tools", 
                        server.getName(), 
                        server.getEnableTools().isEmpty() ? "all" : server.getEnableTools());
            } catch (Exception e) {
                log.error("Failed to initialize MCP server '{}': {}", 
                        server.getName(), e.getMessage());
            }
        }

        // 构建Agent
        this.agent = HarnessAgent.builder()
                .name("mcp-assistant")
                .sysPrompt(ollamaProperties.getSysPrompt() + "\n\n你可以使用MCP工具进行文件操作、Git操作等。")
                .model(modelId)
                .toolkit(toolkit)
                .workspace(workspace)
                .build();

        log.info("McpAgent initialized with {} MCP servers", 
                mcpProperties.getServers().size());
    }

    /**
     * 创建MCP客户端
     */
    private McpClientWrapper createMcpClient(McpProperties.McpServerConfig server) {
        McpClientBuilder builder = McpClientBuilder.create(server.getName())
                .timeout(Duration.ofSeconds(120))
                .initializationTimeout(Duration.ofSeconds(30));

        switch (server.getTransport().toLowerCase()) {
            case "stdio":
                // StdIO传输 - 启动本地MCP进程
                String[] cmdArgs = new String[server.getArgs().size()];
                server.getArgs().toArray(cmdArgs);
                builder.stdioTransport(server.getCommand(), cmdArgs);
                break;
                
            case "sse":
                // SSE传输 - HTTP Server-Sent Events
                builder.sseTransport(server.getEndpoint());
                break;
                
            case "http":
                // HTTP传输 - 无状态HTTP
                builder.streamableHttpTransport(server.getEndpoint());
                break;
                
            default:
                throw new IllegalArgumentException("Unknown transport: " + server.getTransport());
        }

        return builder.buildAsync().block();
    }
    
    /**
     * 获取MCP状态
     */
    public String getMcpStatus() {
        if (!mcpProperties.isEnabled()) {
            return "MCP未启用";
        }
        
        StringBuilder sb = new StringBuilder("MCP服务器状态:\n");
        for (McpProperties.McpServerConfig server : mcpProperties.getServers()) {
            sb.append("- ").append(server.getName())
              .append(": ").append(server.isActive() ? "激活" : "关闭")
              .append(" (").append(server.getTransport()).append(")\n");
        }
        return sb.toString();
    }

    /**
     * 对话（带MCP工具能力）
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        if (!mcpProperties.isEnabled() || agent == null) {
            return Mono.just(ChatResponse.failure(null, "MCP未启用"));
        }

        String sessionId = resolveSessionId(request);
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(resolveUserId(request))
                .build();

        return Mono.fromCallable(() -> request.getMessage())
                .flatMap(message -> agent.call(new UserMessage(message), ctx))
                .map(response -> ChatResponse.success(sessionId, response.getContent().toString()))
                .onErrorMap(e -> new AgentException("MCP对话失败: " + e.getMessage(), e, sessionId));
    }

    /**
     * 流式对话
     */
    public Flux<String> chatStream(ChatRequest request) {
        if (!mcpProperties.isEnabled() || agent == null) {
            return Flux.just("MCP未启用");
        }

        String sessionId = resolveSessionId(request);
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(resolveUserId(request))
                .build();

        return agent.streamEvents(new UserMessage(request.getMessage()), ctx)
                .filter(event -> event.getType() == AgentEventType.TEXT_BLOCK_DELTA)
                .cast(TextBlockDeltaEvent.class)
                .map(TextBlockDeltaEvent::getDelta);
    }

    private String resolveSessionId(ChatRequest request) {
        return request.getSessionId() == null || request.getSessionId().isBlank() 
                ? UUID.randomUUID().toString() 
                : request.getSessionId();
    }

    private String resolveUserId(ChatRequest request) {
        return request.getUserId() == null || request.getUserId().isBlank() 
                ? ollamaProperties.getDefaultUserId() 
                : request.getUserId();
    }
}