package com.haorui.service.impl;

import com.haorui.config.McpProperties;
import com.haorui.config.OllamaProperties;
import com.haorui.dto.ChatRequest;
import com.haorui.dto.ChatResponse;
import com.haorui.exception.AgentException;
import com.haorui.service.McpAgentService;
import com.haorui.util.AgentContextUtils;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCP Agent服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpAgentServiceImpl implements McpAgentService {

    private final OllamaProperties ollamaProperties;
    private final McpProperties mcpProperties;

    private HarnessAgent agent;
    private Toolkit toolkit;

    @Getter
    private final Map<String, McpClientWrapper> mcpClients = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, ServerStatus> serverStatuses = new ConcurrentHashMap<>();

    @Getter
    private boolean ready = false;

    @Override
    public void initialize() {
        if (!mcpProperties.isEnabled()) {
            log.info("MCP is disabled");
            ready = false;
            return;
        }

        if (mcpProperties.getServers().isEmpty()) {
            log.warn("MCP enabled but no servers configured");
            ready = false;
            return;
        }

        toolkit = new Toolkit();
        int successCount = 0;

        for (McpProperties.McpServerConfig config : mcpProperties.getServers()) {
            ServerStatus status = initializeServer(config);
            serverStatuses.put(config.getName(), status);
            if (status.isSuccess()) {
                successCount++;
            }
        }

        if (successCount > 0) {
            createAgent();
            ready = true;
            log.info("MCP Agent initialized: {} servers ready, {} failed",
                    successCount, mcpProperties.getServers().size() - successCount);
        } else {
            log.error("All MCP servers failed to initialize");
            ready = false;
        }
    }

    private ServerStatus initializeServer(McpProperties.McpServerConfig config) {
        try {
            validateConfig(config);

            McpClientWrapper client = buildMcpClient(config);
            mcpClients.put(config.getName(), client);

            toolkit.createToolGroup(config.getName(), "MCP: " + config.getName(), config.isActive());

            registerTools(config, client);

            log.info("MCP server '{}' connected: transport={}", config.getName(), config.getTransport());
            return ServerStatus.success(config.getName(), config.getTransport());

        } catch (Exception e) {
            log.error("MCP server '{}' failed: {}", config.getName(), e.getMessage(), e);
            return ServerStatus.failure(config.getName(), e.getMessage());
        }
    }

    private void validateConfig(McpProperties.McpServerConfig config) {
        if (config.getName() == null || config.getName().isBlank()) {
            throw new IllegalArgumentException("MCP server name is required");
        }

        String transport = config.getTransport().toLowerCase();
        if (!Set.of("stdio", "sse", "http").contains(transport)) {
            throw new IllegalArgumentException("Invalid transport: " + transport);
        }

        if ("stdio".equals(transport)) {
            if (config.getCommand() == null || config.getCommand().isBlank()) {
                throw new IllegalArgumentException("Command is required for stdio transport");
            }
        } else {
            if (config.getEndpoint() == null || config.getEndpoint().isBlank()) {
                throw new IllegalArgumentException("Endpoint is required for " + transport + " transport");
            }
        }
    }

    private McpClientWrapper buildMcpClient(McpProperties.McpServerConfig config) {
        Duration timeout = Duration.ofSeconds(mcpProperties.getRequestTimeoutSeconds());
        Duration initTimeout = Duration.ofSeconds(mcpProperties.getInitTimeoutSeconds());

        McpClientBuilder builder = McpClientBuilder.create(config.getName())
                .timeout(timeout)
                .initializationTimeout(initTimeout);

        switch (config.getTransport().toLowerCase()) {
            case "stdio":
                String[] args = config.getArgs().toArray(new String[0]);
                builder.stdioTransport(config.getCommand(), args);
                break;
            case "sse":
                builder.sseTransport(config.getEndpoint());
                break;
            case "http":
                builder.streamableHttpTransport(config.getEndpoint());
                break;
        }

        return builder.buildAsync().block(initTimeout);
    }

    private void registerTools(McpProperties.McpServerConfig config, McpClientWrapper client) {
        var registration = toolkit.registration()
                .mcpClient(client)
                .group(config.getName());

        if (!config.getEnableTools().isEmpty()) {
            registration.enableTools(config.getEnableTools());
        }
        if (!config.getDisableTools().isEmpty()) {
            registration.disableTools(config.getDisableTools());
        }

        registration.apply();
    }

    private void createAgent() {
        Path workspace = Paths.get(ollamaProperties.getWorkspacePath());
        String modelId = "ollama:" + ollamaProperties.getModel();

        agent = HarnessAgent.builder()
                .name("mcp-assistant")
                .sysPrompt(buildSystemPrompt())
                .model(modelId)
                .toolkit(toolkit)
                .workspace(workspace)
                .generateOptions(GenerateOptions.builder().build())  // 防止 streamEvents 时 options NPE
                .build();
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder(ollamaProperties.getSysPrompt());
        sb.append("\n\n你可以使用以下MCP工具：\n");
        serverStatuses.forEach((name, status) -> {
            if (status.isSuccess()) {
                sb.append("- ").append(name).append("\n");
            }
        });
        return sb.toString();
    }

    @Override
    public Mono<ChatResponse> chat(ChatRequest request) {
        if (!isServiceReady()) {
            return Mono.just(ChatResponse.failure(null, "MCP服务未就绪"));
        }

        return Mono.defer(() -> {
            RuntimeContext ctx = AgentContextUtils.buildContext(request, ollamaProperties.getDefaultUserId());
            return agent.call(new UserMessage(request.getMessage()), ctx)
                    .map(response -> ChatResponse.success(ctx.getSessionId(), response.getContent().toString()))
                    .onErrorMap(e -> new AgentException("对话处理失败: " + e.getMessage(), e, ctx.getSessionId()));
        }).subscribeOn(Schedulers.boundedElastic());   // 避免在 NIO 线程上阻塞
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        if (!isServiceReady()) {
            return Flux.just("[错误] MCP服务未就绪");
        }

        return Flux.defer(() -> {
            RuntimeContext ctx = AgentContextUtils.buildContext(request, ollamaProperties.getDefaultUserId());
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
    public void toggleServer(String serverName, boolean active) {
        if (!mcpClients.containsKey(serverName)) {
            throw new IllegalArgumentException("Unknown server: " + serverName);
        }
        toolkit.updateToolGroups(List.of(serverName), active);
        log.info("MCP server '{}' toggled: {}", serverName, active ? "active" : "inactive");
    }

    @Override
    public Map<String, Object> getStatusDetails() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", mcpProperties.isEnabled());
        status.put("ready", ready);
        status.put("servers", serverStatuses);
        return status;
    }

    private boolean isServiceReady() {
        return mcpProperties.isEnabled() && ready && agent != null;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MCP service...");
        mcpClients.forEach((name, client) -> {
            try {
                if (client instanceof AutoCloseable closeable) {
                    closeable.close();
                    log.debug("MCP client '{}' closed", name);
                }
            } catch (Exception e) {
                log.warn("Failed to close MCP client '{}': {}", name, e.getMessage());
            }
        });
        mcpClients.clear();
        serverStatuses.clear();
        ready = false;
        log.info("MCP service shutdown complete");
    }


    @Getter
    public static class ServerStatus {
        private final String name;
        private final boolean success;
        private final String transport;
        private final String errorMessage;
        private final long timestamp;

        public static ServerStatus success(String name, String transport) {
            return new ServerStatus(name, true, transport, null);
        }

        public static ServerStatus failure(String name, String error) {
            return new ServerStatus(name, false, null, error);
        }

        private ServerStatus(String name, boolean success, String transport, String errorMessage) {
            this.name = name;
            this.success = success;
            this.transport = transport;
            this.errorMessage = errorMessage;
            this.timestamp = System.currentTimeMillis();
        }
    }
}