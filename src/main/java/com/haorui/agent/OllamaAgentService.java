package com.haorui.agent;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ollama Agent服务示例
 * 使用AgentScope Harness Agent连接本地Ollama
 */
@Slf4j
@Service
public class OllamaAgentService {

    private final HarnessAgent agent;

    public OllamaAgentService() {
        // 初始化工作区目录
        Path workspace = Paths.get(".agentscope/workspace");

        // 使用Ollama模型
        // 注意: 使用 ModelRegistry 简化配置时，只需 .model("ollama:模型名")
        // 例如: .model("ollama:qwen2.5:7b") 或 .model("ollama:llama3.2")
        this.agent = HarnessAgent.builder()
                .name("ollama-assistant")
                .sysPrompt("你是一个友好的AI助手，使用中文回答问题。")
                .model("ollama:qwen2.5:7b")  // 自动连接本地Ollama (localhost:11434)
                .workspace(workspace)
                .build();

        log.info("Ollama Agent initialized with model: qwen2.5:7b");
    }

    /**
     * 与Agent进行对话
     *
     * @param userInput 用户输入
     * @param sessionId 会话ID（相同ID可保持对话上下文）
     * @return Agent的回复
     */
    public Mono<String> chat(String userInput, String sessionId) {
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId("user")
                .build();

        return agent.call(new UserMessage(userInput), ctx)
                .map(response -> response.getContent().toString());
    }

    /**
     * 流式输出对话（实时获取回复）
     * 适用于Web界面或控制台实时显示
     *
     * @param userInput 用户输入
     * @param sessionId 会话ID
     * @return 流式文本片段
     */
    public reactor.core.publisher.Flux<String> chatStream(String userInput, String sessionId) {
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId("user")
                .build();

        return agent.streamEvents(new UserMessage(userInput), ctx)
                .filter(event -> event.getType().name().equals("TEXT_BLOCK_DELTA"))
                .map(event -> event.toString());
    }
}